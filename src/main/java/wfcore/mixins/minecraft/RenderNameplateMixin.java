package wfcore.mixins.minecraft;

import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Occlude nameplates behind terrain by default, and only let them show through walls when the
 * {@code isSneaking} flag is set ({@link RenderLivingLabelMixin} repurposes that flag to mean
 * "the player is looking at this entity").
 * <p>
 * Vanilla does the opposite: names ghost through walls unless the entity is sneaking. The whole
 * difference is the {@code isSneaking} branches being inverted plus the front pass kept opaque, so
 * two narrow injectors replace what used to be a full {@code @Overwrite} and now compose with other
 * mods touching {@code drawNameplate}.
 */
@Mixin(EntityRenderer.class)
public class RenderNameplateMixin {

    @ModifyVariable(method = "drawNameplate", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private static boolean wfcore$invertVisibility(boolean isSneaking) {
        return !isSneaking;
    }

    @ModifyConstant(method = "drawNameplate", constant = @Constant(intValue = 553648127, ordinal = 1))
    private static int wfcore$opaqueFrontPass(int color) {
        return -1;
    }
}
