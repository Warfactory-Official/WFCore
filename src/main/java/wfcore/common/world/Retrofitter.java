package wfcore.common.world;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import wfcore.WFCore;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

public class Retrofitter {

   public static boolean active = false;
   public static final Retrofitter INSTANCE = new Retrofitter();

    // Adjust this to control scan speed. 1-3 is safe. 5+ may lag heavy modpacks.
    private static final int CHUNKS_PER_TICK = 5;

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (!active || event.side.isClient() || event.phase != TickEvent.Phase.END) return;

        WorldServer world = (WorldServer) event.world;
        if (world.provider.getDimension() != 0) return;

        for (int i = 0; i < CHUNKS_PER_TICK; i++) {
            long packed = pollFromSet();

            if (packed == Long.MIN_VALUE) break;

            int cx = (int) (packed >> 32);
            int cz = (int) packed;

            wakeChunk(world, cx, cz);
        }
    }
    private synchronized long pollFromSet() {

        if (queue.isEmpty()) return Long.MIN_VALUE;

        long val = queue.iterator().nextLong();
        queue.remove(val);
        return val;
    }

    private void wakeChunk(WorldServer world, int cx, int cz) {
        boolean wasLoaded = world.getChunkProvider().chunkExists(cx, cz);

        net.minecraft.world.chunk.Chunk chunk = world.getChunkProvider().loadChunk(cx, cz);

        if (chunk != null && !wasLoaded) {
            world.getChunkProvider().queueUnload(chunk);

            if (WFCore.DEBUG) {
                WFCore.LOGGER.info("Woke and queued unload for chunk at [{}, {}]", cx, cz);
            }
        }
    }

        public final LongOpenHashSet queue = new LongOpenHashSet();

    private final int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    private final ExecutorService executor = Executors.newFixedThreadPool(threads);

   public void startGlobalScan(File region){
       active = true;
       File[] files = region.listFiles((dir, name) -> name.endsWith(".mca"));
       if (files == null) return;

       WFCore.LOGGER.info("Starting background radar scan on {} threads...", threads);

       for (File mcaFile : files) {
           executor.submit(() -> {
               scanRegionFile(mcaFile);
           });
       }

       executor.shutdown();
   }


    public void scanRegionFile(File mcaFile) {
        String[] parts = mcaFile.getName().split("\\.");
        if (parts.length < 3) return;

        int regX = Integer.parseInt(parts[1]);
        int regZ = Integer.parseInt(parts[2]);

        RegionFile region = null;
        try {
            region = new RegionFile(mcaFile);

            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    if (region.chunkExists(x, z)) {
                        try (DataInputStream dis = region.getChunkDataInputStream(x, z)) {
                            if (dis != null) {
                                NBTTagCompound nbt = CompressedStreamTools.read(dis);
                                processChunkNBT(nbt, x, z, regX, regZ);
                            }
                        } catch (Exception e) {
                            WFCore.LOGGER.error("Failed to read chunk NBT in " + mcaFile.getName(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            WFCore.LOGGER.error("Error accessing region file: " + mcaFile.getName(), e);
        } finally {
            if (region != null) {
                try {
                    region.close();
                } catch (IOException e) {
                    WFCore.LOGGER.error("Failed to close RegionFile: " + mcaFile.getName());
                }
            }
        }
    }

    private void processChunkNBT(NBTTagCompound nbt, int localX, int localZ, int regX, int regZ) {
        NBTTagCompound level = nbt.getCompoundTag("Level");
        NBTTagList teList = level.getTagList("TileEntities", 10);

        if (teList.tagCount() == 0) return;

        for (int i = 0; i < teList.tagCount(); i++) {
                int worldChunkX = (regX * 32) + localX;
                int worldChunkZ = (regZ * 32) + localZ;

                long packed = (long) worldChunkX << 32 | (worldChunkZ & 0xFFFFFFFFL);

                synchronized (queue) {
                    queue.add(packed);
                }
                break;
        }
    }
}
