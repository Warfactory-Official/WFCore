package wfcore.mixins.hbm;

import com.hbm.items.tool.ItemDetonator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wfcore.common.drones.EntitySuicideDrone;

import java.util.List;

@Mixin(value = ItemDetonator.class)
public class SuicideDroneCompatWithNTMDetonatorMixin {

    @Inject(method = "onItemRightClick(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/EnumHand;)Lnet/minecraft/util/ActionResult;",
            at = @At(value = "RETURN"))
    private void injectSuicideDroneLaunch(World world, EntityPlayer player, EnumHand handIn, CallbackInfoReturnable<ActionResult<ItemStack>> cir) {
        ItemStack stack = player.getHeldItem(handIn);
        if (stack.getTagCompound() != null) {
            int x = stack.getTagCompound().getInteger("x");
            int y = stack.getTagCompound().getInteger("y");
            int z = stack.getTagCompound().getInteger("z");
            BlockPos pos = new BlockPos(x, y, z);

            List<EntitySuicideDrone> suicideDrones = world.getEntitiesWithinAABB(
                    EntitySuicideDrone.class,
                    new AxisAlignedBB(pos).grow(1.0D)
            );

            if (!suicideDrones.isEmpty()) {
                for (EntitySuicideDrone drone : suicideDrones) {
                    drone.tryLaunch();
                }
            }
        }
    }
}
