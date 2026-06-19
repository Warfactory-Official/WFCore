package wfcore.common.pipenet.ac.net;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wfcore.api.capability.IACEnergyContainer;
import wfcore.common.pipenet.ac.tile.TileEntityACPipe;

/** The per-side AC capability a cable exposes; routes pushed energy to the AC port at the other end. */
public class ACNetHandler implements IACEnergyContainer {

    private ACPipeNet net;
    private final TileEntityACPipe pipe;
    private final EnumFacing facing;

    public ACNetHandler(ACPipeNet net, @NotNull TileEntityACPipe pipe, @Nullable EnumFacing facing) {
        this.net = net;
        this.pipe = pipe;
        this.facing = facing;
    }

    public void updateNetwork(ACPipeNet net) {
        this.net = net;
    }

    public ACPipeNet getNet() {
        return net;
    }

    @Nullable
    private ACRoutePath getRoute() {
        if (net == null || pipe == null || pipe.isInvalid() || facing == null) {
            return null;
        }
        return net.getNetData(pipe.getPipePos(), facing);
    }

    private void setPipesActive() {
        if (net == null) return;
        for (BlockPos pos : net.getAllNodes().keySet()) {
            if (pipe.getWorld().getTileEntity(pos) instanceof TileEntityACPipe acPipe) {
                acPipe.setActive(true, 100);
            }
        }
    }

    @Override
    public long acceptEnergy(EnumFacing side, long amount) {
        ACRoutePath route = getRoute();
        if (route == null) return 0;
        IACEnergyContainer handler = route.getHandler();
        if (handler == null) return 0;
        long limit = Math.min(amount, Math.min(getThroughput(), route.getMinThroughput()));
        if (limit <= 0) return 0;
        long accepted = handler.acceptEnergy(route.getTargetFacing().getOpposite(), limit);
        if (accepted > 0) setPipesActive();
        return accepted;
    }

    @Override
    public long getThroughput() {
        return pipe.getNodeData().throughput;
    }

    @Override
    public boolean inputsAC(EnumFacing side) {
        return true;
    }

    @Override
    public boolean outputsAC(EnumFacing side) {
        return true;
    }
}
