package wfcore.common.items.registry;

import gregtech.api.GTValues;
import gregtech.common.items.MetaItems;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import wfcore.api.items.ItemStackHashStrategy;

import java.util.Collections;
import java.util.Map;

public class CPURegistry {

    // The Master Map: Key is the ItemStack (Strategy handled), Value is the Stats
    private static final Object2ObjectOpenCustomHashMap<ItemStack, CPUEntry> REGISTRY =
            new Object2ObjectOpenCustomHashMap<>(new ItemStackHashStrategy());

    /**
     * Registers a CPU item with specific stats.
     */
    public static void register(ItemStack stack, CPUEntry entry) {
        ItemStack key = stack.copy();
        key.setCount(1); // Ensure stack size doesn't affect the key
        REGISTRY.put(key, entry);
    }

    /**
     * Gets the CPU stats for a given stack. Returns null if not a registered CPU.
     */
    public static CPUEntry getEntry(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return REGISTRY.get(stack);
    }

    public static boolean isCPU(ItemStack stack) {
        return REGISTRY.containsKey(stack);
    }

    /**
     * Returns a read-only view of all registered CPUs.
     */
    public static Map<ItemStack, CPUEntry> getDefinitions() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        CPUEntry stats = CPURegistry.getEntry(event.getItemStack());

        if (stats != null) {
            event.getToolTip().add(TextFormatting.GOLD + I18n.format("wfcore.tooltip.cpu_stats"));

            event.getToolTip().add(String.format("%s %s: %s%.1f%%",
                    TextFormatting.GRAY, I18n.format("wfcore.tooltip.efficiency"),
                    TextFormatting.GREEN, stats.efficiency() * 100));

            event.getToolTip().add(String.format("%s %s: %s%d - %d EU/t",
                    TextFormatting.GRAY, I18n.format("wfcore.tooltip.power_draw"),
                    TextFormatting.RED, stats.minPower(), stats.maxPower()));

            if (stats.efficiency() < 0.6) {
                event.getToolTip().add(TextFormatting.DARK_RED + I18n.format("wfcore.tooltip.high_heat_warning"));
            }
        }
    }

    public static void register() {
        register(MetaItems.INTEGRATED_CIRCUIT_MV.getStackForm(), new CPUEntry(0.5, GTValues.V[GTValues.HV], GTValues.VH[GTValues.MV]));
    }

    public record CPUEntry(
            double efficiency, // 0.0 to 1.0 (lower = more heat)
            long maxPower,     // Max power draw (EU/t)
            long minPower      // Idle/Baseline power draw (EU/t)
    ) {
        public double getCurrentEfficency(long power) {
            if (power < minPower) return 0;
            double load = (double) (power - minPower) / (maxPower - power);
            double dropoff = 0.2 * Math.pow(load, 2);
            return Math.max(0.05, efficiency - dropoff);
        }


        public long getCWU(long power, double currentTemp) {
            double tempPenalty = 0;
            if (currentTemp > 90) ;
            tempPenalty = Math.pow((currentTemp - 90) / 10, 2) * 0.5;
            double eff = Math.max(0.01, getCurrentEfficency(power) - tempPenalty);
            return (long) (power * eff);
        }

        public long getHeat(long power, double currentTemp) {
            return power - getCWU(power, currentTemp);
        }

    }


}
