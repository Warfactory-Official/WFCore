package wfcore.common.metatileentities.compute;

public interface ICpuSlot {

    int getEstimatedCWU(int power);
    int getBaseCWU();
    double getEfficiency();
    long getVoltageIn();
    double getHeatGenerated(int power);







}
