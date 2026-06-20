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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import wfcore.common.fluid.CoolantRegistry;
import wfcore.common.metatileentities.WFCoreAbilities;

import java.util.List;

public class MetaTileEntityCooling extends MetaTileEntityMultiblockPart implements ICooler, IMultiblockAbilityPart<ICooler> {

    @Getter
    private final boolean isLiquid;

    public MetaTileEntityCooling(ResourceLocation metaTileEntityId, int tier, boolean isLiquid) {
        super(metaTileEntityId, tier);
        this.isLiquid = isLiquid;
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
    public double getPassiveCoolingRate(double currentTemp, double thermalMass, double ambient) {
        if (isLiquid || currentTemp <= ambient) return 0;

        int fanTier = getFanTier();
        double coolingCoefficient = (fanTier > 0) ? 0.05 * fanTier : 0.01;

        return (coolingCoefficient * (currentTemp - ambient)) / thermalMass;
    }

    @Override
    public double getMaxActiveCoolingRate(double thermalMass) {
        if (!isLiquid) return 0;

        FluidStack stack = getImportFluids().drain(1, false); // Peek
        if (stack == null) return 0;

        CoolantRegistry.CoolantSettings settings = CoolantRegistry.get(stack.getFluid());
        if (settings == null) return 0;

        // 100mB is your max per tick per hatch
        return (100 * settings.heatCapacity()) / thermalMass;
    }

    @Override
    public int getFluidUsagePerTick() {
        return isLiquid ? 100 : 0;
    }

    @Override
    public double executeActiveCooling(double percentage, double thermalMass, IFluidHandler in, IFluidHandler out) {
        if (!isLiquid || percentage <= 0 || in == null) return 0;

        for (IFluidTankProperties prop : in.getTankProperties()) {
            FluidStack stack = prop.getContents();
            if (stack == null || stack.amount <= 0) continue;

            CoolantRegistry.CoolantSettings settings = CoolantRegistry.get(stack.getFluid());
            if (settings == null) continue;

            // Base max drain per tick
            int maxDrain = 100;
            int amountToDrain = (int) Math.ceil(maxDrain * percentage);
            amountToDrain = Math.min(amountToDrain, stack.amount);

            if (amountToDrain <= 0) continue;

            // Define exactly what we want to drain
            FluidStack drainTarget = new FluidStack(stack.getFluid(), amountToDrain);

            if (settings.hotVariant() != null) {
                // Failsafe: if there is no output hatch for a closed loop, we can't proceed
                if (out == null) continue;

                FluidStack hotStack = new FluidStack(settings.hotVariant(), amountToDrain);

                // Check if there is enough space in the output handler
                if (out.fill(hotStack, false) == amountToDrain) {

                    // Actually perform the drain from the input
                    FluidStack actuallyDrained = in.drain(drainTarget, true);

                    if (actuallyDrained != null && actuallyDrained.amount > 0) {
                        // Ensure we only output exactly what we successfully drained
                        hotStack.amount = actuallyDrained.amount;
                        out.fill(hotStack, true);
                        return (actuallyDrained.amount * settings.heatCapacity()) / thermalMass;
                    }
                }
            }
            else {
                FluidStack actuallyDrained = in.drain(drainTarget, true);

                if (actuallyDrained != null && actuallyDrained.amount > 0) {
                    return (actuallyDrained.amount * settings.heatCapacity()) / thermalMass;
                }
            }
        }

        return 0;
    }

    @Override
    protected boolean openGUIOnRightClick() {
        return false;
    }

    private int getFanTier() {
        if (getHolder() == null) return 0;

        EnumFacing exposedFace = getFrontFacing();
        Cover cover = getCoverAtSide(exposedFace);

        if (cover instanceof wfcore.common.covers.CoverCoolingFan fan) {
            return fan.getTier();
        }
        return 0;
    }
}



