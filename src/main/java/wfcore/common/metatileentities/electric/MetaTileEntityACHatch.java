package wfcore.common.metatileentities.electric;

import gregtech.api.GTValues;
import gregtech.api.gui.ModularUI;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityMultiblockPart;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import org.jetbrains.annotations.Nullable;
import wfcore.api.capability.IACEnergyContainer;
import wfcore.common.capability.WFCapabilities;
import wfcore.common.metatileentities.WFCoreAbilities;

import java.util.List;

/**
 * The Large Transformer's AC converter hatch. An INPUT hatch buffers AC EU arriving from a cable (drained by
 * the transformer into DC); an OUTPUT hatch is a source the transformer pushes DC-converted AC into, which it
 * forwards to the connected cable. Exposes WFCore's AC capability on its front face.
 */
public class MetaTileEntityACHatch extends MetaTileEntityMultiblockPart
        implements IMultiblockAbilityPart<IACEnergyContainer>, IACEnergyContainer {

    private final boolean isOutput;
    private final long capacity;
    private long stored;

    public MetaTileEntityACHatch(ResourceLocation metaTileEntityId, int tier, boolean isOutput) {
        super(metaTileEntityId, tier);
        this.isOutput = isOutput;
        this.capacity = GTValues.V[tier] * 64L;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityACHatch(metaTileEntityId, getTier(), isOutput);
    }

    public boolean isOutput() {
        return isOutput;
    }

    public long getStored() {
        return stored;
    }

    /** INPUT side: the transformer pulls buffered AC out to convert to DC. */
    public long drainBuffer(long max) {
        long drained = Math.min(max, stored);
        stored -= drained;
        return drained;
    }

    /** OUTPUT side: the transformer pushes converted AC, which is forwarded onto the connected cable. */
    public long pushAC(long amount) {
        if (!isOutput || amount <= 0) return 0;
        TileEntity te = getNeighbor(getFrontFacing());
        if (te == null) return 0;
        IACEnergyContainer cable = te.getCapability(WFCapabilities.CAPABILITY_AC_ENERGY,
                getFrontFacing().getOpposite());
        if (cable == null) return 0;
        return cable.acceptEnergy(getFrontFacing().getOpposite(), amount);
    }

    // ------------------------------------------------------------------ IACEnergyContainer

    @Override
    public long acceptEnergy(EnumFacing side, long amount) {
        if (isOutput || amount <= 0) return 0;
        long accept = Math.min(amount, capacity - stored);
        if (accept <= 0) return 0;
        stored += accept;
        return accept;
    }

    @Override
    public long getThroughput() {
        return capacity;
    }

    @Override
    public boolean inputsAC(EnumFacing side) {
        return !isOutput && side == getFrontFacing();
    }

    @Override
    public boolean outputsAC(EnumFacing side) {
        return isOutput && side == getFrontFacing();
    }

    // ------------------------------------------------------------------ MTE plumbing

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == WFCapabilities.CAPABILITY_AC_ENERGY && (side == null || side == getFrontFacing())) {
            return WFCapabilities.CAPABILITY_AC_ENERGY.cast(this);
        }
        return super.getCapability(capability, side);
    }

    @Override
    public MultiblockAbility<IACEnergyContainer> getAbility() {
        return isOutput ? WFCoreAbilities.AC_OUTPUT : WFCoreAbilities.AC_INPUT;
    }

    @Override
    public void registerAbilities(List<IACEnergyContainer> abilityList) {
        abilityList.add(this);
    }

    @Override
    protected boolean openGUIOnRightClick() {
        return false;
    }

    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        return null;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format(isOutput ? "wfcore.machine.ac_hatch.output" : "wfcore.machine.ac_hatch.input"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setLong("ACStored", stored);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.stored = data.getLong("ACStored");
    }
}
