package wfcore.common.metatileentities.multi.electric;

import gregtech.api.GTValues;
import gregtech.api.capability.*;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.resources.IGuiTexture;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.gui.widgets.ProgressWidget;
import gregtech.api.gui.widgets.SuppliedImageWidget;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.*;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.pattern.PatternMatchContext;
import gregtech.api.pattern.TraceabilityPredicate;
import gregtech.api.util.GTUtility;
import gregtech.api.util.TextFormattingUtil;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.common.ConfigHolder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import gregtech.api.util.TextComponentUtil;
import wfcore.client.render.WFTextures;
import wfcore.common.blocks.BlockMetalSheetCasing;
import wfcore.common.blocks.BlockRegistry;
import wfcore.common.items.registry.CPURegistry;
import wfcore.common.metatileentities.WFCoreAbilities;
import wfcore.common.metatileentities.compute.ICooler;
import wfcore.common.metatileentities.compute.ICpuSlot;
import wfcore.common.metatileentities.compute.IRamSlot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class MetaTileEntityMainframe extends MultiblockWithDisplayBase
        implements IOpticalComputationProvider, IControllable, IProgressBarMultiblock {

    private static final double MAX_TEMP = 105.0;

    @Getter
    private final GPCHandler gpcHandler;
    private final ProgressWidget.TimedProgressSupplier progressSupplier;
    private IEnergyContainer energyContainer;
    private IFluidHandler coolantIn;
    private IFluidHandler coolantOut;
    private boolean isActive;
    @Getter
    private boolean isWorkingEnabled = true;
    private boolean hasNotEnoughEnergy;
    @Getter
    private double AMBIENT = Double.NaN;
    private double currentTemp = Double.NaN;
    private long currentCWU;

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
    protected void addDisplayText(List<ITextComponent> textList) {
        MultiblockDisplayText.builder(textList, isStructureFormed())
                .setWorkingStatus(true, gpcHandler.getAllocatedCWUt() > 0) // transform into two-state system for
                // display
                .setWorkingStatusKeys(
                        "gregtech.multiblock.idling",
                        "gregtech.multiblock.idling",
                        "gregtech.multiblock.data_bank.providing")
                .addCustom(tl -> {
                    if (isStructureFormed()) {
                        // Energy Usage
                        ITextComponent voltageName = new TextComponentString(
                                GTValues.VNF[GTUtility.getTierByVoltage(gpcHandler.getMaxEUt())]);
                        tl.add(TextComponentUtil.translationWithColor(
                                TextFormatting.GRAY,
                                "gregtech.multiblock.hpca.energy",
                                TextFormattingUtil.formatNumbers(gpcHandler.cachedEUt),
                                TextFormattingUtil.formatNumbers(gpcHandler.getMaxEUt()),
                                voltageName));

                        // Provided Computation
                        ITextComponent cwutInfo = TextComponentUtil.stringWithColor(
                                TextFormatting.AQUA,
                                gpcHandler.cachedCWUt + " / " + gpcHandler.getMaxCWUt() + " CWU/t");
                        tl.add(TextComponentUtil.translationWithColor(
                                TextFormatting.GRAY,
                                "gregtech.multiblock.hpca.computation",
                                cwutInfo));
                    }
                })
                .addWorkingStatusLine();
    }

    @Override
    public void update() {
        super.update();
        if (isFirstTick()) {
            this.AMBIENT = setAmbient();
            if (Double.isNaN(this.currentTemp)) {
                this.currentTemp = this.AMBIENT;
            }
        }
    }

    private double setAmbient() {
        if (!Double.isNaN(AMBIENT)) return AMBIENT;
        if (getWorld() == null || getPos() == null) return 22.0;

        return switch (getWorld().provider.getDimension()) {
            case -1 -> 70.0;
            case 1 -> 5.0;
            default -> {
                Biome biome = getWorld().getBiome(getPos());
                float temp = biome.getTemperature(getPos());
                yield (temp * 30.0) - 5.0;
            }
        };
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
        this.coolantIn = new FluidTankList(false, getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        this.coolantOut = new FluidTankList(false, getAbilities(MultiblockAbility.EXPORT_FLUIDS));

        this.gpcHandler.onStructureForm();
    }

    private void consumeEnergy() {
        long energyToConsume = gpcHandler.getCurrentEUt();
        boolean hasMaintenance = ConfigHolder.machines.enableMaintenance && hasMaintenanceMechanics();
        if (hasMaintenance) {
            energyToConsume += getNumMaintenanceProblems() * energyToConsume / 10;
        }

        if (this.hasNotEnoughEnergy && energyContainer.getInputPerSec() > 19L * energyToConsume) {
            this.hasNotEnoughEnergy = false;
        }

        if (this.energyContainer.getEnergyStored() >= energyToConsume) {
            if (!hasNotEnoughEnergy) {
                long consumed = this.energyContainer.removeEnergy(energyToConsume);
                if (consumed == -energyToConsume) {
                    setActive(true);
                } else {
                    this.hasNotEnoughEnergy = true;
                    setActive(false);
                }
            }
        } else {
            this.hasNotEnoughEnergy = true;
            setActive(false);
        }
    }
    @Override
    public void setWorkingEnabled(boolean isWorkingAllowed) {
        if (this.isWorkingEnabled != isWorkingAllowed) {
            this.isWorkingEnabled = isWorkingAllowed;
            markDirty();
            if (getWorld() != null && !getWorld().isRemote) {
                writeCustomData(GregtechDataCodes.WORKING_ENABLED, buf -> buf.writeBoolean(isWorkingEnabled));
            }
        }
    }
    @Override
    protected void updateFormedValid() {
        if (!isWorkingEnabled()) {
            setActive(false);
            currentTemp = Math.max(AMBIENT, currentTemp - 0.25);
            this.currentCWU = 0;
            gpcHandler.clearAllocation();
            return;
        }

        // roll the previous tick's demand into the allocation, then bill energy for it
        gpcHandler.tick();
        consumeEnergy();

        if (isActive) {
            double temperatureChange = gpcHandler.calculateTemperatureChange(currentTemp >= 70.0);

            if (currentTemp + temperatureChange <= AMBIENT) {
                currentTemp = AMBIENT;
            } else {
                currentTemp += temperatureChange;
            }

            if (currentTemp >= MAX_TEMP) {
                this.explodeMultiblock(10);
            }

            this.currentCWU = gpcHandler.getAllocatedCWUt();
        } else {
            currentTemp = Math.max(AMBIENT, currentTemp - 0.25);
            this.currentCWU = 0;
            gpcHandler.clearAllocation();
        }
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.energyContainer = new EnergyContainerList(new ArrayList<>());
        this.gpcHandler.onStructureInvalidate();
    }

    @Override
    public int requestCWUt(int cwut, boolean simulate, @NotNull Collection<IOpticalComputationProvider> seen) {
        if (!isActive() || !isWorkingEnabled() || hasNotEnoughEnergy) return 0;
        return gpcHandler.requestComputation(cwut, simulate);
    }

    @Override
    public int getMaxCWUt(@NotNull Collection<IOpticalComputationProvider> seen) {
        if (!isActive() || !isWorkingEnabled() || hasNotEnoughEnergy) return 0;
        return (int) gpcHandler.getProvidableCWUt();
    }

    @Override
    public boolean canBridge(@NotNull Collection<IOpticalComputationProvider> seen) {
        return false;
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return WFTextures.ALU_SHEET;
    }

    @Override
    public void writeInitialSyncData(@NotNull PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeBoolean(this.isActive);
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
        } else if (dataId == GregtechDataCodes.CACHED_CWU) {
            gpcHandler.cachedCWUt = buf.readLong();
        }
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
                .or(abilities(WFCoreAbilities.GPC_RAM_SLOT).setMinGlobalLimited(1, 2))
                .or(abilities(MultiblockAbility.HPCA_COMPONENT));
    }

    @Override
    protected @NotNull BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("AA", "CC", "CC", "CC", "AA")
                .aisle("VA", "XV", "XV", "XV", "VA")
                .setRepeatable(2, 6)
                .aisle("SA", "CC", "CC", "CC", "AA")
                .where('S', selfPredicate())
                .where('A', aluCasing())
                .where('V', aluCasing())
                .where('X', mainframeComponents())
                .where('C', aluCasing().setMinGlobalLimited(5)
                        .or(maintenancePredicate())
                        .or(abilities(MultiblockAbility.INPUT_ENERGY).setMinGlobalLimited(1))
                        .or(abilities(MultiblockAbility.IMPORT_FLUIDS).setMaxGlobalLimited(1))
                        .or(abilities(MultiblockAbility.EXPORT_FLUIDS).setMaxGlobalLimited(1))
                        .or(abilities(MultiblockAbility.COMPUTATION_DATA_TRANSMISSION).setExactLimit(1)))
                .build();
    }

    @Override
    public int getNumProgressBars() {
        return 2;
    }

    @Override
    public double getFillPercentage(int index) {
        if (index == 0) {
            if (Double.isNaN(currentTemp)) return 0.0;
            return Math.max(0.0, Math.min(1.0, currentTemp / 100.0));
        } else if (index == 1) {
            if (gpcHandler.totalThroughput <= 0) return 0.0;
            return Math.max(0.0, Math.min(1.0, (double) currentCWU / gpcHandler.totalThroughput));
        }
        return 0.0;
    }

    @Override
    public TextureArea getProgressBarTexture(int index) {
        return switch (index) {
            case 0 -> GuiTextures.PROGRESS_BAR_BOILER_HEAT;
            case 1 -> GuiTextures.PROGRESS_BAR_HPCA_COMPUTATION;
            default -> IProgressBarMultiblock.super.getProgressBarTexture(index);
        };
    }

    @Override
    public void addBarHoverText(List<ITextComponent> hoverList, int index) {
        switch (index) {
            case 0 -> {
                hoverList.add(TextComponentUtil.stringWithColor(getHeatTextColor(),
                        "Heat: " + formatTemperature(currentTemp)));
                hoverList.add(TextComponentUtil.stringWithColor(TextFormatting.GRAY,
                        "Ambient: " + formatTemperature(AMBIENT)));
            }
            case 1 -> {
                hoverList.add(TextComponentUtil.stringWithColor(TextFormatting.AQUA,
                        String.format("Computation: %d / %d CWU/t", currentCWU, gpcHandler.totalThroughput)));
                hoverList.add(TextComponentUtil.stringWithColor(TextFormatting.GRAY,
                        String.format("Thermal sag: %.0f%%", gpcHandler.getCurrentSag() * 100.0)));
            }
        }
    }

    @Override
    protected ModularUI.Builder createUITemplate(EntityPlayer entityPlayer) {
        ModularUI.Builder builder = super.createUITemplate(entityPlayer);

        // Create the hover grid
        builder.widget(new ProgressWidget(
                () -> getGpcHandler().getAllocatedCWUt() > 0 ? progressSupplier.getAsDouble() : 0,
                74, 57, 47, 47, GuiTextures.HPCA_COMPONENT_OUTLINE, ProgressWidget.MoveType.HORIZONTAL)
                .setIgnoreColor(true)
                .setHoverTextConsumer(getGpcHandler()::addInfo));
//        int startX = 76;
//        int startY = 59;
//        for (int i = 0; i < 3; i++) {
//            for (int j = 0; j < 3; j++) {
//                final int index = i * 3 + j;
//                Supplier<IGuiTexture> textureSupplier = () -> getGpcHandler().getComponentTexture(index);
//                builder.widget(new SuppliedImageWidget(startX + (15 * j), startY + (15 * i), 13, 13, textureSupplier)
//                        .setIgnoreColor(true));
//            }
//        }
        return builder;
    }

    private TextFormatting getHeatTextColor() {
        if (Double.isNaN(currentTemp)) {
            return TextFormatting.GRAY;
        }
        if (currentTemp >= 105.0) {
            return TextFormatting.DARK_RED;
        }
        if (currentTemp >= 90.0) {
            return TextFormatting.RED;
        }
        if (currentTemp >= 70.0) {
            return TextFormatting.GOLD;
        }
        return TextFormatting.YELLOW;
    }

    private String formatTemperature(double temperature) {
        return Double.isNaN(temperature) ? "--" : String.format("%.1f C", temperature);
    }

    public static class GPCHandler {
        private final MetaTileEntityMainframe mainframe;
        long totalThroughput;
        double totalThermalMass;
        int cpuCount;
        private CPURegistry.CPUEntry[] activeCPUs;
        private ICooler[] passiveCoolers;
        private ICooler[] liquidCoolers;
        private long[] cpuLimits;
        @Getter
        private long allocatedCWUt;
        private long requestedCWUtThisTick;
        private long cachedCWUt;
        private long cachedEUt;
        @Getter
        private double currentSag;
        @Getter
        private int numBridges;

        private GPCHandler(MetaTileEntityMainframe mainframe) {
            this.mainframe = mainframe;
            reset();
        }

        // rolls the demand collected since the last tick into the active allocation
        public void tick() {
            this.currentSag = calculateSag();

            this.allocatedCWUt = Math.min(this.requestedCWUtThisTick, getProvidableCWUt());
            this.requestedCWUtThisTick = 0;

            if (cachedCWUt != allocatedCWUt) {
                this.cachedCWUt = allocatedCWUt;
                if (mainframe != null && !mainframe.getWorld().isRemote) {
                    mainframe.writeCustomData(GregtechDataCodes.CACHED_CWU, buf -> buf.writeLong(cachedCWUt));
                }
            }
            this.cachedEUt = getCurrentEUt();
        }

        // the most CWU/t this mainframe can hand out right now (thermal sag + memory throughput capped)
        public long getProvidableCWUt() {
            long thermal = (long) (getMaxCWUt() * (1.0 - this.currentSag));
            return Math.max(0L, Math.min(thermal, this.totalThroughput));
        }

        public int requestComputation(int cwut, boolean simulate) {
            if (cwut <= 0) return 0;
            long remaining = getProvidableCWUt() - this.requestedCWUtThisTick;
            if (remaining <= 0) return 0;
            int granted = (int) Math.min((long) cwut, remaining);
            if (!simulate) this.requestedCWUtThisTick += granted;
            return granted;
        }

        private double calculateSag() {
            if (this.mainframe.currentTemp <= 90.0) return 0.0;
            double penalty = Math.pow((this.mainframe.currentTemp - 90.0) / 10.0, 2) * 0.5;
            return Math.min(1.0, penalty);
        }

        private void onStructureInvalidate() {
            reset();
        }

        public void onStructureForm() {
            reset();
            rebuild();
        }

        private void reset() {
            clearAllocation();
            this.activeCPUs = new CPURegistry.CPUEntry[0];
            this.passiveCoolers = new ICooler[0];
            this.liquidCoolers = new ICooler[0];
            this.cpuLimits = new long[0];
            this.totalThroughput = 0;
            this.cpuCount = 0;
            this.numBridges = 0;
        }

        public void clearAllocation() {
            this.allocatedCWUt = 0;
            this.requestedCWUtThisTick = 0;
        }

        public long getCurrentEUt() {
            long maximumCWUt = Math.max(1, getMaxCWUt());
            long maximumEUt = getMaxEUt();
            long upkeepEUt = getUpkeepEUt();

            if (maximumEUt == upkeepEUt) {
                return maximumEUt;
            }
            return upkeepEUt + ((maximumEUt - upkeepEUt) * allocatedCWUt / maximumCWUt);
        }

        private long getUpkeepEUt() {
            long upkeepEUt = 0;
            for (CPURegistry.CPUEntry component : activeCPUs) {
                upkeepEUt += component.minPower();
            }
            return upkeepEUt;
        }

        public long getMaxEUt() {
            long maximumEUt = 0;
            for (int i = 0; i < activeCPUs.length; i++) {
                maximumEUt += cpuLimits[i];
            }
            return maximumEUt;
        }

        public int getMaxCoolingDemand() {
            int maxCooling = 0;
            for (int i = 0; i < activeCPUs.length; i++) {
                maxCooling += (int) activeCPUs[i].getHeat(this.cpuLimits[i]);
            }
            return maxCooling;
        }

        public int getMaxCoolingAmount() {
            double maxCooling = 0;
            for (ICooler cooler : passiveCoolers) {
                maxCooling += cooler.getPassiveCoolingRate(mainframe.currentTemp, totalThermalMass, mainframe.AMBIENT);
            }
            for (ICooler cooler : liquidCoolers) {
                maxCooling += cooler.getMaxActiveCoolingRate(totalThermalMass);
            }
            return (int) maxCooling;
        }

        public int getMaxCoolantDemand() {
            int maxCoolantDemand = 0;
            for (ICooler cooler : liquidCoolers) {
                maxCoolantDemand += cooler.getFluidUsagePerTick();
            }
            return maxCoolantDemand;
        }

        public long getMaxCWUt() {
            long maxCWUt = 0;
            for (int i = 0; i < activeCPUs.length; i++) {
                maxCWUt += activeCPUs[i].getCWU(this.cpuLimits[i]);
            }
            return maxCWUt;
        }

        public double calculateTemperatureChange(boolean forceCoolWithActive) {
            long maxCWUt = Math.max(1, getMaxCWUt());
            int maxCoolingDemand = getMaxCoolingDemand();
            double temperatureIncrease = (double) maxCoolingDemand * allocatedCWUt / maxCWUt;

            double passiveCoolingDone = 0;
            for (ICooler cooler : passiveCoolers) {
                passiveCoolingDone += cooler.getPassiveCoolingRate(mainframe.currentTemp, totalThermalMass, mainframe.AMBIENT);
            }

            double remainingHeat = temperatureIncrease - passiveCoolingDone;

            if (remainingHeat <= 0 && !forceCoolWithActive) {
                return remainingHeat;
            }

            double activePotential = 0;
            for (ICooler cooler : liquidCoolers) {
                activePotential += cooler.getMaxActiveCoolingRate(totalThermalMass);
            }

            if (activePotential > 0) {
                double coolingNeeded = forceCoolWithActive ? activePotential : Math.min(remainingHeat, activePotential);
                double percentageToExecute = coolingNeeded / activePotential;

                double actualActiveCooling = 0;
                for (ICooler cooler : liquidCoolers) {
                    actualActiveCooling += cooler.executeActiveCooling(percentageToExecute, totalThermalMass, mainframe.coolantIn, mainframe.coolantOut);
                }
                remainingHeat -= actualActiveCooling;
            }

            return remainingHeat;
        }

        public void rebuild() {
            this.totalThroughput = mainframe.getAbilities(WFCoreAbilities.GPC_RAM_SLOT)
                    .stream()
                    .mapToLong(IRamSlot::getTotalThroughput)
                    .sum();

            this.numBridges = Math.toIntExact(mainframe.getAbilities(MultiblockAbility.HPCA_COMPONENT)
                    .stream()
                    .filter(IHPCAComponentHatch::isBridge)
                    .count());

            List<ICooler> coolerList = mainframe.getAbilities(WFCoreAbilities.GPC_COOLER);

            List<ICooler> passive = new ArrayList<>();
            List<ICooler> active = new ArrayList<>();

            for (ICooler c : coolerList) {
                if (c.isLiquid()) {
                    active.add(c);
                } else {
                    passive.add(c);
                }
            }
            this.passiveCoolers = passive.toArray(new ICooler[0]);
            this.liquidCoolers = active.toArray(new ICooler[0]);

            List<ICpuSlot> slotList = mainframe.getAbilities(WFCoreAbilities.GPC_CPU_SLOT);
            List<CPURegistry.CPUEntry> hardware = new ArrayList<>();

            for (ICpuSlot slot : slotList) {
                CPURegistry.CPUEntry stats = slot.getStats();
                if (stats != null) {
                    hardware.add(stats);
                }
            }

            this.cpuCount = hardware.size();
            this.activeCPUs = hardware.toArray(new CPURegistry.CPUEntry[0]);

            this.cpuLimits = new long[this.cpuCount];
            for (int i = 0; i < this.cpuCount; i++) {
                this.cpuLimits[i] = this.activeCPUs[i].maxPower();
            }

            double baseFrameMass = 500.0;
            int totalPhysicalHatches = slotList.size() + coolerList.size() + mainframe.getAbilities(WFCoreAbilities.GPC_RAM_SLOT).size();
            double expansionMass = totalPhysicalHatches * 50.0;

            this.totalThermalMass = baseFrameMass + expansionMass;
        }

        public void addInfo(List<ITextComponent> textList) {
            // Max Computation
            ITextComponent data = TextComponentUtil.stringWithColor(TextFormatting.AQUA,
                    Long.toString(getMaxCWUt()));
            textList.add(TextComponentUtil.translationWithColor(TextFormatting.GRAY,
                    "gregtech.multiblock.hpca.info_max_computation", data));

            // Cooling
            TextFormatting coolingColor = getMaxCoolingAmount() < getMaxCoolingDemand() ? TextFormatting.RED :
                    TextFormatting.GREEN;
            data = TextComponentUtil.stringWithColor(coolingColor, Integer.toString(getMaxCoolingDemand()));
            textList.add(TextComponentUtil.translationWithColor(TextFormatting.GRAY,
                    "gregtech.multiblock.hpca.info_max_cooling_demand", data));

            data = TextComponentUtil.stringWithColor(coolingColor, Integer.toString(getMaxCoolingAmount()));
            textList.add(TextComponentUtil.translationWithColor(TextFormatting.GRAY,
                    "gregtech.multiblock.hpca.info_max_cooling_available", data));

            // Coolant Required
            if (getMaxCoolingDemand() > 0) {
                data = TextComponentUtil.stringWithColor(
                        TextFormatting.YELLOW,
                        getMaxCoolantDemand() + "L ");
                ITextComponent coolantName = TextComponentUtil.translationWithColor(TextFormatting.YELLOW,
                        "gregtech.multiblock.hpca.info_coolant_name");
                data.appendSibling(coolantName);
            } else {
                data = TextComponentUtil.stringWithColor(TextFormatting.GREEN, "0");
            }
            textList.add(TextComponentUtil.translationWithColor(TextFormatting.GRAY,
                    "gregtech.multiblock.hpca.info_max_coolant_required", data));

            // Bridging
            if (numBridges > 0) {
                textList.add(TextComponentUtil.translationWithColor(TextFormatting.GREEN,
                        "gregtech.multiblock.hpca.info_bridging_enabled"));
            } else {
                textList.add(TextComponentUtil.translationWithColor(TextFormatting.RED,
                        "gregtech.multiblock.hpca.info_bridging_disabled"));
            }
        }

        public void addWarnings(List<ITextComponent> textList) {
            List<ITextComponent> warnings = new ArrayList<>();
            if (numBridges > 1) {
                warnings.add(TextComponentUtil.translationWithColor(TextFormatting.GRAY,
                        "gregtech.multiblock.hpca.warning_multiple_bridges"));
            }
            if (cpuCount == 0) {
                warnings.add(TextComponentUtil.translationWithColor(TextFormatting.GRAY,
                        "gregtech.multiblock.hpca.warning_no_computation"));
            }
            if (getMaxCoolingDemand() > getMaxCoolingAmount()) {
                warnings.add(TextComponentUtil.translationWithColor(TextFormatting.GRAY,
                        "gregtech.multiblock.hpca.warning_low_cooling"));
            }
            if (!warnings.isEmpty()) {
                textList.add(TextComponentUtil.translationWithColor(TextFormatting.YELLOW,
                        "gregtech.multiblock.hpca.warning_structure_header"));
                textList.addAll(warnings);
            }
        }


    }
}
