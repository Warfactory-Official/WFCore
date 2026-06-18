package wfcore.integration.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import wfcore.Reference;

import java.util.List;

/** JEI/HEI recipe category for WFCore researches. */
public class ResearchCategory implements IRecipeCategory<ResearchWrapper> {

    public static final String UID = "wfcore:research";

    private final IDrawable background;
    private final IDrawable icon;

    public ResearchCategory(IGuiHelper guiHelper, ItemStack iconStack) {
        this.background = guiHelper.createBlankDrawable(160, 96);
        this.icon = iconStack.isEmpty() ? null : guiHelper.createDrawableIngredient(iconStack);
    }

    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return I18n.format("wfcore.jei.research.title");
    }

    @Override
    public String getModName() {
        return Reference.MODNAME;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayout layout, ResearchWrapper wrapper, IIngredients ingredients) {
        IGuiItemStackGroup group = layout.getItemStacks();
        List<ItemStack> inputs = wrapper.getResearch().getItemsPerRun();
        for (int i = 0; i < inputs.size(); i++) {
            group.init(i, true, 2, 22 + i * 18);
            group.set(i, inputs.get(i));
        }
        List<ItemStack> outputs = wrapper.outputs();
        for (int j = 0; j < outputs.size(); j++) {
            int slot = inputs.size() + j;
            group.init(slot, false, 140, 22 + j * 18);
            group.set(slot, outputs.get(j));
        }
    }
}
