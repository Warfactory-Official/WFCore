package wfcore.common.metatileentities.multi.electric;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.Fluids;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IOpticalComputationProvider;
import gregtech.api.gui.ModularUI;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.common.blocks.BlockComputerCasing;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import wfcore.common.blocks.BlockMetalSheetCasing;
import wfcore.common.blocks.BlockRegistry;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * This class contains the mte of the General Purpose Computer.
 * A GPComputer is a multiblock computer that provides computing units.
 * Must be cooled down with water, NTM Coolant or NTM Cold Perfluoromethyl.
 */
public class MetaTileEntityGPComputer extends MultiblockWithDisplayBase
    implements IOpticalComputationProvider {

    // Static
    public static final List<Fluid> ALLOWED_COOLANTS = Arrays.stream(new Fluid[]{
            Fluids.WATER.getFF(), Fluids.COOLANT.getFF(), Fluids.PERFLUOROMETHYL_COLD.getFF()
    }).toList();

    // Attributes
    public boolean isOn;
    public int totalCWU;
    public int availableCWU;
    private IEnergyContainer energyContainer;
    private Fluid coolantType = Fluids.WATER.getFF();
    private IFluidHandler coldCoolantHandler;
    private IFluidHandler hotCoolantHandler;


    public MetaTileEntityGPComputer(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    protected void updateFormedValid() { // TODO: update
        if (getWorld().isRemote) return;
        if (!isOn) return;
        if (coolantType == null || !ALLOWED_COOLANTS.contains(coolantType)) {
            isOn = false;
            markDirty();
            return;
        }
    }

    public boolean changeCoolant(Fluid newCoolant) {
        if (!ALLOWED_COOLANTS.contains(newCoolant)) return false;
        coolantType = newCoolant;
        return true;
    }

    @Override
    protected @NotNull BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("CSC", "FHF", "FHF", "FHF", "RET")
                .aisle("FHF", "C C", "C C", "C C", "FHF")
                .aisle("FHF", "C C", "C C", "C C", "FHF")
                .aisle("FHF", "C C", "C C", "C C", "FHF")
                .aisle("FHF", "C C", "C C", "C C", "FHF")
                .aisle("CCC", "FHF", "FHF", "FHF", "IMO")
                .where('S', selfPredicate())
                .where('C', states(BlockRegistry.SHEET_CASING.getState(BlockMetalSheetCasing.MetalSheetCasingType.ALUMINIUM_SHEET_CASING)))
                .where('F', blocks(ModBlocks.fan))
                .where('H', states(MetaBlocks.COMPUTER_CASING.getState(BlockComputerCasing.CasingType.COMPUTER_HEAT_VENT)))
                .where('R', metaTileEntities(MetaTileEntities.COMPUTATION_HATCH_RECEIVER))
                .where('E', metaTileEntities(MetaTileEntities.ENERGY_OUTPUT_HATCH))
                .where('T', metaTileEntities(MetaTileEntities.COMPUTATION_HATCH_TRANSMITTER))
                .where('I', metaTileEntities(MetaTileEntities.FLUID_IMPORT_HATCH))
                .where('M', metaTileEntities(MetaTileEntities.MAINTENANCE_HATCH))
                .where('O', metaTileEntities(MetaTileEntities.FLUID_EXPORT_HATCH))
                .where('-', air())
                .where(' ', any())
                .build();
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart iMultiblockPart) {
        return null;
    }
    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity iGregTechTileEntity) {
        return new MetaTileEntityGPComputer(metaTileEntityId);
    }
    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) { // Fuck off I hate this shit
        return null;
    }

    @Override
    public int requestCWUt(int cwut, boolean simulate, @NotNull Collection<IOpticalComputationProvider> seen) {
        int result = Math.min(cwut, availableCWU);
        if (!simulate) availableCWU -= result;
        return result;
    }

    @Override
    public int getMaxCWUt(@NotNull Collection<IOpticalComputationProvider> seen) {
        return totalCWU;
    }

    @Override
    public boolean canBridge(@NotNull Collection<IOpticalComputationProvider> seen) {
        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        isOn = data.getBoolean("isOn");
    }
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setBoolean("isOn", isOn);
        return data;
    }
}
