package wfcore.common.managers;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import lombok.Getter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import wfcore.WFCore;

public class RadarDataManager {

    public static final RadarDataManager INSTANCE = new RadarDataManager();
    private static final String DATA_NAME = "wf_radar_registry";

    private RadarDataManager() {
    }

    public static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public PersistenceHandler getHandler(World world) {
        if (world.isRemote) return null;

        var storage = world.getMapStorage();
        PersistenceHandler handler = (PersistenceHandler) storage.getOrLoadData(PersistenceHandler.class, DATA_NAME);

        if (handler == null) {
            handler = new PersistenceHandler(DATA_NAME);
            storage.setData(DATA_NAME, handler);
        }
        return handler;
    }

    public void addMachine(World world, int x, int z, int value) {
        PersistenceHandler handler = getHandler(world);
        if (handler != null) {
            int r = handler.getMachineMap().put(pack(x, z), value);
            if(WFCore.DEBUG && r != 0)
                WFCore.LOGGER.warn("Tried to add duplicate for {} {}", x,z);
            handler.markDirty();
        }
    }

    public void addMachine(World world, long packed, int value) {
        PersistenceHandler handler = getHandler(world);
        if (handler != null) {
            int r = handler.getMachineMap().put(packed, value);
            handler.markDirty();
        }
    }

    public void removeMachine(World world, int x, int z) {
        PersistenceHandler handler = getHandler(world);
        if (handler != null) {
            if (handler.getMachineMap().remove(pack(x, z)) != 0) {
                handler.markDirty();
            }
        }
    }
    public boolean hasMachine(World world, int x, int z) {
        PersistenceHandler handler = getHandler(world);
        if (handler != null) {
            return handler.getMachineMap().containsKey(pack(x, z));
        }
        return false;
    }

    public static class PersistenceHandler extends WorldSavedData {
        @Getter
        private final Long2IntOpenHashMap machineMap = new Long2IntOpenHashMap(1000);

        public PersistenceHandler(String name) {
            super(name);
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            machineMap.clear();
            NBTTagList list = nbt.getTagList("machines", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound tag = list.getCompoundTagAt(i);
                machineMap.put(tag.getLong("p"), tag.getInteger("v"));
            }
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
            NBTTagList list = new NBTTagList();
            for (var entry : machineMap.long2IntEntrySet()) {
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
