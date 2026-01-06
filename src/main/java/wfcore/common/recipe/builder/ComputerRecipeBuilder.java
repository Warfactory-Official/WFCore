package wfcore.common.recipe.builder;

import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeBuilder;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.recipes.builders.BlastRecipeBuilder;
import gregtech.api.recipes.recipeproperties.TemperatureProperty;
import gregtech.api.util.EnumValidationResult;
import gregtech.api.util.GTLog;
import org.jetbrains.annotations.NotNull;
import wfcore.common.recipe.properties.ComputeProperty;

import java.math.BigInteger;

public class ComputerRecipeBuilder extends RecipeBuilder<ComputerRecipeBuilder> {
    public ComputerRecipeBuilder() {}

    public ComputerRecipeBuilder(ComputerRecipeBuilder builder) { super(builder); }

    public ComputerRecipeBuilder(Recipe recipe, RecipeMap<ComputerRecipeBuilder> recipeMap) { super(recipe, recipeMap); }

    @Override
    public ComputerRecipeBuilder copy() {
        return new ComputerRecipeBuilder(this);
    }

    @Override
    public boolean applyProperty(@NotNull String key, Object value) {
        if (key.equals(ComputeProperty.KEY)) {
            this.compute((BigInteger) value);
            return true;
        }

        return super.applyProperty(key, value);
    }

    public ComputerRecipeBuilder compute(BigInteger numOps) {
        if (numOps.compareTo(BigInteger.ZERO) < 0) {
            GTLog.logger.error("Number of compute operations produced cannot be less than 0.",
                    new IllegalArgumentException());
            recipeStatus = EnumValidationResult.INVALID;
        }

        this.applyProperty(ComputeProperty.getInstance(), numOps);
        return this;
    }
}
