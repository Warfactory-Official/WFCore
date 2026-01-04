package wfcore.api.util.math;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import wfcore.api.capability.data.IData;
import wfcore.api.capability.data.DataHandler;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClusterData implements IData {
    public final List<IntCoord2> coordinates;
    public final IntCoord2 centerPoint;
    public final BoundingBox boundingBox;
    public final int playerPopulation;
    public final BigInteger bitsUsed;

    public ClusterData(List<IntCoord2> coordinates, IntCoord2 centerPoint, BoundingBox boundingBox, int playerPopulation) {
        this.coordinates = coordinates;
        this.centerPoint = centerPoint;
        this.boundingBox = boundingBox;
        this.playerPopulation = playerPopulation;
        bitsUsed = BigInteger.valueOf(28 + ((long) coordinates.size() << 2));
    }

    // TODO: make this translatable
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();

        // summarize cluster
        str.append("Cluster Centered on ");
        str.append(centerPoint.toString());
        str.append(" with ");
        str.append(playerPopulation);
        str.append(" player(s) inside ");
        str.append(boundingBox.toString());

        // list points
        str.append("\nCOORDS: [\n    ");
        var coordIt = coordinates.iterator();
        int coordsOnLine = 0;
        while (coordIt.hasNext()) {
            IntCoord2 currCoord = coordIt.next();
            // list points
            str.append(currCoord.toString());
            ++coordsOnLine;

            // support next character if there will be one
            if (coordIt.hasNext()) {
                // only put five coordinates on a given line
                if (coordsOnLine >= 5) {
                    str.append(",\n    ");
                    coordsOnLine = 0;
                } else {
                    str.append(", ");
                }
            }
        }

        str.append("\n]");
        return str.toString();
    }

    public static ClusterData fromNBT(NBTTagCompound nbt) {
        List<IntCoord2> coords = new ArrayList<>();
        nbt.getTagList("coords", 10).tagList.forEach(coordTag -> coords.add(IntCoord2.fromNBT((NBTTagCompound) coordTag)));

        return new ClusterData(
                coords,
                IntCoord2.fromNBT(nbt.getCompoundTag("center")),
                BoundingBox.fromNBT(nbt.getCompoundTag("bounds")),
                nbt.getInteger("pop")
        );
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("pop", playerPopulation);
        nbt.setTag("center", centerPoint.toNBT());
        nbt.setTag("bounds", boundingBox.toNBT());

        NBTTagList coords = new NBTTagList();
        coordinates.forEach(coord -> coords.appendTag(coord.toNBT()));
        nbt.setTag("coords", coords);

        return nbt;
    }

    @Override
    public DataHandler.DataClassIdentifier getTypeId() {
        return DataHandler.DataClassIdentifier.CLUSTER_DATA;
    }

    @Override
    public BigInteger numBits() {
        return bitsUsed;
    }

    @Override
    public BigInteger numOpsToExtract() {
        // should take ~15min at 256 OPS of compute power for every 4 coordinates
        return IData.numOpsFromOPSTime(900, 256, (long) coordinates.size() >> 2);
    }
}
