package wfcore.common.recipe.logic;

import gregtech.api.capability.impl.MultiblockRecipeLogic;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import wfcore.common.metatileentities.multi.electric.MetaTileEntityComputer;

// TODO: Consider overclocking/ parallelization (?)
public class ComputerRecipeLogic extends MultiblockRecipeLogic {
    public ComputerRecipeLogic(MetaTileEntityComputer tileEntity) {
        super(tileEntity);
    }

    // used to add compute
    @Override
    protected void completeRecipe() {
        super.completeRecipe();
    }
}
