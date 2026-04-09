package wfcore.common.metatileentities.compute;

import org.jetbrains.annotations.Nullable;
import wfcore.common.items.registry.CPURegistry;

public interface ICpuSlot {

    double getEstimatedCWU(int power, double temp);

    @Nullable
    CPURegistry.CPUEntry getStats();






}
