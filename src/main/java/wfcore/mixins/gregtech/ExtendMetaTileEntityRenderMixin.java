package wfcore.mixins.gregtech;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.TickableTileEntityBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import wfcore.api.metatileentity.IAnimatedMTE;

@Mixin(MetaTileEntityHolder.class)
public abstract class ExtendMetaTileEntityRenderMixin extends TickableTileEntityBase {


    @Shadow MetaTileEntity metaTileEntity;

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared(){

        if(metaTileEntity instanceof IAnimatedMTE iAnimatedMTE){
            return iAnimatedMTE.getRenderDistanceSqared();
        }
        else
            return super.getMaxRenderDistanceSquared();
    }

}
