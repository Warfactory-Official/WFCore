package wfcore.common.pipenet.ac.net;

import gregtech.api.pipenet.Node;
import gregtech.api.pipenet.PipeNet;
import gregtech.api.pipenet.WorldPipeNet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import wfcore.common.pipenet.ac.ACPipeProperties;

import java.util.Map;

public class ACPipeNet extends PipeNet<ACPipeProperties> {

    private final Map<BlockPos, ACRoutePath> netData = new Object2ObjectOpenHashMap<>();

    public ACPipeNet(WorldPipeNet<ACPipeProperties, ? extends PipeNet<ACPipeProperties>> world) {
        super(world);
    }

    @Nullable
    public ACRoutePath getNetData(BlockPos pipePos, EnumFacing facing) {
        if (netData.containsKey(pipePos)) {
            return netData.get(pipePos);
        }
        ACRoutePath data = ACNetWalker.createNetData(getWorldData(), pipePos, facing);
        if (data == ACNetWalker.FAILED_MARKER) {
            return null;
        }
        netData.put(pipePos, data);
        return data;
    }

    @Override
    public void onNeighbourUpdate(BlockPos fromPos) {
        netData.clear();
    }

    @Override
    public void onPipeConnectionsUpdate() {
        netData.clear();
    }

    @Override
    public void onChunkUnload() {
        netData.clear();
    }

    @Override
    protected void transferNodeData(Map<BlockPos, Node<ACPipeProperties>> transferredNodes,
                                    PipeNet<ACPipeProperties> parentNet) {
        super.transferNodeData(transferredNodes, parentNet);
        netData.clear();
        ((ACPipeNet) parentNet).netData.clear();
    }

    @Override
    protected void writeNodeData(ACPipeProperties nodeData, NBTTagCompound tagCompound) {
        tagCompound.setLong("throughput", nodeData.throughput);
    }

    @Override
    protected ACPipeProperties readNodeData(NBTTagCompound tagCompound) {
        return new ACPipeProperties(tagCompound.getLong("throughput"));
    }
}
