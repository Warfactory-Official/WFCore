package wfcore.api.metatileentity;

import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.IFastRenderMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import wfcore.client.render.AnimatedRenderQueue;
import wfcore.common.network.SPacketUpdateRenderMask;
import wfcore.common.te.TERegistry;

import java.util.Collection;

/**
 * Interface for MetaTileEntities that support animated rendering.
 * <p>
 * Implementing classes provide access to animation state, transformation,
 * hidden blocks, and other rendering-related metadata.
 * This interface is intended to be used client-side for rendering and server-side
 * for animation state management.
 */
public interface IAnimatedMTE extends IFastRenderMetaTileEntity {

    static int CHANGE_ANIM = 1077;




    default double getRenderDistanceSqared(){
        return 16384.0D;
    }

    /**
     * Returns the local transformation offset for rendering.
     * <p>
     * This is the position offset applied before rendering the GLTF model.
     * Default is {@link Vec3d#ZERO} (no offset).
     *
     * @return a Vec3d representing the local transform
     */
    default Vec3d getTransform() {
        return Vec3d.ZERO;
    }


    default BlockPos getLightPos() {
        return thisObject().getPos();
    }
    /**
     * Returns the collection of block positions that should be hidden when rendering this tile entity.
     * Useful for multiblocks where internal components should not be drawn.
     *
     * @return a collection of BlockPos to hide
     */
    Collection<BlockPos> getHiddenBlocks();

    /**
     * Returns the current animation state name.
     * <p>
     * Used to select which AnimationLoop to play in the renderer.
     * Default is "default".
     *
     *
     * @return the current animation state
     */
    default String getAnimState() {
        return "default";
    }

    /**
     * Returns the "epoch" of the current animation in ticks.
     * <p>
     * This value is used to compute animation progress relative to world time.
     * Typically set on the server when an animation starts or resumes.
     *
     * @return world tick timestamp when the animation began
     */
    long getAnimEpoch();

    /**
     * Utility method to cast this tile entity to its concrete type.
     *
     * @param <T> concrete MetaTileEntity type
     * @return this instance cast to T
     */
    @SuppressWarnings("unchecked")
    default <T extends MetaTileEntity> T thisObject() {
        return (T) this;
    }

    /**
     * Returns the unique name of this MetaTileEntity.
     * Typically derived from its registry ID.
     *
     * @return the tile entity name
     */
    default String getName() {
        return thisObject().metaTileEntityId.getPath();
    }

    /**
     * Disables or enables rendering for specific blocks on the server.
     * <p>
     * Sends a packet to all clients in the dimension to hide or show
     * blocks returned by {@link #getHiddenBlocks()}.
     * Blocks are hidden by being excluded from the chunk VBO
     * <b>Server-side only.</b>
     *
     * @param disable true to hide blocks, false to show
     */
    default void disableBlockRendering(boolean disable) {
        World world = thisObject().getWorld();
        if (world.getMinecraftServer() != null) {
            BlockPos pos = thisObject().getPos();
            int dimId = world.provider.getDimension();
            var packet = new SPacketUpdateRenderMask(pos, disable ? getHiddenBlocks() : null, dimId);
            GregTechAPI.networkHandler.sendToDimension(packet, dimId);
        }
    }

    /**
     * Determines whether this tile entity should be rendered.
     * <p>
     * Used to conditionally skip rendering for client-side optimization
     * and to indicate whenever multiblock is formed
     *
     * @return true if this tile entity should be rendered, false otherwise
     */
    default boolean shouldRender() {
        return true;
    }

    /**
     * If true, the TESR will continue rendering even when the chunk is culled.
     * <p>
     * This is useful for animated multiblocks or objects that must always be visible.
     *
     * @return true if rendering should ignore chunk culling
     */
    @Override
    default boolean isGlobalRenderer() {
        return true;
    }

    /**
     * Renders this MetaTileEntity.
     * <p>
     * Default implementation looks up the renderer from {@link TERegistry}
     * and delegates the render call.
     *
     * @param x            X coordinate for rendering
     * @param y            Y coordinate for rendering
     * @param z            Z coordinate for rendering
     * @param partialTicks partial tick time for interpolation
     */
    @Override
    default void renderMetaTileEntity(double x, double y, double z, float partialTicks) {
        if (thisObject().getWorld() == Minecraft.getMinecraft().world && shouldRender()) {
            AnimatedRenderQueue.getInstance().submit(this);
        }
    }
}

