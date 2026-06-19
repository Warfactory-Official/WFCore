package wfcore.common.pipenet.ac.net;

import gregtech.api.pipenet.PipeNetWalker;
import gregtech.api.util.GTUtility;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import wfcore.api.capability.IACEnergyContainer;
import wfcore.common.capability.WFCapabilities;
import wfcore.common.pipenet.ac.tile.TileEntityACPipe;

/**
 * Walks an AC cable run in a straight line (like laser pipes) from a source port until it finds the AC port
 * at the other end, tracking the thinnest cable along the way.
 */
public class ACNetWalker extends PipeNetWalker<TileEntityACPipe> {

    public static final ACRoutePath FAILED_MARKER = new ACRoutePath(null, null, 0, 0);

    private static final EnumFacing[] X_AXIS_FACINGS = { EnumFacing.WEST, EnumFacing.EAST };
    private static final EnumFacing[] Y_AXIS_FACINGS = { EnumFacing.UP, EnumFacing.DOWN };
    private static final EnumFacing[] Z_AXIS_FACINGS = { EnumFacing.NORTH, EnumFacing.SOUTH };

    private ACRoutePath routePath;
    private long minThroughput = Long.MAX_VALUE;
    private BlockPos sourcePipe;
    private EnumFacing facingToHandler;
    private EnumFacing.Axis axis;

    @Nullable
    public static ACRoutePath createNetData(World world, BlockPos sourcePipe, EnumFacing faceToSourceHandler) {
        ACNetWalker walker = new ACNetWalker(world, sourcePipe, 1);
        walker.sourcePipe = sourcePipe;
        walker.facingToHandler = faceToSourceHandler;
        walker.axis = faceToSourceHandler.getAxis();
        walker.traversePipeNet();
        return walker.isFailed() ? FAILED_MARKER : walker.routePath;
    }

    protected ACNetWalker(World world, BlockPos sourcePipe, int distance) {
        super(world, sourcePipe, distance);
    }

    @Override
    protected PipeNetWalker<TileEntityACPipe> createSubWalker(World world, EnumFacing facingToNextPos,
                                                              BlockPos nextPos, int walkedBlocks) {
        ACNetWalker walker = new ACNetWalker(world, nextPos, walkedBlocks);
        walker.facingToHandler = facingToHandler;
        walker.sourcePipe = sourcePipe;
        walker.axis = axis;
        return walker;
    }

    @Override
    protected EnumFacing[] getSurroundingPipeSides() {
        return switch (axis) {
            case X -> X_AXIS_FACINGS;
            case Y -> Y_AXIS_FACINGS;
            case Z -> Z_AXIS_FACINGS;
        };
    }

    @Override
    protected void checkPipe(TileEntityACPipe pipeTile, BlockPos pos) {
        long throughput = pipeTile.getNodeData().throughput;
        ACNetWalker root = (ACNetWalker) this.root;
        if (throughput < root.minThroughput) {
            root.minThroughput = throughput;
        }
    }

    @Override
    protected void checkNeighbour(TileEntityACPipe pipeTile, BlockPos pipePos, EnumFacing faceToNeighbour,
                                  @Nullable TileEntity neighbourTile) {
        if (neighbourTile == null ||
                (GTUtility.arePosEqual(pipePos, sourcePipe) && faceToNeighbour == facingToHandler)) {
            return;
        }
        ACNetWalker root = (ACNetWalker) this.root;
        if (root.routePath == null) {
            IACEnergyContainer handler = neighbourTile.getCapability(
                    WFCapabilities.CAPABILITY_AC_ENERGY, faceToNeighbour.getOpposite());
            if (handler != null && handler.inputsAC(faceToNeighbour.getOpposite())) {
                long min = root.minThroughput == Long.MAX_VALUE ? 0 : root.minThroughput;
                root.routePath = new ACRoutePath(pipeTile, faceToNeighbour, getWalkedBlocks(), min);
                stop();
            }
        }
    }

    @Override
    protected Class<TileEntityACPipe> getBasePipeClass() {
        return TileEntityACPipe.class;
    }
}
