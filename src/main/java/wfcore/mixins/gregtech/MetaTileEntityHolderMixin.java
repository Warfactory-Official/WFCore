package wfcore.mixins.gregtech;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wfcore.WFCore;
import wfcore.api.radar.MultiblockRadarLogic;
import wfcore.api.radar.RadarTargetIdentifier;
import wfcore.common.managers.RadarDataManager;

@Mixin(value = MetaTileEntityHolder.class, remap = false)
public abstract class MetaTileEntityHolderMixin {
    @Inject(method="setMetaTileEntity", at =@At("TAIL"))
    private void onSetMetaTileEntity(MetaTileEntity mte, CallbackInfoReturnable<MetaTileEntity> cir){
        MetaTileEntityHolder holder = (MetaTileEntityHolder) (Object) this;
        World world = holder.getWorld();
        if((world != null && !world.isRemote) && mte != null){
            if(MultiblockRadarLogic.isOnTEWhitelist(mte)){
                RadarDataManager.INSTANCE.addMachine(
                        world,
                        holder.getPos().getX(),
                        holder.getPos().getZ(),
                        MultiblockRadarLogic.getValue(mte)
                );
                if (WFCore.DEBUG)
                    WFCore.LOGGER.info("Added TileEntity {} to map at {} [Dim: {}]", mte.metaTileEntityId.toString(), holder.getPos().toString(), world.provider.getDimension());
            }

        }

    }



}
