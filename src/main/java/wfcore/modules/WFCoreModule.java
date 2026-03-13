package wfcore.modules;

import gregtech.api.GregTechAPI;
import gregtech.api.modules.GregTechModule;
import gregtech.api.modules.IGregTechModule;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import wfcore.Tags;
import wfcore.WFCore;
import wfcore.common.network.SPacketUpdateRenderMask;

@GregTechModule(
        moduleID = "warfactory_core",
        containerID = Tags.MODID,
        name = "Warfactory 2 Core",
        description = "Warfactory 2 core module",
        coreModule = true)
public class WFCoreModule implements IGregTechModule {
    @Override
    public @NotNull Logger getLogger() {
        return WFCore.LOGGER;
    }

    @Override
    public void registerPackets() {
        GregTechAPI.networkHandler.registerPacket(SPacketUpdateRenderMask.class);
    }

}
