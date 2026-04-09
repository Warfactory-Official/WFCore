package wfcore.common.metatileentities;

import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import wfcore.common.metatileentities.compute.ICpuSlot;
import wfcore.common.metatileentities.compute.IRamSlot;

public class WFCoreMultiblockAbilityRegistry {
    public static final MultiblockAbility<ICpuSlot> CPU_SLOT =
            new MultiblockAbility<>("cpu_slot");
    public static final MultiblockAbility<IRamSlot> RAM_SLOT =
            new MultiblockAbility<>("ram_slot");


}
