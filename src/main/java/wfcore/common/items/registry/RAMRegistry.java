package wfcore.common.items.registry;

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
import wfcore.common.items.ItemRegistry;

import java.util.Collections;
import java.util.Map;

public class RAMRegistry {

    // Reusing the same strategy logic for consistent ItemStack lookups
    private static final Object2ObjectOpenCustomHashMap<ItemStack, RAMEntry> REGISTRY =
            new Object2ObjectOpenCustomHashMap<>(new ItemStackHashStrategy());

    public static void register(ItemStack stack, int throughput) {
        ItemStack key = stack.copy();
        key.setCount(1);
        REGISTRY.put(key, new RAMEntry(throughput));
    }

    public static RAMEntry getEntry(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return REGISTRY.get(stack);
    }

    public static boolean isRAM(ItemStack stack) {
        return REGISTRY.containsKey(stack);
    }

    public static Map<ItemStack, RAMEntry> getDefinitions() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        RAMEntry stats = RAMRegistry.getEntry(event.getItemStack());

        if (stats != null) {
            event.getToolTip().add(TextFormatting.GOLD + I18n.format("wfcore.tooltip.ram_stats"));

            event.getToolTip().add(String.format("%s %s: %s%d CWU/t",
                    TextFormatting.GRAY, I18n.format("wfcore.tooltip.throughput"),
                    TextFormatting.LIGHT_PURPLE, stats.throughput()));
        }
    }

    // Example registration
    public static void register() {
        register( new ItemStack(ItemRegistry.EIGHT_CARROT), 256);
    }

    /**
     * @param throughput Max CWU this RAM can handle per tick
     */
    public record RAMEntry(int throughput) {}
}
