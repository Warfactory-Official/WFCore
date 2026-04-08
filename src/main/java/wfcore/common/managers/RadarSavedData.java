package wfcore.common.managers;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import wfcore.api.util.math.ClusterData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RadarSavedData extends WorldSavedData {

    private static final String DATA_NAME = "wfcore_radar_scans";

    // FastUtils map for zero-allocation lookups
    private final Object2ObjectOpenHashMap<UUID, RadarScanRecord> database = new Object2ObjectOpenHashMap<>();

    public RadarSavedData() {
        super(DATA_NAME);
    }

    public RadarSavedData(String name) {
        super(name);
    }


    public static RadarSavedData get() {
        // Always attach to Dim 0 for global data
        World overworld = DimensionManager.getWorld(0);
        if (overworld == null) {
            throw new RuntimeException("Attempted to access RadarSavedData before Overworld was loaded!");
        }

        MapStorage storage = overworld.getMapStorage();
        RadarSavedData instance = (RadarSavedData) storage.getOrLoadData(RadarSavedData.class, DATA_NAME);

        if (instance == null) {
            instance = new RadarSavedData();
            storage.setData(DATA_NAME, instance);
        }
        return instance;
    }

    public void addScan(UUID id, List<ClusterData> clusters) {
        database.put(id, new RadarScanRecord(clusters, System.currentTimeMillis()));
        this.markDirty();
    }
    public void rmScan(UUID id) {
        database.remove(id);
        this.markDirty();
    }

    public List<ClusterData> getScan(UUID id) {
        RadarScanRecord record = database.get(id);
        if (record != null) {
            record.lastAccessed = System.currentTimeMillis();
            this.markDirty();
            return record.clusters;
        }
        return null;
    }


    public void pruneStaleRecords(long maxAgeMillis) {
        long now = System.currentTimeMillis();
        boolean removedAny = database.values().removeIf(record -> (now - record.lastAccessed) > maxAgeMillis);

        if (removedAny) {
            this.markDirty();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        database.clear();
        NBTTagList entryList = nbt.getTagList("Database", 10);

        for (int i = 0; i < entryList.tagCount(); i++) {
            NBTTagCompound entryTag = entryList.getCompoundTagAt(i);
            UUID id = new UUID(entryTag.getLong("UUIDMost"), entryTag.getLong("UUIDLeast"));
            long lastAccessed = entryTag.getLong("LastAccessed");

            NBTTagList clustersTag = entryTag.getTagList("Clusters", 10);
            List<ClusterData> clusters = new ArrayList<>(clustersTag.tagCount());
            for (int j = 0; j < clustersTag.tagCount(); j++) {
                clusters.add(ClusterData.fromNBT(clustersTag.getCompoundTagAt(j)));
            }

            database.put(id, new RadarScanRecord(clusters, lastAccessed));
        }
    }


    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList entryList = new NBTTagList();

        database.object2ObjectEntrySet().fastForEach(entry -> {
            NBTTagCompound entryTag = new NBTTagCompound();
            entryTag.setLong("UUIDMost", entry.getKey().getMostSignificantBits());
            entryTag.setLong("UUIDLeast", entry.getKey().getLeastSignificantBits());
            entryTag.setLong("LastAccessed", entry.getValue().lastAccessed);

            NBTTagList clustersTag = new NBTTagList();
            for (ClusterData cluster : entry.getValue().clusters) {
                clustersTag.appendTag(cluster.toNBT());
            }
            entryTag.setTag("Clusters", clustersTag);

            entryList.appendTag(entryTag);
        });

        nbt.setTag("Database", entryList);
        return nbt;
    }


    private static class RadarScanRecord {
        final List<ClusterData> clusters;
        long lastAccessed;

        RadarScanRecord(List<ClusterData> clusters, long lastAccessed) {
            this.clusters = clusters;
            this.lastAccessed = lastAccessed;
        }
    }
}
