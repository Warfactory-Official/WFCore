package wfcore.integration.warforge;

import com.flansmod.warforge.api.WarforgeAPI;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.util.AssemblyLineManager;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import wfcore.api.capability.data.IDataStorage;
import wfcore.common.metatileentities.research.MetaTileEntityResearchUnit;

import java.math.BigInteger;
import java.util.UUID;

/**
 * Bridges WarForge factions with WFCore's research/data systems: a machine gains access to the entire
 * blueprint library when the faction that owns its chunk keeps some loaded data storage with data inside it
 * (a research unit with stored research, or any inventory holding research data sticks / non-empty data
 * storage items such as a Data Bank's sticks or pen drives) anywhere in its claimed, loaded territory.
 */
public final class FactionLibraryAccess {

    private FactionLibraryAccess() {}

    /** Whether the faction owning the chunk at {@code pos} has qualifying data storage in its loaded claims. */
    public static boolean hasLibraryAccess(World world, BlockPos pos) {
        if (world == null || world.isRemote || pos == null) return false;
        UUID faction = WarForgeMod.FACTIONS.getClaim(new DimChunkPos(world.provider.getDimension(), pos));
        if (faction == null || Faction.nullUuid.equals(faction)) return false;
        return WarforgeAPI.anyLoadedClaimedTile(faction, FactionLibraryAccess::tileHoldsData);
    }

    private static boolean tileHoldsData(TileEntity te) {
        // A research unit that has stored/completed research counts as data storage with data.
        if (te instanceof IGregTechTileEntity) {
            MetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
            if (mte instanceof MetaTileEntityResearchUnit
                    && ((MetaTileEntityResearchUnit) mte).getResearchState().hasAnyData()) {
                return true;
            }
        }
        // Any reachable inventory holding a research data stick or a non-empty data-storage item.
        if (scanHandler(te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null))) return true;
        for (EnumFacing side : EnumFacing.VALUES) {
            if (scanHandler(te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side))) return true;
        }
        return false;
    }

    private static boolean scanHandler(IItemHandler inv) {
        if (inv == null) return false;
        for (int slot = 0; slot < inv.getSlots(); slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            if (AssemblyLineManager.readResearchId(stack) != null) return true;
            if (stack.getItem() instanceof IDataStorage) {
                IDataStorage storage = (IDataStorage) stack.getItem();
                if (storage.numBitsTaken(stack).compareTo(BigInteger.ZERO) > 0) return true;
            }
        }
        return false;
    }
}
