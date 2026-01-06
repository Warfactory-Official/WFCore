package wfcore.common.recipe.properties;

import gregtech.api.metatileentity.multiblock.CleanroomType;
import gregtech.api.recipes.recipeproperties.CleanroomProperty;
import gregtech.api.recipes.recipeproperties.RecipeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

public class ComputeProperty extends RecipeProperty<BigInteger> {
    public static final String KEY = "compute";

    private static ComputeProperty INSTANCE;

    private ComputeProperty() {
        super(KEY, BigInteger.class);
    }

    public static ComputeProperty getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ComputeProperty();
        }

        return INSTANCE;
    }

    @Override
    public void drawInfo(@NotNull Minecraft minecraft, int x, int y, int color, Object value) {
        BigInteger type = castValue(value);
        if (type == null) return;

        minecraft.fontRenderer.drawString(I18n.format("gregtech.recipe.ops", getName(type)), x, y, color);
    }

    @Override
    public int getInfoHeight(Object value) {
        BigInteger type = castValue(value);
        if (type == null) return 0;
        return super.getInfoHeight(value);
    }

    @NotNull
    private static String getName(@NotNull BigInteger value) {
        String name = value.toString();
        if (name.length() >= 20) return name.substring(0, 20) + "..";
        return name;
    }
}
