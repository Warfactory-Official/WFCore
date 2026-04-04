package wfcore.common.world;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import wfcore.WFCore;
import wfcore.api.radar.MultiblockRadarLogic;
import wfcore.api.radar.RadarTargetIdentifier;
import wfcore.common.managers.RadarDataManager;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

public class Retrofitter {

    public static final Retrofitter INSTANCE = new Retrofitter();
    private static final int CHUNKS_PER_TICK = 5;
    public static boolean active = false;

    public final Queue<Combined> queue = new ConcurrentLinkedQueue<>();

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (!active || event.side.isClient() || event.phase != TickEvent.Phase.END) return;

        WorldServer world = (WorldServer) event.world;
        if (world.provider.getDimension() != 0) return;

        for (int i = 0; i < CHUNKS_PER_TICK; i++) {
            Combined d = queue.poll();
            if (d == null) {
                active = false;
                break;
            }
            RadarDataManager.INSTANCE.addMachine(world, d.packed, d.value);
        }
    }

    public void startGlobalScan(File region) {
        active = true;
        File[] files = region.listFiles((dir, name) -> name.endsWith(".mca"));
        if (files == null) return;

        WFCore.LOGGER.info("Starting Virtual Thread background scan...");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (File mcaFile : files) {
                executor.submit(() -> scanRegionFile(mcaFile));
            }
        }
    }

    public void scanRegionFile(File mcaFile) {
        String[] parts = mcaFile.getName().split("\\.");
        if (parts.length < 3) return;

        RegionFile region = null;
        try {
            region = new RegionFile(mcaFile);
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    if (region.chunkExists(x, z)) {
                        processChunk(region, x, z);
                    }
                }
            }
        } catch (Exception e) {
            WFCore.LOGGER.error("Failed to parse region: " + mcaFile.getName(), e);
        } finally {
            if (region != null) {
                try {
                    region.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void processChunk(RegionFile region, int x, int z) throws Exception {
        try (DataInputStream dis = region.getChunkDataInputStream(x, z)) {
            if (dis == null) return;
            NBTTagCompound nbt = CompressedStreamTools.read(dis);
            processChunkNBT(nbt);
        }
    }

    private void processChunk(RegionFile region, int x, int z, int regX, int regZ) throws Exception {
        try (DataInputStream dis = region.getChunkDataInputStream(x, z)) {
            if (dis == null) return;
            NBTTagCompound nbt = CompressedStreamTools.read(dis);
            processChunkNBT(nbt);
        }
    }

    private void processChunkNBT(NBTTagCompound nbt) {
        NBTTagCompound level = nbt.getCompoundTag("Level");
        NBTTagList teList = level.getTagList("TileEntities", 10);
        if (teList.tagCount() == 0) return;

        NBTTagList sections = level.getTagList("Sections", 10);

        for (int i = 0; i < teList.tagCount(); i++) {
            NBTTagCompound teNbt = teList.getCompoundTagAt(i);
            int y = teNbt.getInteger("y");

            NBTTagCompound section = findSection(sections, y >> 4);
            if (section == null) continue;

            int blockIndex = ((y & 15) << 8) | ((teNbt.getInteger("z") & 15) << 4) | (teNbt.getInteger("x") & 15);
            int blockId = getBlockIdFromSection(section, blockIndex);

            RadarTargetIdentifier identifier = RadarTargetIdentifier.getBestIdentifier(teNbt, blockId);

            if (MultiblockRadarLogic.TE_WHITELIST.contains(identifier)) {
                int value = MultiblockRadarLogic.getValue(identifier);
                queue.add(new Combined(RadarDataManager.pack(teNbt.getInteger("x"), teNbt.getInteger("z")), value));
            }
        }
    }

    private NBTTagCompound findSection(NBTTagList sections, int sectionY) {
        for (int i = 0; i < sections.tagCount(); i++) {
            NBTTagCompound s = sections.getCompoundTagAt(i);
            if (s.getByte("Y") == sectionY) return s;
        }
        return null;
    }

    private int getBlockIdFromSection(NBTTagCompound section, int index) {
        byte[] blocks = section.getByteArray("Blocks");
        int id = blocks[index] & 0xFF;
        if (section.hasKey("Add", 7)) {
            byte[] add = section.getByteArray("Add");
            int addVal = (add[index >> 1] >> ((index & 1) << 2)) & 0xF;
            id |= (addVal << 8);
        }
        return id;
    }

    public record Combined(long packed, int value) {
    }
}
