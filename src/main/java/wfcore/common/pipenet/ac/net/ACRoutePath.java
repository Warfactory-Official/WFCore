package wfcore.common.pipenet.ac.net;

import gregtech.api.pipenet.IRoutePath;
import net.minecraft.util.EnumFacing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wfcore.api.capability.IACEnergyContainer;
import wfcore.common.capability.WFCapabilities;
import wfcore.common.pipenet.ac.tile.TileEntityACPipe;

/** A resolved point-to-point route from one AC port to the AC port at the other end of a cable run. */
public class ACRoutePath implements IRoutePath<TileEntityACPipe> {

    private final TileEntityACPipe targetPipe;
    private final EnumFacing faceToHandler;
    private final int distance;
    private final long minThroughput;

    public ACRoutePath(TileEntityACPipe targetPipe, EnumFacing faceToHandler, int distance, long minThroughput) {
        this.targetPipe = targetPipe;
        this.faceToHandler = faceToHandler;
        this.distance = distance;
        this.minThroughput = minThroughput;
    }

    @NotNull
    @Override
    public TileEntityACPipe getTargetPipe() {
        return targetPipe;
    }

    @NotNull
    @Override
    public EnumFacing getTargetFacing() {
        return faceToHandler;
    }

    @Override
    public int getDistance() {
        return distance;
    }

    /** Thinnest cable along the run - the run can carry no more than this. */
    public long getMinThroughput() {
        return minThroughput;
    }

    @Nullable
    public IACEnergyContainer getHandler() {
        return getTargetCapability(WFCapabilities.CAPABILITY_AC_ENERGY);
    }
}
