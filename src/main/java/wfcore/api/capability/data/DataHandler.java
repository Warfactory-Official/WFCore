package wfcore.api.capability.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.nbt.NBTTagCompound;
import wfcore.api.util.math.ClusterData;

import java.util.function.Function;

public class DataHandler {
    // do not change the id's used here or else pre-existing saves will lose data
    // new entries must be APPENDED to preserve existing ordinals
    public enum DataClassIdentifier {
        CLUSTER_DATA(ClusterData.class);

        public final Class<? extends IData> clazz;

        DataClassIdentifier(final Class<? extends IData> clazz) {
            this.clazz = clazz;
        }
    }

    public Int2ObjectOpenHashMap<Function<NBTTagCompound, ? extends IData>> DATA_READER_REGISTRY = new Int2ObjectOpenHashMap<>();
    public boolean isInitialized = false;

    public DataHandler() {

    }

    public void registerDataClass(DataClassIdentifier dataClass, Function<NBTTagCompound, ? extends IData> fromNBT) {
        DATA_READER_REGISTRY.put(dataClass.ordinal(), fromNBT);
    }

    public DataHandler initializeDataHandler() {
        // each data class must be initialized here
        registerDataClass(DataClassIdentifier.CLUSTER_DATA, ClusterData::fromNBT);

        isInitialized = true;
        return this;
    }
}
