package wfcore.common.research;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import wfcore.api.research.Research;

import java.util.List;

/**
 * Example WFCore research tree (Java API). Packs/addons add their own via {@link Research#builder(String)}
 * or, at runtime, through GroovyScript ({@code mods.wfcore.research}). Grid positions place tree nodes.
 */
public final class WFResearches {

    private WFResearches() {}

    public static void register() {
        Research.builder("basic_electronics")
                .icon(new ItemStack(Items.REPEATER))
                .pos(0, 1)
                .runs(20)
                .itemsPerRun(List.of(new ItemStack(Items.REDSTONE, 2), new ItemStack(Items.IRON_INGOT, 1)))
                .cwuPerRun(160).eut(32).ticksPerRun(40)
                .unlocks(new ItemStack(Items.REPEATER), new ItemStack(Items.COMPARATOR))
                .blueprint()
                .register();

        Research.builder("advanced_circuits")
                .icon(new ItemStack(Items.COMPARATOR))
                .pos(1, 0)
                .requires("basic_electronics")
                .runs(40)
                .itemsPerRun(List.of(new ItemStack(Items.REDSTONE, 4), new ItemStack(Items.GOLD_INGOT, 1)))
                .cwuPerRun(320).eut(32).ticksPerRun(40)
                .unlocks(new ItemStack(Blocks.DAYLIGHT_DETECTOR))
                .blueprint()
                .register();

        Research.builder("sensor_arrays")
                .icon(new ItemStack(Blocks.OBSERVER))
                .pos(1, 2)
                .requires("basic_electronics")
                .runs(30)
                .itemsPerRun(List.of(new ItemStack(Items.QUARTZ, 2)))
                .cwuPerRun(240).eut(32).ticksPerRun(40)
                .register();

        Research.builder("fire_control_systems")
                .icon(new ItemStack(Items.CLOCK))
                .pos(2, 1)
                .requires("advanced_circuits", "sensor_arrays")
                .runs(60)
                .itemsPerRun(List.of(new ItemStack(Items.DIAMOND, 1)))
                .cwuPerRun(640).eut(32).ticksPerRun(40)
                .unlocks(new ItemStack(Items.CLOCK), new ItemStack(Blocks.DISPENSER))
                .blueprint()
                .register();
    }
}
