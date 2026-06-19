package wfcore.common.pipenet.ac.net;

import gregtech.api.pipenet.WorldPipeNet;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import wfcore.common.pipenet.ac.ACPipeProperties;

public class WorldACPipeNet extends WorldPipeNet<ACPipeProperties, ACPipeNet> {

    private static final String DATA_ID = "wfcore.ac_pipe_net";

    public WorldACPipeNet(String name) {
        super(name);
    }

    @NotNull
    public static WorldACPipeNet getWorldPipeNet(@NotNull World world) {
        WorldACPipeNet netWorldData = (WorldACPipeNet) world.loadData(WorldACPipeNet.class, DATA_ID);
        if (netWorldData == null) {
            netWorldData = new WorldACPipeNet(DATA_ID);
            world.setData(DATA_ID, netWorldData);
        }
        netWorldData.setWorldAndInit(world);
        return netWorldData;
    }

    @Override
    protected ACPipeNet createNetInstance() {
        return new ACPipeNet(this);
    }
}
