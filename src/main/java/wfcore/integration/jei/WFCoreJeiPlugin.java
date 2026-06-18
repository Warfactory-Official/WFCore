package wfcore.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import wfcore.api.research.Research;
import wfcore.api.research.ResearchRegistry;
import wfcore.common.metatileentities.WFCoreMetaTileEntities;

import java.util.ArrayList;
import java.util.List;

/** JEI/HEI plugin (auto-discovered via @JEIPlugin) registering the research category + recipes. */
@JEIPlugin
public class WFCoreJeiPlugin implements IModPlugin {

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ResearchCategory(
                registration.getJeiHelpers().getGuiHelper(),
                WFCoreMetaTileEntities.RESEARCH_UNIT.getStackForm()));
    }

    @Override
    public void register(IModRegistry registry) {
        List<ResearchWrapper> recipes = new ArrayList<>();
        for (Research research : ResearchRegistry.all()) {
            recipes.add(new ResearchWrapper(research));
        }
        registry.addRecipes(recipes, ResearchCategory.UID);
        registry.addRecipeCatalyst(WFCoreMetaTileEntities.RESEARCH_UNIT.getStackForm(), ResearchCategory.UID);
    }
}
