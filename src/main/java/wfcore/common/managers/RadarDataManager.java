package wfcore.common.managers;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import lombok.Getter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

public class RadarDataManager {

    private static final String DATA_NAME = "wf_radar_registry";
    public static final RadarDataManager INSTANCE = new RadarDataManager();
    @Getter
    private final Long2IntOpenHashMap machineMap = new Long2IntOpenHashMap(50000);
    private PersistenceHandler storageHandler;

    private RadarDataManager() {
    }

    public static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public void addMachine(World world, int x, int z, int value) {
        machineMap.put(pack(x, z), value);
        markDirty(world);
    }

    public void removeMachine(World world, int x, int z) {
        if (machineMap.remove(pack(x, z)) != 0) {
            markDirty(world);
        }
    }


    private void markDirty(World world) {
        if (storageHandler == null) init(world);
        storageHandler.markDirty();
    }

    public void init(World world) {
        var storage = world.getMapStorage();
        this.storageHandler = (PersistenceHandler) storage.getOrLoadData(PersistenceHandler.class, DATA_NAME);

        if (this.storageHandler == null) {
            this.storageHandler = new PersistenceHandler(DATA_NAME);
            storage.setData(DATA_NAME, this.storageHandler);
        }
    }


    private static class PersistenceHandler extends WorldSavedData {

        public PersistenceHandler(String name) {
            super(name);
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            Long2IntOpenHashMap map = RadarDataManager.INSTANCE.getMachineMap();
            map.clear();
            NBTTagList list = nbt.getTagList("machines", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound tag = list.getCompoundTagAt(i);
                map.put(tag.getLong("p"), tag.getInteger("v"));
            }
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
            NBTTagList list = new NBTTagList();
            var iterator = RadarDataManager.INSTANCE.getMachineMap().long2IntEntrySet().iterator();

            while (iterator.hasNext()) {
                var entry = iterator.next();
                NBTTagCompound tag = new NBTTagCompound();
                tag.setLong("p", entry.getLongKey());
                tag.setInteger("v", entry.getIntValue());
                list.appendTag(tag);
            }

            nbt.setTag("machines", list);
            return nbt;
        }
    }


}
