package wfcore.api.util;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

@SideOnly(Side.CLIENT)
public class RenderMaskManager {

    public final static ThreadLocal<Boolean> isBuildingChunk = ThreadLocal.withInitial(() -> Boolean.FALSE);
    protected static Set<BlockPos> modelDisabled = new ObjectOpenHashSet<>();
    protected static Map<BlockPos, Collection<BlockPos>> multiDisabled = new HashMap<>();

     // Set to true to see red boxes in the world where blocks are masked.
    public static boolean debugMode = true;

    public static void removeDisableModel(BlockPos controllerPos, boolean updateRendering) {
        Collection<BlockPos> poses = multiDisabled.remove(controllerPos);
        if (poses == null) return;

        modelDisabled.clear();
        multiDisabled.values().forEach(modelDisabled::addAll);

        if (updateRendering) updateRenderChunk(poses);
    }

    public static void addDisableModel(BlockPos controllerPos, Collection<BlockPos> poses, boolean updateRendering) {
        multiDisabled.put(controllerPos, poses);
        modelDisabled.addAll(poses);

        if (updateRendering) updateRenderChunk(poses);
    }

    private static void updateRenderChunk(Collection<BlockPos> poses) {
        if (poses.isEmpty()) return;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : poses) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        Minecraft.getMinecraft().world.markBlockRangeForRenderUpdate(
                new BlockPos(minX, minY, minZ),
                new BlockPos(maxX, maxY, maxZ)
        );
    }

    public static boolean isModelDisabled(BlockPos pos) {
        if (debugMode) return false;

        if (isBuildingChunk.get()) {
            return isModelDisabledRaw(pos);
        }
        return false;
    }

    public static boolean isModelDisabledRaw(BlockPos pos) {
        return modelDisabled.contains(pos);
    }

    public static boolean isModelDisabled(BlockPos.MutableBlockPos pos) {
        return isModelDisabled(pos.toImmutable());
    }

    public static boolean isModelDisabledRaw(BlockPos.MutableBlockPos pos) {
        return isModelDisabledRaw(pos.toImmutable());
    }

    public static void clearDisabled() {
        modelDisabled.clear();
        multiDisabled.clear();
    }


    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!debugMode || modelDisabled.isEmpty()) return;

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        float partialTicks = event.getPartialTicks();
        double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);

        for (BlockPos pos : modelDisabled) {
            AxisAlignedBB bb = new AxisAlignedBB(pos).offset(-d0, -d1, -d2);

            RenderGlobal.renderFilledBox(bb, 1.0F, 0.0F, 0.0F, 0.4F);

            RenderGlobal.drawSelectionBoundingBox(bb, 1.0F, 0.0F, 0.0F, 0.8F);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}

