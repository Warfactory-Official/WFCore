package wfcore.common.drones;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import wfcore.WFCore;
import wfcore.common.network.drone.PacketDroneLaunch;
import wfcore.common.network.drone.PacketDroneTarget;

@Mod.EventBusSubscriber
public final class DroneRegistry {

    // Constants
    private static final int ENTITY_ID_SUICIDE_DRONE = 1200;
    public static final String NETWORK_CHANNEL = "wfcore_drone";

    public static SimpleNetworkWrapper networkWrapper;
    private static int packetId = 0;
    private DroneRegistry() {}

    public static void preInit(FMLPreInitializationEvent event) {
        registerNetwork();
    }

    public static void init(FMLInitializationEvent event) {
        registerEntities();
    }

    private static void registerNetwork() {
        networkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel(NETWORK_CHANNEL);

        networkWrapper.registerMessage(
                PacketDroneTarget.Handler.class,
                PacketDroneTarget.class,
                packetId++,
                Side.SERVER
        );

        networkWrapper.registerMessage(
                PacketDroneLaunch.Handler.class,
                PacketDroneLaunch.class,
                packetId++,
                Side.SERVER
        );
    }

    private static void registerEntities() {
        EntityRegistry.registerModEntity(
                new ResourceLocation("suicide_drone"),
                EntitySuicideDrone.class,
                "suicide_drone",
                ENTITY_ID_SUICIDE_DRONE,
                WFCore.INSTANCE,
                64,
                1,
                true
        );
    }
}
