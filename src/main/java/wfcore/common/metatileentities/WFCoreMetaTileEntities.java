package wfcore.common.metatileentities;


import gregtech.api.GTValues;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import wfcore.Reference;
import wfcore.common.metatileentities.compute.MetaTileEntityCPUSlot;
import wfcore.common.metatileentities.compute.MetaTileEntityRAMSlot;
import wfcore.common.metatileentities.multi.electric.MetaTileEntityComputer;
import wfcore.common.metatileentities.multi.electric.MetaTileEntityRadar;
import wfcore.common.metatileentities.multi.primitive.MetaTileEntityWarfactoryBlastFurnace;

import static gregtech.common.metatileentities.MetaTileEntities.registerMetaTileEntity;
import static wfcore.common.recipe.WFCoreMachineRecipes.COMPUTER_RECIPE_MAP;

public class WFCoreMetaTileEntities {


    public static MetaTileEntityWarfactoryBlastFurnace LARGEBLASTFURNACE;
    public static MetaTileEntityRadar RADAR;
    public static MetaTileEntityComputer COMPUTER;
    public static MetaTileEntityCPUSlot CPU_SLOT;
    public static MetaTileEntityRAMSlot RAM_SLOT_MV;
    public static MetaTileEntityRAMSlot RAM_SLOT_HV;

    public static int id = 10000;

    static {
    }

    public static void init() {
        //Multis
        LARGEBLASTFURNACE = registerMetaTileEntity(id++, new MetaTileEntityWarfactoryBlastFurnace(location("largeblastfurnace")));
        RADAR = registerMetaTileEntity(id++, new MetaTileEntityRadar(location("radar")));
        COMPUTER = registerMetaTileEntity(id++, new MetaTileEntityComputer(location("computer"), COMPUTER_RECIPE_MAP));
        CPU_SLOT = registerMetaTileEntity(id++, new MetaTileEntityCPUSlot(location("cpu_slot"), GTValues.LV));
        RAM_SLOT_MV = registerMetaTileEntity(id++, new MetaTileEntityRAMSlot(location("ram_slot.mv"), GTValues.MV));
        RAM_SLOT_HV = registerMetaTileEntity(id++, new MetaTileEntityRAMSlot(location("ram_slot.hv"), GTValues.HV));
    }

    private static ResourceLocation location(@NotNull String name) {
        return new ResourceLocation(Reference.MODID, name);
    }
}