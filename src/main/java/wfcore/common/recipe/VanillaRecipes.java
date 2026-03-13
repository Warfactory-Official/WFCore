package wfcore.common.recipe;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import gregtech.api.GTValues;
import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.items.MetaItems;
import gregtech.common.metatileentities.MetaTileEntities;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.registries.ForgeRegistry;
import org.jetbrains.annotations.NotNull;
import wfcore.RefStrings;
import wfcore.api.util.FurnaceUtil;
import wfcore.common.items.ItemRegistry;

import java.util.HashSet;
import java.util.Set;

import static wfcore.WFCore.MODID;

//this is where all the vanilla crafting type recipes (crafting table, furance, etc) we are modifying are stored.

public class VanillaRecipes {
    private static final Set<IRecipe> RECIPES = new HashSet<>();

    public static void registerFurnaceRecipes(RegistryEvent.Register<IRecipe> event) {
        FurnaceUtil.removeByOutput(MetaItems.FIRECLAY_BRICK.getStackForm(1));
        FurnaceRecipes.instance().addSmelting(Items.CARROT, new ItemStack(ItemRegistry.EIGHT_CARROT), 1);
    }

    public static void registerCTRecipes(RegistryEvent.Register<IRecipe> event) {
        ForgeRegistry<IRecipe> registry = (ForgeRegistry<IRecipe>) event.getRegistry();

//Steam Age machines
        registry.remove(new ResourceLocation(RefStrings.HBM, "machine_ammo_press"));

        //ammo press recipe
        new ShapedOreRecSelfReg(
                new ResourceLocation(MODID, "hbm_ammo_press"),
                new ItemStack(ModBlocks.machine_ammo_press),
                "SCS",
                "P P",
                "PPP",
                'S', "screwSteel",
                'P', "plateSteel",
                'C', Blocks.PISTON
        ).setRegistryName(MODID, "hbm_ammo_press");

        //combination oven
        registry.remove(new ResourceLocation(RefStrings.HBM, "furnace_combination"));

        new ShapedOreRecSelfReg(
                new ResourceLocation(MODID, "furnace_combination"),
                new ItemStack(ModBlocks.furnace_combination),
                "BBB",
                "SIS",
                "III",
                'B', "blockBrick",
                'S', "stone",
                'I', "plateIron"
        ).setRegistryName(MODID, "furnace_combination" +
                "on");

        //firebox


        new ShapedOreRecSelfReg(
                new ResourceLocation(MODID, "heater_firebox"),
                new ItemStack(ModBlocks.heater_firebox),
                "PPP",
                "PCP",
                "PPP",
                'P', "plateDoubleWroughtIron",
                'C', "plateCopper"
        ).setRegistryName(MODID, "heater_firebox" +
                "on");

        //soldering station

        //registers GT ULV hull as an Itemstack to use in 3x3 crafting
        ItemStack ulvHull = MetaTileEntities.HULL[GTValues.ULV].getStackForm();
        new ShapedOreRecSelfReg(
                new ResourceLocation(MODID, "machine_soldering_station"),
                new ItemStack(ModBlocks.machine_soldering_station),
                "KPK",
                "PCP",
                "KPK",
                'P', "plateSteel",
                'C', ulvHull,
                'K', ModItems.coil_copper
        ).setRegistryName(MODID, "machine_soldering_station" +
                "on");

        // copper coil recipe
        registry.remove(new ResourceLocation(RefStrings.HBM, "coil_copper"));

        new ShapedOreRecSelfReg(
                new ResourceLocation(MODID, "coil_copper"),
                new ItemStack(ModItems.coil_copper),
                "PPP",
                "PCP",
                "PPP",
                'P', "wireFineCopper",
                'C', "stickSteel"
        ).setRegistryName(MODID, "coil_copper" +
                "on");

        //crucible recipe

        registry.remove(new ResourceLocation(RefStrings.HBM, "machine_crucible"));
        new ShapedOreRecSelfReg(
                new ResourceLocation(MODID, "machine_crucible"),
                new ItemStack(ModBlocks.machine_crucible),
                "BSB",
                "BCB",
                "PSP",
                'P', "plateWroughtIron",
                'C', "plateBronze",
                'S', "stickLongBronze",
                'B', "blockBrick"

        ).setRegistryName(MODID, "machine_crucible" +
                "on");

        //basin recipe

        registry.remove(new ResourceLocation(RefStrings.HBM, "foundry_basin"));
        new ShapedOreRecSelfReg(
                new ResourceLocation(MODID, "foundry_basin"),
                new ItemStack(ModBlocks.foundry_basin),

                "BCB",
                "BCB",
                "BBB",

                'C', "plateBronze",
                'B', "blockBrick"
        ).setRegistryName(MODID, "foundry_basin" +
                "on");

        // small basin recipe

        registry.remove(new ResourceLocation(RefStrings.HBM, "foundry_mold"));
        new ShapedOreRecSelfReg(
                new ResourceLocation(MODID, "foundry_mold"),
                new ItemStack(ModBlocks.foundry_mold),


                "BCB",
                "BBB",

                'C', "plateBronze",
                'B', "blockBrick"
        ).setRegistryName(MODID, "foundry_mold" +
                "on");


        // small basin recipe

        registry.remove(new ResourceLocation(RefStrings.HBM, "foundry_mold"));
        new ShapedOreRecSelfReg(
                new ResourceLocation(MODID, "foundry_mold"),
                new ItemStack(ModBlocks.foundry_mold),


                "BCB",
                "BBB",

                'C', "plateBronze",
                'B', "blockBrick"
        ).setRegistryName(MODID, "foundry_mold" +
                "on");

        //Stirling Engine crafting

        registry.remove(new ResourceLocation(RefStrings.HBM, "machine_stirling"));
        new ShapedOreRecSelfReg(
                new ResourceLocation(MODID, "machine_stirling"),
                new ItemStack(ModBlocks.machine_stirling),

                " W ",
                "BCB",
                "BBB",
                'C', "gearInvar",
                'W', ModItems.gear_large,
                'B', "plateSteel"
        ).setRegistryName(MODID, "machine_stirling" +
                "on");

        // Large gear recipe"gear_large"
        registry.remove(new ResourceLocation(RefStrings.HBM, "gear_large"));
        new ShapedOreRecSelfReg(
                new ResourceLocation(MODID, "gear_large"),
                new ItemStack(ModItems.gear_large),

                "WBW",
                "BCB",
                "WBW",
                'C', "gearInvar",
                'B', "stickBronze",
                'W', "plateInvar"
        ).setRegistryName(MODID, "gear_large" +
                "on");
//other Steam age recipe removals

        registry.remove(new ResourceLocation(RefStrings.GT, "electronic_circuit_lv"));
        registry.remove(new ResourceLocation(RefStrings.HBM, "circuit_2"));
        registry.remove(new ResourceLocation(RefStrings.HBM, "circuit"));
        registry.remove(new ResourceLocation(RefStrings.HBM, "plate_polymer_5"));
        registry.remove(new ResourceLocation(RefStrings.HBM, "plate_polymer_4"));

        registry.remove(new ResourceLocation(RefStrings.GT, "electronic_circuit_lv"));


// rubber w/o sulfur


        ItemStack rubberIngot = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("gregtech:meta_ingot")), 1, 1068);
        ItemStack rawRubber = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("gregtech:meta_dust")), 1, 1002);
        ItemStack Hammer = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("gregtech:hammer")), 1);
        new VanillaRecipes.ShapedOreRecSelfReg(
                new ResourceLocation(MODID, "rubber_ingot"),
                rubberIngot.copy(),


                " H ",
                " R ",
                " R ",
                'H', Hammer,
                'R', rawRubber


        ).setRegistryName(MODID, "rubber_ingot_hammer");


