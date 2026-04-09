package wfcore.common.drones;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import wfcore.common.items.BaseItem;

import javax.annotation.Nullable;
import java.util.List;

public class ItemSuicideDrone extends BaseItem {

    public ItemSuicideDrone(String s) {
        super(s, s);
        this.setMaxStackSize(1);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (facing != EnumFacing.UP) return EnumActionResult.PASS;

        double spawnX = pos.getX() + 0.5;
        double spawnY = pos.getY() + 1.0;
        double spawnZ = pos.getZ() + 0.5;

        if (!worldIn.isRemote) {
            EntitySuicideDrone drone = new EntitySuicideDrone(worldIn);
            drone.setPosition(spawnX, spawnY, spawnZ);
            drone.rotationYaw = player.rotationYaw;
            worldIn.spawnEntity(drone);
        }

        if (!player.capabilities.isCreativeMode) {
            player.getHeldItem(hand).shrink(1);
        }

        return EnumActionResult.SUCCESS;
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("This toy is amazing!");
    }
}
