package wfcore.integration.jei;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import wfcore.api.research.Research;

import java.util.ArrayList;
import java.util.List;

/** JEI/HEI wrapper presenting a {@link Research} as a recipe: per-run inputs, unlocked outputs, cost text. */
public class ResearchWrapper implements IRecipeWrapper {

    private final Research research;

    public ResearchWrapper(Research research) {
        this.research = research;
    }

    public Research getResearch() {
        return research;
    }

    /** Outputs shown in JEI - the unlocked items, or the icon if nothing is unlocked. */
    public List<ItemStack> outputs() {
        List<ItemStack> outs = new ArrayList<>(research.getUnlockedItems());
        if (outs.isEmpty() && !research.getIcon().isEmpty()) {
            outs.add(research.getIcon());
        }
        return outs;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        if (!research.getItemsPerRun().isEmpty()) {
            ingredients.setInputs(VanillaTypes.ITEM, research.getItemsPerRun());
        }
        List<ItemStack> outs = outputs();
        if (!outs.isEmpty()) {
            ingredients.setOutputs(VanillaTypes.ITEM, outs);
        }
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        FontRenderer font = minecraft.fontRenderer;
        font.drawString(I18n.format(research.getNameKey()), 2, 2, 0x404040);
        int x = 26;
        int y = 24;
        font.drawString(I18n.format("wfcore.jei.runs", research.getRunsRequired()), x, y, 0x707070);
        font.drawString(I18n.format("wfcore.jei.cwu", research.getCwuPerRun()), x, y + 10, 0x707070);
        font.drawString(I18n.format("wfcore.jei.eut", research.getEut()), x, y + 20, 0x707070);
        font.drawString(I18n.format("wfcore.jei.total_cwu", research.getTotalCWU()), x, y + 30, 0x707070);
    }
}
