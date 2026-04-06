package wfcore.mixins.minecraft;

import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wfcore.client.render.AnimatedRenderQueue;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Inject(
            method = "renderWorldPass",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderGlobal;renderEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V",
                    shift = At.Shift.AFTER
            )
    )
    private void wfcore$afterEntities(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        // Fire the queue. We don't care if the user is wearing 3D glasses or not!
        AnimatedRenderQueue.getInstance().flush(partialTicks);
    }

}