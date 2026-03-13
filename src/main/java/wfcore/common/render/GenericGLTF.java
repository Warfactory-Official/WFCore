package wfcore.common.render;

import com.modularmods.mcgltf.MCglTF;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.animation.Animation;
import wfcore.Tags;
import wfcore.api.metatileentity.IAnimatedMTE;
import wfcore.api.metatileentity.MteRenderer;
import wfcore.common.metatileentities.multi.electric.MetaTileEntityRadar;

/**
 * Generic GLTF renderer for MetaTileEntities that implement {@link IAnimatedMTE}.
 * <p>
 * This class allows rendering of any tile entity that meets the generic type constraint,
 * using a single GLTF model and animation set. It handles:
 * <ul>
 *     <li>Fetching the model resource</li>
 *     <li>Animation timing using world time and tile entity epoch</li>
 *     <li>Delegation to shader or vanilla rendering paths</li>
 * </ul>
 *
 * @param <T> the type of tile entity this renderer supports; must extend {@link MetaTileEntity}
 *           and implement {@link IAnimatedMTE}
 */
public class GenericGLTF<T extends MetaTileEntity & IAnimatedMTE> extends MteRenderer<T> {

    /**
     * The GLTF model resource to render.
     */
    public final ResourceLocation modelResource;

    /**
     * Creates a new generic renderer with the given GLTF model resource.
     *
     * @param modelResource the GLTF model resource
     */
    public GenericGLTF(ResourceLocation modelResource) {
        this.modelResource = modelResource;
    }

    /**
     * Returns the GLTF model location for this renderer.
     *
     * @return the model resource
     */
    @Override
    public ResourceLocation getModelLocation() {
        return modelResource;
    }

    /**
     * Renders the GLTF model for the tile entity, updating animation state as needed.
     * <p>
     * Animation progress is calculated as the difference between the world time and the tile entity's
     * epoch. This allows independent animation progress per tile entity while still using global
     * world time. If the animation is finished or has not started yet, it will not update.
     *
     * @param mte          the tile entity to render
     * @param partialTicks partial tick interpolation for smooth animation
     * @param <T>          ensures type safety; must match the tile entity type constraint
     */
    public <T extends MetaTileEntity & IAnimatedMTE> void renderGLTF(T mte, float partialTicks) {
        float worldTimeS = Animation.getWorldTime(mte.getWorld(), partialTicks);
        var animation = animations.get(mte.getAnimState());
        float epochS = mte.getAnimEpoch() / 20f;
        float time = worldTimeS - epochS;

        if (animation != null && time >= 0) {
            animation.update(time);
        }

        // Choose rendering path depending on whether a shader mod is active
        if (MCglTF.getInstance().isShaderModActive()) {
            renderedScene.renderForShaderMod();
        } else {
            renderedScene.renderForVanilla();
        }
    }
}


