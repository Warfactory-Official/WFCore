package wfcore.api.capability.data;

import net.minecraft.nbt.NBTTagCompound;

import java.math.BigInteger;

// ALL INHERITORS MUST REGISTER THEMSELVES IN THE DATA HANDLER CLASS
public interface IData {

    static Class<? extends IData> getDataClass(IData data) {
        return data.getTypeId().clazz;
    }
    static BigInteger numOpsFromOPSTime(long numSeconds, long OPSBaseline, long scalar) {
        return BigInteger.valueOf(numSeconds * OPSBaseline * scalar);
    }

    DataHandler.DataClassIdentifier getTypeId();
    BigInteger numBits();
    BigInteger numOpsToExtract();
    NBTTagCompound toNBT();
}
