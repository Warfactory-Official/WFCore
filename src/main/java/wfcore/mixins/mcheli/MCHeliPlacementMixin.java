package wfcore.mixins.mcheli;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_ItemAircraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blocks placing MCHeli vehicles by hand. Only small UAVs stay hand-placeable; every other vehicle
 * (tanks, planes, helis, large UAVs) must be produced by a vehicle factory. The factory spawns its
 * vehicles via {@code World#spawnEntity} directly, so it bypasses this path.
 */
@Mixin(value = MCH_ItemAircraft.class, remap = false)
public abstract class MCHeliPlacementMixin {

    @Shadow
    public abstract MCH_AircraftInfo getAircraftInfo();

    @Inject(method = "spawnAircraft", at = @At("HEAD"), cancellable = true, remap = false)
    private void wfcore$blockNonSmallUavPlacement(ItemStack itemStack, World world, EntityPlayer player,
                                                  BlockPos blockpos, CallbackInfoReturnable<MCH_EntityAircraft> cir) {
        MCH_AircraftInfo info = getAircraftInfo();
        if (info == null || !info.isUAV || !info.isSmallUAV) {
            cir.setReturnValue(null);
        }
    }
}
