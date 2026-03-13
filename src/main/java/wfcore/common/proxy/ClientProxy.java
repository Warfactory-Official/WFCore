package wfcore.common.proxy;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import wfcore.api.util.RenderMaskManager;
import wfcore.client.render.WFTextures;
import wfcore.common.events.ClientRegistryEvents;
import wfcore.common.render.ModelRegistry;
import wfcore.common.te.TERegistry;

public class ClientProxy extends CommonProxy {
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        MinecraftForge.EVENT_BUS.register(new ClientRegistryEvents());
        MinecraftForge.EVENT_BUS.register(RenderMaskManager.class);
        ModelRegistry.init();
        TERegistry.registerRenderers();
        WFTextures.registerTextures();

    }
}
