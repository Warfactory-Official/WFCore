package wfcore.common.te;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import wfcore.api.block.IAnimatedTEProvider;

public class AnimatablePartTileEntity extends TileEntity {

    public IAnimatedTEProvider getPartBlock() {
        if (getBlockType() instanceof IAnimatedTEProvider) {
            return (IAnimatedTEProvider) getBlockType();
        }
        throw new IllegalStateException("Block should implement IAnimatedTEProvider!");
    }

    @SideOnly(Side.CLIENT)
    @Override
    public @NotNull AxisAlignedBB getRenderBoundingBox() {
        return getPartBlock().getRenderBoundingBox(getWorld(), getPos(), getBlockMetadata());
    }

}
