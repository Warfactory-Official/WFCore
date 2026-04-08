package wfcore.common.metatileentities;

import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import wfcore.common.metatileentities.compute.ICpuSlot;

public class WFCoreMultiblockAbilityRegistry {
    public static final MultiblockAbility<ICpuSlot> CPU_SLOT =
            new MultiblockAbility<>("cpu_slot");

}
