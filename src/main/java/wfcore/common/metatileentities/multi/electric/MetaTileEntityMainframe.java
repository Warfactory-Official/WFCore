package wfcore.common.metatileentities.multi.electric;

import gregtech.api.capability.*;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.gui.widgets.ProgressWidget;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.pattern.TraceabilityPredicate;
import gregtech.client.renderer.ICubeRenderer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import wfcore.client.render.WFTextures;
import wfcore.common.blocks.BlockMetalSheetCasing;
import wfcore.common.blocks.BlockRegistry;
import wfcore.common.items.registry.CPURegistry;
import wfcore.common.metatileentities.WFCoreAbilities;
import wfcore.common.metatileentities.compute.ICooler;
import wfcore.common.metatileentities.compute.ICpuSlot;
import wfcore.common.metatileentities.compute.IRamSlot;
import wfcore.common.metatileentities.compute.MetaTileEntityCPUSlot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class contains the mte of the General Purpose Computer.
 * A GPComputer is a multiblock computer that provides computing units.
 * Must be cooled down with water, NTM Coolant or NTM Cold Perfluoromethyl.
 */
public class MetaTileEntityMainframe extends MultiblockWithDisplayBase
    implements IOpticalComputationProvider, IControllable {


    private IEnergyContainer energyContainer;
    private IFluidHandler coolantHandler;
    @Getter
    private final GPCHandler gpcHandler;

    private boolean isActive;
    @Getter @Setter
    private boolean isWorkingEnabled = true;
    private boolean hasNotEnoughEnergy;


    private final ProgressWidget.TimedProgressSupplier progressSupplier;

    public MetaTileEntityMainframe(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
        this.energyContainer = new EnergyContainerList(new ArrayList<>());
        this.progressSupplier = new ProgressWidget.TimedProgressSupplier(200, 47, false);
        this.gpcHandler = new GPCHandler(this);
    }
    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == GregtechTileCapabilities.CAPABILITY_CONTROLLABLE) {
            return GregtechTileCapabilities.CAPABILITY_CONTROLLABLE.cast(this);
        }
        return super.getCapability(capability, side);
    }


    @Override
    public int requestCWUt(int cwut, boolean simulate, @NotNull Collection<IOpticalComputationProvider> seen) {
        return 0;
    }

    @Override
    public int getMaxCWUt(@NotNull Collection<IOpticalComputationProvider> seen) {
        return 0;
    }

    @Override
    public boolean canBridge(@NotNull Collection<IOpticalComputationProvider> seen) {
        return false;
    }

    @Override
    protected void updateFormedValid() {

    }


    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return WFTextures.ALU_SHEET;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityMainframe(metaTileEntityId);
    }
    @Override
    public boolean isActive() {
        return super.isActive() && this.isActive;
    }

    public void setActive(boolean active) {
        if (this.isActive != active) {
            this.isActive = active;
            markDirty();
            if (getWorld() != null && !getWorld().isRemote) {
                writeCustomData(GregtechDataCodes.WORKABLE_ACTIVE, buf -> buf.writeBoolean(active));
            }
        }
    }

    public TraceabilityPredicate aluCasing() {
        return states(BlockRegistry.SHEET_CASING.getState(BlockMetalSheetCasing.MetalSheetCasingType.ALUMINIUM_SHEET_CASING));
    }
    protected TraceabilityPredicate mainframeComponents() {
        return abilities(WFCoreAbilities.GPC_CPU_SLOT).setMinGlobalLimited(1)
                .or(abilities(WFCoreAbilities.GPC_COOLER).setPreviewCount(6))
                .or(abilities(WFCoreAbilities.GPC_RAM_SLOT).setMinGlobalLimited(1,2));
    }

    @Override
    protected @NotNull BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("AA", "CC", "CC", "CC", "AA")
                .aisle("VA", "XV", "XV", "XV", "VA")
                .setRepeatable(2,6)
                .aisle("SA", "CC", "CC", "CC", "AA")
                .where('S', selfPredicate())
                .where('A', aluCasing())
                .where('V', aluCasing())
                .where('X', mainframeComponents())
                .where('C', aluCasing().setMinGlobalLimited(5)
                        .or(maintenancePredicate())
                        .or(abilities(MultiblockAbility.INPUT_ENERGY).setMinGlobalLimited(1))
                        .or(abilities(MultiblockAbility.IMPORT_FLUIDS).setMaxGlobalLimited(1))
                        .or(abilities(MultiblockAbility.COMPUTATION_DATA_TRANSMISSION).setExactLimit(1)))
                .build();
    }

    public class GPCHandler {
        private final MetaTileEntityMainframe mainframe;
        private CPURegistry.CPUEntry[] activeCPUs;
        private ICooler[] coolers;

        private MetaTileEntityCPUSlot[] activeSlotInstances;

        // Cached constants
        long totalThroughput;
        double totalThermalMass;
        int cpuCount;

        private GPCHandler(MetaTileEntityMainframe mainframe) {
            this.mainframe = mainframe;
        }

        public void rebuild() {
            this.totalThroughput = mainframe.getAbilities(WFCoreAbilities.GPC_RAM_SLOT)
                    .stream()
                    .mapToLong(IRamSlot::getTotalThroughput)
                    .sum();

            List<ICooler> coolerList = mainframe.getAbilities(WFCoreAbilities.GPC_COOLER);
            this.coolers = coolerList.toArray(new ICooler[0]);

            List<ICpuSlot> slotList = mainframe.getAbilities(WFCoreAbilities.GPC_CPU_SLOT);
            List<CPURegistry.CPUEntry> hardware = new ArrayList<>();

            for (ICpuSlot slot : slotList) {
                CPURegistry.CPUEntry stats = slot.getStats();
                if (stats != null) {
                    hardware.add(stats);
                }
            }
            this.activeCPUs = hardware.toArray(new CPURegistry.CPUEntry[0]);
            this.cpuCount = hardware.size();

            //this.totalThermalMass = mainframe.getStructurePattern().getChassisCount() * 10.0;
        }
    }
}
