package wfcore.common.metatileentities.compute;

public interface ICooler {

    public boolean isLiquid();
    public double applyCooling(double currentTemp, double thermalMass);

}
