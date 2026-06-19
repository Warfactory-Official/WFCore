package wfcore.common.metatileentities.multi.electric;

import gregtech.api.capability.GregtechDataCodes;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IControllable;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import gregtech.api.metatileentity.multiblock.MultiblockDisplayText;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.pattern.PatternMatchContext;
import gregtech.api.pattern.TraceabilityPredicate;
import gregtech.api.util.RelativeDirection;
import gregtech.client.renderer.ICubeRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wfcore.api.capability.IACEnergyContainer;
import wfcore.client.render.WFTextures;
import wfcore.common.blocks.BlockMetalSheetCasing;
import wfcore.common.blocks.BlockRegistry;
import wfcore.common.fluid.CoolantRegistry;
import wfcore.common.metatileentities.WFCoreAbilities;
import wfcore.common.metatileentities.electric.MetaTileEntityACHatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Large Transformer: a 3x3x3 coolant-cooled power converter, like the Active Transformer but without laser
 * tech. It converts normal (DC) EU to/from WFCore "AC EU" through a single AC output (DC->AC) and a single
 * AC input (AC->DC) converter hatch. Every EU converted must be cooled by draining coolant - cooler coolants
 * (helium, liquid nitrogen) carry more EU per millibucket than water.
 */
public class MetaTileEntityLargeTransformer extends MultiblockWithDisplayBase implements IControllable {

    private static final long MAX_TRANSFER_PER_TICK = Integer.MAX_VALUE;
    private static final double EU_PER_MB_FACTOR = 2.0; // water (heatCapacity 1.0) => 2 EU per mB

    private IEnergyContainer powerInput;
    private IEnergyContainer powerOutput;
    private IMultipleTankHandler coolantInput;
    private MetaTileEntityACHatch acInputHatch;
    private MetaTileEntityACHatch acOutputHatch;

    private boolean isWorkingEnabled = true;
    private boolean isActive;

    public MetaTileEntityLargeTransformer(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
        this.powerInput = new EnergyContainerList(new ArrayList<>());
        this.powerOutput = new EnergyContainerList(new ArrayList<>());
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityLargeTransformer(metaTileEntityId);
    }

