package wfcore;

import gregtech.api.GregTechAPI;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wfcore.api.radar.MultiblockRadarLogic;
import wfcore.common.network.SPacketUpdateRenderMask;
import wfcore.common.proxy.CommonProxy;
import wfcore.modules.WFCoreModule;

@Mod(modid = wfcore.Reference.MODID, version = wfcore.Reference.VERSION, name = wfcore.Reference.MODNAME, acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "after:hbm;required-before:gregtech;required-after-client:mcgltf;required-client:ctm"

)
public class WFCore {
    public static final Logger LOGGER = LogManager.getLogger(wfcore.Reference.MODID);
    public static final String MODID = "wfcore";
    public static final boolean DEBUG = true;

    @SidedProxy(
            clientSide = "wfcore.common.proxy.ClientProxy",
            serverSide = "wfcore.common.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);

        // read the radar config file
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        proxy.loadComplete(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    // we need may want to check blocks after they are placed to track them
    @SubscribeEvent
    public void blockPlaced(BlockEvent.EntityPlaceEvent event) {
        // only handle on server
        if (event.getWorld().isRemote) {
            return;
        }


    }

    // we may want to inspect all entities when they are created
    @SubscribeEvent
    public void entityConstructing(EntityEvent.EntityConstructing event) {
        // only handle on server
        if (event.getEntity().getEntityWorld().isRemote) {
            return;
        }
    }

}
