package wfcore.common.metatileentities;


import gregtech.api.GTValues;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import wfcore.Reference;
import wfcore.common.metatileentities.compute.MetaTileEntityCPUSlot;
import wfcore.common.metatileentities.compute.MetaTileEntityCooling;
import wfcore.common.metatileentities.compute.MetaTileEntityRAMSlot;
import wfcore.common.metatileentities.electric.MetaTileEntityACHatch;
import wfcore.common.metatileentities.electric.MetaTileEntityPrinter;
import wfcore.common.metatileentities.multi.electric.MetaTileEntityLargeTransformer;
import wfcore.common.metatileentities.multi.electric.MetaTileEntityComputer;
import wfcore.common.metatileentities.multi.electric.MetaTileEntityLightGroundVehicleFactory;
import wfcore.common.metatileentities.multi.electric.MetaTileEntityMainframe;
import wfcore.common.metatileentities.multi.electric.MetaTileEntityRadar;
import wfcore.common.metatileentities.research.MetaTileEntityResearchUnit;
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
    public static MetaTileEntityCooling PASSIVE_COOLER_MV;
    public static MetaTileEntityCooling LIQUID_COOLER_MV;
    public static MetaTileEntityMainframe MAINFRAME;
    public static MetaTileEntityResearchUnit RESEARCH_UNIT;
    public static MetaTileEntityPrinter PRINTER;
    public static MetaTileEntityLightGroundVehicleFactory LIGHT_GROUND_VEHICLE_FACTORY;
    public static MetaTileEntityLargeTransformer LARGE_TRANSFORMER;
    public static MetaTileEntityACHatch AC_INPUT_HATCH;
    public static MetaTileEntityACHatch AC_OUTPUT_HATCH;

    public static int id = 10000;
    public static void init() {
        //Multis
        LARGEBLASTFURNACE = registerMetaTileEntity(id++, new MetaTileEntityWarfactoryBlastFurnace(location("largeblastfurnace")));
        RADAR = registerMetaTileEntity(id++, new MetaTileEntityRadar(location("radar")));
        COMPUTER = registerMetaTileEntity(id++, new MetaTileEntityComputer(location("computer"), COMPUTER_RECIPE_MAP));
        CPU_SLOT = registerMetaTileEntity(id++, new MetaTileEntityCPUSlot(location("cpu_slot"), GTValues.LV));
        RAM_SLOT_MV = registerMetaTileEntity(id++, new MetaTileEntityRAMSlot(location("ram_slot.mv"), GTValues.MV));
        RAM_SLOT_HV = registerMetaTileEntity(id++, new MetaTileEntityRAMSlot(location("ram_slot.hv"), GTValues.HV));
        PASSIVE_COOLER_MV = registerMetaTileEntity(id++, new MetaTileEntityCooling(location("cooler_passive.mv"), GTValues.MV,false));
        LIQUID_COOLER_MV = registerMetaTileEntity(id++, new MetaTileEntityCooling(location("cooler_liquid.mv"), GTValues.MV,true));
        MAINFRAME = registerMetaTileEntity(id++, new MetaTileEntityMainframe(location("mainframe")));
        RESEARCH_UNIT = registerMetaTileEntity(id++, new MetaTileEntityResearchUnit(location("research_unit")));
        PRINTER = registerMetaTileEntity(id++, new MetaTileEntityPrinter(location("printer"), GTValues.EV));
        LIGHT_GROUND_VEHICLE_FACTORY = registerMetaTileEntity(id++,
                new MetaTileEntityLightGroundVehicleFactory(location("light_ground_vehicle_factory")));
        LARGE_TRANSFORMER = registerMetaTileEntity(id++, new MetaTileEntityLargeTransformer(location("large_transformer")));
        AC_INPUT_HATCH = registerMetaTileEntity(id++, new MetaTileEntityACHatch(location("ac_hatch.input"), GTValues.EV, false));
        AC_OUTPUT_HATCH = registerMetaTileEntity(id++, new MetaTileEntityACHatch(location("ac_hatch.output"), GTValues.EV, true));

    }

    private static ResourceLocation location(@NotNull String name) {
        return new ResourceLocation(Reference.MODID, name);
    }
}