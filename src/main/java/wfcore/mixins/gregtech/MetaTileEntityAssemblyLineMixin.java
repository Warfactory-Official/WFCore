package wfcore.mixins.gregtech;

import gregtech.api.recipes.Recipe;
import gregtech.common.metatileentities.multi.electric.MetaTileEntityAssemblyLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wfcore.integration.warforge.FactionLibraryAccess;

/**
 * Grants an Assembly Line full research/blueprint access (as if every data stick were present) when the
 * faction owning its chunk keeps loaded data storage with data inside its claimed territory. Injects right
 * before the vanilla data-access-hatch research check, so all of the Assembly Line's other recipe checks
 * (ordered items/fluids) still run normally; only the research gate is satisfied.
 */
@Mixin(value = MetaTileEntityAssemblyLine.class, remap = false)
public abstract class MetaTileEntityAssemblyLineMixin {

    @Unique
    private long wfcore$libCacheTick = Long.MIN_VALUE;

    @Unique
    private boolean wfcore$libCacheValue = false;

    @Inject(
            method = "checkRecipe",
            at = @At(
                    value = "INVOKE",
                    target = "Lgregtech/common/metatileentities/multi/electric/MetaTileEntityAssemblyLine;isRecipeAvailable(Ljava/lang/Iterable;Lgregtech/api/recipes/Recipe;)Z",
                    ordinal = 0),
            cancellable = true,
            remap = false)
    private void wfcore$grantFactionLibrary(Recipe recipe, boolean consumeIfSuccess,
                                            CallbackInfoReturnable<Boolean> cir) {
        MetaTileEntityAssemblyLine self = (MetaTileEntityAssemblyLine) (Object) this;
        if (self.getWorld() == null || self.getWorld().isRemote) return;

        long now = self.getWorld().getTotalWorldTime();
        if (wfcore$libCacheTick == Long.MIN_VALUE || now - wfcore$libCacheTick >= 60L) {
            wfcore$libCacheTick = now;
            wfcore$libCacheValue = FactionLibraryAccess.hasLibraryAccess(self.getWorld(), self.getPos());
        }
        if (wfcore$libCacheValue) {
            cir.setReturnValue(true);
        }
    }
}
