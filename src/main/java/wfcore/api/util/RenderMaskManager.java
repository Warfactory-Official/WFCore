package wfcore.api.util;

import gregtech.api.metatileentity.MetaTileEntityHolder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import wfcore.api.metatileentity.IAnimatedMTE;
import wfcore.common.metatileentities.multi.electric.MetaTileEntityRadar;

import java.util.*;

@SideOnly(Side.CLIENT)
public class RenderMaskManager {

    public final static ThreadLocal<Boolean> isBuildingChunk = ThreadLocal.withInitial(() -> Boolean.FALSE);
    protected static Set<BlockPos> modelDisabled = new ObjectOpenHashSet<>();
    protected static Map<BlockPos, Collection<BlockPos>> multiDisabled = new HashMap<>();

    // Set to true to see red boxes in the world where blocks are masked.
    public static boolean debugMode = false;
    public static boolean aabbDebug = true;

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
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) return;
        if (!aabbDebug && !debugMode) return;

        float partialTicks = event.getPartialTicks();
        EntityPlayer player = mc.player;
        double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);

        if (aabbDebug) {
            List<TileEntity> telist = mc.world.loadedTileEntityList;
            for (int i = 0; i < telist.size(); i++) {
                TileEntity te = telist.get(i);
                if (te instanceof MetaTileEntityHolder mte) {
                    if (mte.getMetaTileEntity() instanceof IAnimatedMTE) {
                        AxisAlignedBB aabb = mte.getRenderBoundingBox().offset(-d0, -d1, -d2);
                        RenderGlobal.renderFilledBox(aabb, 0.0F, 1.0F, 1.0F, 0.15F);
                        RenderGlobal.drawSelectionBoundingBox(aabb, 1, 1, 1, 1);
                    }
                }
            }
        }

        if (debugMode && !modelDisabled.isEmpty()) {
            for (BlockPos pos : modelDisabled) {
                AxisAlignedBB bb = new AxisAlignedBB(pos).offset(-d0, -d1, -d2);
                RenderGlobal.renderFilledBox(bb, 1.0F, 0.0F, 0.0F, 0.4F);
                RenderGlobal.drawSelectionBoundingBox(bb, 1.0F, 0.0F, 0.0F, 0.8F);
            }
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}

