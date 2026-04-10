package wfcore.common.metatileentities.compute;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.cover.Cover;
import gregtech.api.gui.ModularUI;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityMultiblockPart;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import wfcore.common.fluid.CoolantRegistry;
import wfcore.common.metatileentities.WFCoreAbilities;

import java.util.List;

public class MetaTileEntityCooling extends MetaTileEntityMultiblockPart implements ICooler, IMultiblockAbilityPart<ICooler> {

    @Getter
    private final boolean isLiquid;
    @Getter
    private double AMBIENT = Double.MIN_VALUE;

    public MetaTileEntityCooling(ResourceLocation metaTileEntityId, int tier, boolean isLiquid) {
        super(metaTileEntityId, tier);
        this.isLiquid = isLiquid;
    }

    private double setAmbient() {
        if (getWorld() == null || getPos() == null) return 22;

        switch (getWorld().provider.getDimension()) {
            case -1 -> {
                return 70.0;
            }
            case 1 -> {
                return 5.0;
            }
            default -> {
                Biome biome = getWorld().getBiome(getPos());
                float temp = biome.getTemperature(getPos());
                double celcius = (temp * 30) - 5;
                return celcius;
            }
        }
    }


    @Override
    public void update() {
        super.update();
        if (isFirstTick()) {
            this.AMBIENT = setAmbient();
        }
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityCooling(metaTileEntityId, getTier(), isLiquid);
    }

    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        return null;
    }


    @Override
    public MultiblockAbility<ICooler> getAbility() {
        return WFCoreAbilities.GPC_COOLER;
    }

    @Override
    public void registerAbilities(List<ICooler> abilityList) {
        abilityList.add(this);

    }

    @Override
    public double applyCooling(double currentTemp, double thermalMass) {

        if(isLiquid){
            return applyFluidCooling(thermalMass);
        }


        if (currentTemp <= AMBIENT) return 0;
        int fanTier = getFanTier();
        double coolingCoefficient = 0.01;
        if (fanTier > 0)
            coolingCoefficient = 0.05 * fanTier;


        return (coolingCoefficient * (currentTemp - AMBIENT)) / thermalMass;

    }

    private double applyFluidCooling(double thermalMass) {
        IMultipleTankHandler inputTanks = getImportFluids();
        IMultipleTankHandler outputTanks = getExportFluids();

        for (IFluidTank tank : inputTanks.getFluidTanks()) {
            FluidStack stack = tank.getFluid();
            if (stack == null) continue;

            CoolantRegistry.CoolantSettings settings = CoolantRegistry.get(stack.getFluid());
            if (settings != null) {
                int amountToDrain = Math.min(stack.amount, 100);

                FluidStack hotStack = new FluidStack(settings.hotVariant(), amountToDrain);
                if (outputTanks.fill(hotStack, false) == amountToDrain) {

                    tank.drain(amountToDrain, true);
                    outputTanks.fill(hotStack, true);

                    return (amountToDrain * settings.heatCapacity()) / thermalMass;
                }
            }
        }
        return 0;
    }

    private int getFanTier() {
        if (getHolder() == null) return 0;

        EnumFacing exposedFace = getFrontFacing();
        Cover cover = getCoverAtSide(exposedFace);

        if (cover != null) {
            return 1;
        }
        return 0;
    }
}



