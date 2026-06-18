package wfcore.common.metatileentities.compute;

import net.minecraftforge.fluids.capability.IFluidHandler;

public interface ICooler {

    boolean isLiquid();

    // Passive: How much can you cool right now?
    double getPassiveCoolingRate(double currentTemp, double thermalMass, double ambient);

    // Active: What is your max potential and what is the cost?
    double getMaxActiveCoolingRate(double thermalMass);
    int getFluidUsagePerTick();

    // The Action: Actually drain the fluid and return the cooling done
    double executeActiveCooling(double percentage, double thermalMass, IFluidHandler fluidIn, IFluidHandler fluidOut);

}
