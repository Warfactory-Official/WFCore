package wfcore.common.items.registry;

import gregtech.api.GTValues;
import gregtech.common.items.MetaItems;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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

            event.getToolTip().add(String.format("%s %s: %s%d CWU/t",
                    TextFormatting.GRAY, I18n.format("wfcore.tooltip.base_cwu"),
                    TextFormatting.AQUA, stats.baseCWU()));

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
        register(MetaItems.INTEGRATED_CIRCUIT_MV.getStackForm(), new CPUEntry(5, 0.8, GTValues.V[GTValues.HV], GTValues.VH[GTValues.MV]));
    }

    public record CPUEntry(
            int baseCWU,      // Computation units per tick
            double efficiency, // 0.0 to 1.0 (lower = more heat)
            long maxPower,     // Max power draw (EU/t)
            long minPower      // Idle/Baseline power draw (EU/t)
    ) {
    }

    public static class ItemStackHashStrategy implements Hash.Strategy<ItemStack> {

        @Override
        public int hashCode(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return 0;
            int result = stack.getItem().hashCode();
            result = 31 * result + stack.getMetadata();
            if (stack.hasTagCompound()) {
                result = 31 * result + stack.getTagCompound().hashCode();
            }
            return result;
        }

        @Override
        public boolean equals(ItemStack s1, ItemStack s2) {
            if (s1 == s2) return true;
            if (s1 == null || s2 == null) return false;
            return s1.isItemEqual(s2) && ItemStack.areItemStackTagsEqual(s1, s2);
        }
    }
}