    @Override
    protected @NotNull BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK)
                .aisle("XXX", "XSX", "XXX")
                .aisle("XXX", "XXX", "XXX")
                .aisle("XXX", "XXX", "XXX")
                .where('S', selfPredicate())
                .where('X', casingPredicate())
                .build();
    }

    private TraceabilityPredicate casingPredicate() {
        return aluCasing().setMinGlobalLimited(8)
                .or(abilities(MultiblockAbility.INPUT_ENERGY).setMinGlobalLimited(1))
                .or(abilities(MultiblockAbility.OUTPUT_ENERGY).setMinGlobalLimited(1))
                .or(abilities(WFCoreAbilities.AC_INPUT).setMaxGlobalLimited(1))
                .or(abilities(WFCoreAbilities.AC_OUTPUT).setMaxGlobalLimited(1))
                .or(abilities(MultiblockAbility.IMPORT_FLUIDS).setMinGlobalLimited(1));
    }

    private TraceabilityPredicate aluCasing() {
        return states(BlockRegistry.SHEET_CASING.getState(
                BlockMetalSheetCasing.MetalSheetCasingType.ALUMINIUM_SHEET_CASING));
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return WFTextures.ALU_SHEET;
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.powerInput = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
        this.powerOutput = new EnergyContainerList(getAbilities(MultiblockAbility.OUTPUT_ENERGY));
        this.coolantInput = new FluidTankList(false, getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        this.acInputHatch = firstHatch(getAbilities(WFCoreAbilities.AC_INPUT));
        this.acOutputHatch = firstHatch(getAbilities(WFCoreAbilities.AC_OUTPUT));
    }

    @Nullable
    private static MetaTileEntityACHatch firstHatch(List<IACEnergyContainer> abilities) {
        for (IACEnergyContainer ability : abilities) {
            if (ability instanceof MetaTileEntityACHatch hatch) return hatch;
        }
        return null;
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.powerInput = new EnergyContainerList(new ArrayList<>());
        this.powerOutput = new EnergyContainerList(new ArrayList<>());
        this.coolantInput = null;
        this.acInputHatch = null;
        this.acOutputHatch = null;
        setActive(false);
    }

    @Override
    public boolean hasMaintenanceMechanics() {
        return false;
    }

    @Override
    protected void updateFormedValid() {
        if (getWorld().isRemote || !isWorkingEnabled) {
            setActive(false);
            return;
        }
        boolean worked = false;

        // DC -> AC: pull DC, cool it, push AC out through the cable
        if (acOutputHatch != null) {
            long want = Math.min(powerInput.getEnergyStored(), MAX_TRANSFER_PER_TICK);
            long cooled = coolEnergy(want);
            if (cooled > 0) {
                long pushed = acOutputHatch.pushAC(cooled);
                if (pushed > 0) {
                    powerInput.removeEnergy(pushed);
                    worked = true;
                }
            }
        }

        // AC -> DC: pull buffered AC, cool it, output DC
        if (acInputHatch != null) {
            long want = Math.min(acInputHatch.getStored(), MAX_TRANSFER_PER_TICK);
            long cooled = coolEnergy(want);
            if (cooled > 0) {
                acInputHatch.drainBuffer(cooled);
                powerOutput.changeEnergy(cooled);
                worked = true;
            }
        }

        setActive(worked);
    }

    /** Drains coolant to cover up to {@code desiredEU}; returns the EU successfully cooled. */
    private long coolEnergy(long desiredEU) {
        if (desiredEU <= 0 || coolantInput == null) return 0;
        long cooled = 0;
        for (IFluidTankProperties prop : coolantInput.getTankProperties()) {
            if (cooled >= desiredEU) break;
            FluidStack stack = prop.getContents();
            if (stack == null || stack.amount <= 0) continue;
            CoolantRegistry.CoolantSettings settings = CoolantRegistry.get(stack.getFluid());
            if (settings == null) continue;
            long euPerMb = Math.max(1, (long) (settings.heatCapacity() * EU_PER_MB_FACTOR));
            long remaining = desiredEU - cooled;
            int mbNeeded = (int) Math.min(stack.amount, (remaining + euPerMb - 1) / euPerMb);
            if (mbNeeded <= 0) continue;
            FluidStack drained = coolantInput.drain(new FluidStack(stack.getFluid(), mbNeeded), true);
            if (drained != null && drained.amount > 0) {
                cooled += (long) drained.amount * euPerMb;
            }
        }
        return Math.min(cooled, desiredEU);
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        MultiblockDisplayText.builder(textList, isStructureFormed())
                .setWorkingStatus(isWorkingEnabled, isActive)
                .addEnergyUsageLine(powerInput)
                .addCustom(tl -> {
                    if (isStructureFormed()) {
                        tl.add(new net.minecraft.util.text.TextComponentTranslation(
                                acOutputHatch != null ? "wfcore.gui.transformer.dc_to_ac" : "wfcore.gui.transformer.no_ac_out"));
                        tl.add(new net.minecraft.util.text.TextComponentTranslation(
                                acInputHatch != null ? "wfcore.gui.transformer.ac_to_dc" : "wfcore.gui.transformer.no_ac_in"));
                    }
                })
                .addWorkingStatusLine();
    }

    @Override
    public boolean isWorkingEnabled() {
        return isWorkingEnabled;
    }

    @Override
    public void setWorkingEnabled(boolean isWorkingAllowed) {
        this.isWorkingEnabled = isWorkingAllowed;
        markDirty();
        if (getWorld() != null && !getWorld().isRemote) {
            writeCustomData(GregtechDataCodes.WORKING_ENABLED, buf -> buf.writeBoolean(isWorkingEnabled));
        }
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

    @Override
    public boolean isActive() {
        return super.isActive() && isActive;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == GregtechTileCapabilities.CAPABILITY_CONTROLLABLE) {
            return GregtechTileCapabilities.CAPABILITY_CONTROLLABLE.cast(this);
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void writeInitialSyncData(@NotNull PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeBoolean(isActive);
    }

    @Override
    public void receiveInitialSyncData(@NotNull PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.isActive = buf.readBoolean();
    }

    @Override
    public void receiveCustomData(int dataId, @NotNull PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == GregtechDataCodes.WORKABLE_ACTIVE) {
            this.isActive = buf.readBoolean();
            scheduleRenderUpdate();
        } else if (dataId == GregtechDataCodes.WORKING_ENABLED) {
            this.isWorkingEnabled = buf.readBoolean();
            scheduleRenderUpdate();
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, @NotNull List<String> tooltip,
                               boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("wfcore.machine.large_transformer.tooltip1"));
        tooltip.add(I18n.format("wfcore.machine.large_transformer.tooltip2"));
        tooltip.add(I18n.format("wfcore.machine.large_transformer.tooltip3"));
    }
}
