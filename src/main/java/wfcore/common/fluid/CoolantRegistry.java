package wfcore.common.fluid;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import jakarta.annotation.Nullable;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

public class CoolantRegistry {

    private static final Reference2ObjectOpenHashMap<Fluid, CoolantSettings> COOLANTS =
            new Reference2ObjectOpenHashMap<>();

    public record CoolantSettings(Fluid hotVariant, double heatCapacity) {}

    public static void register() {
        register(FluidRegistry.WATER, FluidRegistry.getFluid("hot_water"), 1.0); //TODO: add hot water texture
        register(FluidRegistry.getFluid("oxygen"), null, 3.0);
        register(FluidRegistry.getFluid("helium"), null, 6.0);
        register(FluidRegistry.getFluid("liquid_nitrogen"), FluidRegistry.getFluid("nitrogen"), 10.0);
    }

    public static void register(Fluid cold, @Nullable Fluid hot, double capacity) {
        if (cold == null) return;
        COOLANTS.put(cold, new CoolantSettings(hot, capacity));
    }

    public static CoolantSettings get(Fluid fluid) {
        if (fluid == null) return null;
        return COOLANTS.get(fluid);
    }

    public static boolean isCoolant(Fluid fluid) {
        return COOLANTS.containsKey(fluid);
    }
}
