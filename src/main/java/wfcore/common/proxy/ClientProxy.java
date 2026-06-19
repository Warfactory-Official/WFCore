package wfcore.common.proxy;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import wfcore.api.util.RenderMaskManager;
import wfcore.client.render.WFTextures;
import wfcore.common.events.ClientRegistryEvents;
import wfcore.common.items.registry.CPURegistry;
import wfcore.common.items.registry.RAMRegistry;
import wfcore.common.render.ModelRegistry;
import wfcore.common.te.TERegistry;

public class ClientProxy extends CommonProxy {
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        MinecraftForge.EVENT_BUS.register(new ClientRegistryEvents());
        MinecraftForge.EVENT_BUS.register(RenderMaskManager.class);
        MinecraftForge.EVENT_BUS.register(CPURegistry.class);
        MinecraftForge.EVENT_BUS.register(RAMRegistry.class);
        ModelRegistry.init();
        TERegistry.registerRenderers();
        WFTextures.registerTextures();
        registerACPipeModels();

    }

    private void registerACPipeModels() {
        wfcore.client.render.ACPipeRenderer.INSTANCE.preInit();
        for (wfcore.common.pipenet.ac.BlockACPipe pipe : wfcore.common.pipenet.ac.ACPipes.AC_CABLES) {
            if (pipe == null) continue;
            net.minecraftforge.client.model.ModelLoader.setCustomStateMapper(pipe,
                    new gregtech.client.model.SimpleStateMapper(
                            wfcore.client.render.ACPipeRenderer.INSTANCE.getModelLocation()));
            net.minecraft.item.Item item = net.minecraft.item.Item.getItemFromBlock(pipe);
            net.minecraftforge.client.model.ModelLoader.setCustomMeshDefinition(item,
                    stack -> wfcore.client.render.ACPipeRenderer.INSTANCE.getModelLocation());
        }
    }
}
