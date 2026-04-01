package wfcore.common.world;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldServer;
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

   public static void onTick(WorldServer world){
       if(!active) return;


   }
   public final LongOpenHashSet queue = new LongOpenHashSet();

    private final int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    private final ExecutorService executor = Executors.newFixedThreadPool(threads);

   public void startGlobalScan(File region){
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
