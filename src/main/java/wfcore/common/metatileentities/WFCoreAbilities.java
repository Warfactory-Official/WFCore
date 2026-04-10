package wfcore.common.metatileentities;

import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import wfcore.common.metatileentities.compute.ICooler;
import wfcore.common.metatileentities.compute.ICpuSlot;
import wfcore.common.metatileentities.compute.IRamSlot;

public class WFCoreAbilities {
    public static final MultiblockAbility<ICpuSlot> GPC_CPU_SLOT =
            new MultiblockAbility<>("gcp_cpu_slot");
    public static final MultiblockAbility<IRamSlot> GPC_RAM_SLOT =
            new MultiblockAbility<>("gcp_ram_slot");
    public static final MultiblockAbility<ICooler> GPC_COOLER =
            new MultiblockAbility<>("gcp_cooler");


}
