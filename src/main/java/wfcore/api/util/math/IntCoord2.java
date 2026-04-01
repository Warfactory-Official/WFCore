package wfcore.api.util.math;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.math3.ml.clustering.Clusterable;

//Simple integer tuple implementation, no point getting external Libraries involved
// Implements clusterable for use in dbscan
public class IntCoord2 implements Clusterable {
    private final int X, Z;

    public IntCoord2(int xVal, int zVal) {
        this.X = xVal;
        this.Z = zVal;
    }

    public IntCoord2(BlockPos pos) {
        this.X = (int) pos.getX();
        this.Z = (int) pos.getZ();
    }

    public IntCoord2(long packed) {
        this.X = (int) (packed >> 32);
        this.Z = (int) packed;
    }

    public int getX() {
        return X;
    }

    public int getZ() {
        return Z;
    }

    @Override
    public double[] getPoint() {
        return new double[]{X, Z};
    }

    @Override
    public String toString() {
        return "(" + X + ", " + Z + ")";
    }

    public NBTTagCompound toNBT() {
        var nbt = new NBTTagCompound();
        nbt.setInteger("cx", X);
        nbt.setInteger("cz", Z);
        return nbt;
    }

    public static IntCoord2 fromNBT(NBTTagCompound nbt) {
        return new IntCoord2(nbt.getInteger("cx"), nbt.getInteger("cz"));
    }
}
