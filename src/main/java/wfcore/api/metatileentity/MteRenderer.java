package wfcore.api.metatileentity;

import com.google.common.collect.ImmutableMap;
import com.modularmods.mcgltf.IGltfModelReceiver;
import com.modularmods.mcgltf.RenderedGltfModel;
import com.modularmods.mcgltf.RenderedGltfScene;
import com.modularmods.mcgltf.animation.GltfAnimationCreator;
import de.javagl.jgltf.model.AnimationModel;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.util.RelativeDirection;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;
import wfcore.common.render.AnimationController;
import wfcore.common.render.AnimationLoop;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Base renderer class for MetaTileEntities that support GLTF animations.
 * <p>
 * This class provides the core rendering logic for GLTF models and animation loops,
 * including transformation handling, rotation/flipping based on multiblock orientation,
 * and OpenGL state management. Subclasses must implement {@link #renderGLTF} to define
 * per-tile rendering behavior.
 *
 * @param <T> the type of MetaTileEntity this renderer supports,
 *            must implement {@link IAnimatedMTE}
 */
public abstract class MteRenderer<T extends MetaTileEntity & IAnimatedMTE> implements IGltfModelReceiver {

    /**
     * Map of animation names to their corresponding {@link AnimationLoop} objects.
     * <p>
     * Populated when the GLTF model is received through {@link #onReceiveSharedModel}.
     * Subclasses should retrieve and update animations from this map during rendering.
     */
    public ImmutableMap<String, AnimationLoop> animations;

    /**
     * The preprocessed, GPU-ready GLTF scene used for rendering.
     * <p>
     * Populated when the GLTF model is received via {@link #onReceiveSharedModel}.
     */
    protected RenderedGltfScene renderedScene;

    /**
     * Playback state per tile entity. Weakly keyed so controllers are dropped when their tile unloads.
     */
    private final Map<IAnimatedMTE, AnimationController> controllers = new WeakHashMap<>();

    /**
     * Advances the calling tile entity's {@link AnimationController} and applies the resolved pose to
     * the shared model, immediately before it is drawn.
     * <p>
     * Time is driven by frame deltas, so a tile whose {@link IAnimatedMTE#isAnimationRunning()} is
     * false freezes in place (power loss) and resumes seamlessly, while state changes honour the
     * tile's {@link wfcore.api.metatileentity.AnimTransition} policy to avoid snapping.
     *
     * @param mte          the tile entity being rendered
     * @param partialTicks partial tick interpolation for this frame
     */
    protected <M extends MetaTileEntity & IAnimatedMTE> void applyAnimation(M mte, float partialTicks) {
        if (animations == null) return;
        AnimationController controller = controllers.get(mte);
        if (controller == null) {
            controller = new AnimationController();
            controllers.put(mte, controller);
        }
        float now = (float) mte.getWorld().getTotalWorldTime() + partialTicks;
        controller.advance(mte, animations, now);
        AnimationLoop loop = animations.get(controller.getCurrent());
        if (loop != null) loop.update(controller.getTime());
    }

    /**
     * Rotates the model to face a specified direction.
     * <p>
     * Used to orient multiblock components based on their {@link EnumFacing} front
     * and spin directions. Applies rotation and scaling to the OpenGL matrix.
     *
     * @param face the primary facing direction of the tile entity
     * @param spin the "upward" or secondary facing direction
     */
    public static void rotateToFace(EnumFacing face, EnumFacing spin) {
        int angle = switch (spin) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
        switch (face) {
            case UP -> {
                GlStateManager.scale(-1, 1, 1);
                GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(-angle, 0, 0, 1);
            }
            case DOWN -> {
                GlStateManager.rotate(270.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(spin == EnumFacing.EAST || spin == EnumFacing.WEST ? -angle : angle, 0, 0, 1);
            }
            case EAST -> {
                GlStateManager.rotate(270.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(angle, 0, 0, 1);
            }
            case WEST -> {
                GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(angle, 0, 0, 1);
            }
            case NORTH -> GlStateManager.rotate(angle, 0, 0, 1);
            case SOUTH -> {
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(angle, 0, 0, 1);
            }
        }
    }

    /**
     * Flips the model along axes according to the given facing.
     * <p>
     * Useful for mirrored multiblock components. Applies scaling
     * transformations to the OpenGL matrix.
     *
     * @param facing the direction used to determine flip axes
     */
    public static void flip(EnumFacing facing) {
        int fX = facing.getXOffset() == 0 ? 1 : -1;
        int fY = facing.getYOffset() == 0 ? 1 : -1;
        int fZ = facing.getZOffset() == 0 ? 1 : -1;
        GlStateManager.scale(fX, fY, fZ);
    }

    public static void setupLight(int light) {
        int lx = light % 0x10000;
        int ly = light / 0x10000;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lx, ly);
    }

    /**
     * Called when the GLTF model and animations are ready on the client.
     * <p>
     * Stores the first rendered scene and builds a map of {@link AnimationLoop} objects
     * indexed by their names. Animations with a "_loop" suffix will have {@link AnimationLoop#loop} set to true.
     *
     * @param renderedModel the GLTF model wrapper containing the rendered scene and animation models
     */
    @Override
    public void onReceiveSharedModel(RenderedGltfModel renderedModel) {
        renderedScene = renderedModel.renderedGltfScenes.get(0);
        List<AnimationModel> animationModels = renderedModel.gltfModel.getAnimationModels();
        ImmutableMap.Builder<String, AnimationLoop> animations = ImmutableMap.builder();
        for (AnimationModel animationModel : animationModels) {
            var rawAnim = new AnimationLoop(GltfAnimationCreator.createGltfAnimation(animationModel));
            var name = animationModel.getName().toLowerCase(Locale.ROOT).split("_");
            if (name.length > 1 && name[1].equals("loop"))
                rawAnim.setLoop(true);
            animations.put(name[0], rawAnim);
        }
        this.animations = animations.build();
    }

    /**
     * Performs the full render process for a tile entity.
     * <p>
     * This includes:
     * <ul>
     *     <li>OpenGL matrix push/pop</li>
     *     <li>Basic OpenGL state setup (shading, blending, rescale normals)</li>
     *     <li>Translation to tile entity position + transform offset</li>
     *     <li>Orientation/flip adjustments for multiblock controllers</li>
     *     <li>Delegation to {@link #renderGLTF} for actual GLTF drawing</li>
     *     <li>OpenGL state restoration</li>
     * </ul>
     *
     * @param mte          the tile entity being rendered
     * @param x            world X position
     * @param y            world Y position
     * @param z            world Z position
     * @param partialTicks interpolation factor for smooth rendering
     */
    public void render(T mte, double x, double y, double z, float partialTicks) {
        var vec3d = mte.getTransform();
        GlStateManager.pushMatrix();
        {
            setupLight(mte.getWorld().getCombinedLight(mte.getLightPos(), 0));
            EnumFacing front = mte.getFrontFacing();
            GlStateManager.translate(x, y, z );
            GlStateManager.translate(vec3d.x, vec3d.y, vec3d.z);
            if (mte instanceof MultiblockControllerBase controller) {
                EnumFacing upwards = controller.getUpwardsFacing();
                EnumFacing left = RelativeDirection.LEFT.getRelativeFacing(front, upwards, controller.isFlipped());

                if (controller.isFlipped()) flip(left);
                rotateToFace(front, upwards);
            }
            renderGLTF(mte, partialTicks);
        }
        GlStateManager.popMatrix();
    }

    /**
     * Renders the GLTF model for the specific tile entity.
     * <p>
     * Subclasses must implement this to handle per-tile animation updates and
     * drawing of the {@link RenderedGltfScene}.
     *
     * @param mte          the tile entity being rendered
     * @param partialTicks partial tick interpolation
     * @param <T>          ensures type safety with tile entity extending {@link IAnimatedMTE}
     */
    abstract public <T extends MetaTileEntity & IAnimatedMTE> void renderGLTF(T mte, float partialTicks);
}

