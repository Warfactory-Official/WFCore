package wfcore.mixins.minecraft;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wfcore.WFCore;
import wfcore.api.radar.MultiblockRadarLogic;
import wfcore.api.radar.RadarTargetIdentifier;
import wfcore.common.managers.RadarDataManager;

@Mixin(World.class)
public abstract class RadarRegistryWorldMixin {
    @Inject(method = "addTileEntity", at = @At("TAIL"))
    private void onAddTileEntity(TileEntity te, CallbackInfoReturnable<Boolean> cir) {
        World world = te.getWorld();
        if (world.isRemote) return;

        if (MultiblockRadarLogic.isOnTEWhitelist(te)) {
            RadarDataManager.INSTANCE.addMachine(
                    world,
                    te.getPos().getX(),
                    te.getPos().getZ(),
                    MultiblockRadarLogic.getValue(te)
            );
            if (WFCore.DEBUG)
                WFCore.LOGGER.info("Added TileEntity {} to map at {} [Dim: {}]", RadarTargetIdentifier.getBestIdentifier(te), te.getPos().toString(), world.provider.getDimension());
        }
    }

    @Inject(method = "removeTileEntity", at = @At("HEAD"))
    private void onRemoveTileEntity(BlockPos pos, CallbackInfo ci) {
        World world = (World) (Object) this;

        if (!(world.isRemote || world.getMapStorage() == null) && RadarDataManager.INSTANCE.hasMachine(world,pos.getX(), pos.getZ())) {

            if (world instanceof WorldServer) {
                WorldServer server = (WorldServer) world;
                ChunkProviderServer provider = server.getChunkProvider();

                int cx = pos.getX() >> 4;
                int cz = pos.getZ() >> 4;

                Chunk chunk = provider.getLoadedChunk(cx, cz);

                boolean isChunkUnloading = (chunk == null);

                if (!isChunkUnloading) {
                    long packed = ChunkPos.asLong(cx, cz);
                    if (provider.droppedChunks.contains(packed)) {
                        isChunkUnloading = true;
                    }
                }

                if (!isChunkUnloading) {

                    if (world.isAirBlock(pos) || !MultiblockRadarLogic.isOnTEWhitelist(world.getTileEntity(pos))) {
                        RadarDataManager.INSTANCE.removeMachine(world, pos.getX(), pos.getZ());

                        if (WFCore.DEBUG)
                            WFCore.LOGGER.info("Confirmed removal at {} [Dim: {}] - Block is AIR or INVALID.",
                                    pos.toString(), world.provider.getDimension());
                    } else {
                        if (WFCore.DEBUG)
                            WFCore.LOGGER.info("Persisting data for {} [Dim: {}] - Block still exists, ignoring removal.",
                                    pos.toString(), world.provider.getDimension());
                    }
                } else {
                    if (WFCore.DEBUG)
                        WFCore.LOGGER.info("Persisting data for {} [Dim: {}] - Chunk is unloading.",
                                pos.toString(), world.provider.getDimension());
                }
            }
        }
    }
}


