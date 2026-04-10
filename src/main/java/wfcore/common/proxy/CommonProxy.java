package wfcore.common.proxy;

import gregtech.api.GregTechAPI;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import wfcore.common.commands.WfCoreCommands;
import wfcore.common.config.RadarConfig;
import wfcore.common.drones.DroneRegistry;
import wfcore.common.events.RegistryEvents;
import wfcore.common.fluid.CoolantRegistry;
import wfcore.common.items.registry.CPURegistry;
import wfcore.common.items.registry.RAMRegistry;
import wfcore.common.network.SPacketUpdateRenderMask;
import wfcore.common.recipe.HBMRecepies;
import wfcore.common.recipe.WFCoreMachineRecipes;
import wfcore.common.world.Retrofitter;

public class CommonProxy {


    public void preInit(FMLPreInitializationEvent event) {
        GregTechAPI.networkHandler.registerPacket(SPacketUpdateRenderMask.class);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new RegistryEvents());
        MinecraftForge.EVENT_BUS.register(Retrofitter.INSTANCE);
        DroneRegistry.preInit(event);
        RadarConfig.readRadarConfig();
    }

    public void init(FMLInitializationEvent event) {
        DroneRegistry.init(event);
    }

    public void loadComplete(FMLLoadCompleteEvent event) {
        HBMRecepies.init(event);
    }

    public void postInit(FMLPostInitializationEvent event) {
        CPURegistry.register();
        RAMRegistry.register();
        CoolantRegistry.register();
    }

    public final void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new WfCoreCommands());
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        WFCoreMachineRecipes.initRecipes();
    }


}
