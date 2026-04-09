package wfcore.common.items;

import gregtech.api.block.VariantBlock;
import gregtech.api.block.VariantItemBlock;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import wfcore.common.drones.ItemSuicideDrone;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@SuppressWarnings("unused")
public class ItemRegistry {

    //Declare public static final items here
    public static final Set<Item> ITEMS =  new HashSet<>();

    //Texture should be in assets/wfcore/textures/items
    public static final Item EIGHT_CARROT = new ItemEightCarrot("eight_carrot");

    public static final ItemSuicideDrone ITEM_SUICIDE_DRONE = new ItemSuicideDrone("suicide_drone");

    public static final RadarProbe RADAR_PROBE = new RadarProbe("radar_probe", "radar_probe");
    public static final PenDrive PEN_DRIVE = new PenDrive("pen_drive", "pen_drive");


    public static Optional<ItemBlock> createItemBlock(Block block, Function<Block, ItemBlock> producer) {
        if(producer == null) return Optional.empty();
        ItemBlock itemBlock = producer.apply(block);
        itemBlock.setRegistryName(Objects.requireNonNull(block.getRegistryName()));
        return Optional.of(itemBlock);
    }

    public static ItemBlock createVariantItemBlockUnchecked(VariantBlock<?> block) {
        return new VariantItemBlock(block);
    }



}
