package wfcore.common.metatileentities.multi.electric;

import gregtech.api.items.itemhandlers.GTItemStackHandler;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.recipes.RecipeMap;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.items.IItemHandlerModifiable;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract crafting multiblock that runs a standard GT recipe map and, on completion, spawns a
 * vehicle at a designated spot instead of ejecting an output item.
 *
 * <p>The recipe's single item output (the vehicle item) is routed into {@link #vehicleOutput} by
 * overriding {@link #getOutputInventory()}, so it never reaches a player-accessible bus. A throttled
 * server tick resolves that item and, when {@link #getSpawnPos()} is clear, spawns the vehicle via
 * the subclass {@link #deploy}. If the area is obstructed the finished vehicle is held and retried.
 */
public abstract class MetaTileEntityVehicleFactory extends RecipeMapMultiblockController {

    protected final GTItemStackHandler vehicleOutput = new GTItemStackHandler(this, 1);

    public MetaTileEntityVehicleFactory(ResourceLocation metaTileEntityId, RecipeMap<?> recipeMap) {
        super(metaTileEntityId, recipeMap);
    }

    @Override
    public IItemHandlerModifiable getOutputInventory() {
        return vehicleOutput;
    }

    @Override
    protected void updateFormedValid() {
        super.updateFormedValid();
        if (getWorld().isRemote || getOffsetTimer() % 10 != 0) {
            return;
        }
        ItemStack out = vehicleOutput.getStackInSlot(0);
        if (out.isEmpty()) {
            return;
        }
        if (getWorld() instanceof WorldServer serverWorld && deploy(serverWorld, getSpawnPos(), out)) {
            vehicleOutput.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    /** The world position the finished vehicle is deployed to. Default: 4 blocks in front, 1 up. */
    public BlockPos getSpawnPos() {
        return getPos().offset(getFrontFacing(), 4).up();
    }

    @Override
    public boolean hasMaintenanceMechanics() {
        return false;
    }

    /** Spawn the vehicle encoded by {@code vehicleItem} at {@code pos}; return true if spawned. */
    protected abstract boolean deploy(@NotNull WorldServer world, @NotNull BlockPos pos, @NotNull ItemStack vehicleItem);
}
