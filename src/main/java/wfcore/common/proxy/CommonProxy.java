package wfcore.common.proxy;

import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import wfcore.common.events.RegistryEvents;
import wfcore.common.recipe.HBMRecepies;
import wfcore.common.recipe.WFCoreMachineRecipes;

public class CommonProxy {


    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new RegistryEvents());
    }

    public void init(FMLInitializationEvent event) {
    }

    public void loadComplete(FMLLoadCompleteEvent event) {
        HBMRecepies.init(event);
    }

    public void postInit(FMLPostInitializationEvent event) {
    }

    public final void serverStarting(FMLServerStartingEvent event) {
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        WFCoreMachineRecipes.initRecipes();
    }

}