//PBF recipe chain


        ItemStack firebrick = MetaItems.FIRECLAY_BRICK.getStackForm();
        ItemStack castCopper = ModItems.plate_copper.getDefaultInstance();
        ItemStack screwItem = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("gregtech:meta_screw")), 1, 260);


        ItemStack PBFbrick = MetaBlocks.METAL_CASING.getItemVariant(BlockMetalCasing.MetalCasingType.PRIMITIVE_BRICKS, 3);


        registry.remove(new ResourceLocation(RefStrings.GT, "casing_primitive_bricks"));


        new ShapedOreRecSelfReg(
                new ResourceLocation(MODID, "casing_primitive_bricks"),
                PBFbrick.copy(),


                "WSW",
                "SCS",
                "WSW",
                'C', castCopper,
                'W', firebrick,
                'S', screwItem
        ).setRegistryName(MODID, "casing_primitive_bricks");


        // LV Recipes
        // registry.remove(new ResourceLocation(RefStrings.HBM, "machine_stirling_steel"));


        registry.remove(new ResourceLocation(RefStrings.HBM, "gear_large_1"));

        RECIPES.forEach(registry::register);

    }


    public static class ShapedOreRecSelfReg extends ShapedOreRecipe {

        public ShapedOreRecSelfReg(ResourceLocation group, Block result, Object... recipe) {
            super(group, result, recipe);
            RECIPES.add(this);
        }

        public ShapedOreRecSelfReg(ResourceLocation group, Item result, Object... recipe) {
            super(group, result, recipe);
            RECIPES.add(this);
        }

        public ShapedOreRecSelfReg(ResourceLocation group, @NotNull ItemStack result, Object... recipe) {
            super(group, result, recipe);
            RECIPES.add(this);
        }

        public ShapedOreRecSelfReg(ResourceLocation group, @NotNull ItemStack result, CraftingHelper.ShapedPrimer primer) {
            super(group, result, primer);
            RECIPES.add(this);
        }
    }

}
