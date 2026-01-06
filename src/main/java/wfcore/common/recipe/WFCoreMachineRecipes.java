package wfcore.common.recipe;

import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.widgets.ProgressWidget;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.recipes.RecyclingHandler;
import gregtech.api.recipes.builders.AssemblerRecipeBuilder;
import gregtech.api.unification.OreDictUnifier;
import gregtech.api.unification.material.MarkerMaterials;
import gregtech.api.unification.material.Materials;
import gregtech.api.unification.ore.OrePrefix;
import gregtech.api.unification.stack.ItemMaterialInfo;
import gregtech.common.blocks.MetaBlocks;
import gregtech.core.sound.GTSoundEvents;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import wfcore.common.recipe.builder.ComputerRecipeBuilder;

import java.math.BigInteger;

import static gregtech.api.GTValues.MV;
import static gregtech.api.GTValues.VA;

public class WFCoreMachineRecipes {
    public static final RecipeMap<ComputerRecipeBuilder> COMPUTER_RECIPE_MAP = new RecipeMap<ComputerRecipeBuilder>(
            "computer", 2, 0, 1, 0,
            new ComputerRecipeBuilder(), false)
            .setSlotOverlay(false, false, GuiTextures.CIRCUIT_OVERLAY)
            .setProgressBar(GuiTextures.PROGRESS_BAR_CIRCUIT, ProgressWidget.MoveType.HORIZONTAL)
            .setSound(GTSoundEvents.ASSEMBLER);

    public static void initRecipes() {
        COMPUTER_RECIPE_MAP.recipeBuilder()
                .compute(BigInteger.valueOf(256))
                .input(OrePrefix.circuit, MarkerMaterials.Tier.MV, 1)
                .fluidInputs(Materials.Water.getFluid(50))
                .outputs(MetaBlocks.METAL_SHEET.getItemVariant(EnumDyeColor.WHITE, 32))
                .duration(20)
                .EUt(VA[MV])
                .buildAndRegister();
    }
}
