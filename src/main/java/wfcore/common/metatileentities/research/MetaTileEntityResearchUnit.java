package wfcore.common.metatileentities.research;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import gregtech.api.capability.IDataAccessHatch;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IOpticalComputationProvider;
import gregtech.api.capability.IOpticalComputationReceiver;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.pattern.PatternMatchContext;
import gregtech.api.pattern.TraceabilityPredicate;
import gregtech.api.util.AssemblyLineManager;
import gregtech.api.util.RelativeDirection;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.common.items.MetaItems;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wfcore.api.research.Research;
import wfcore.api.research.ResearchRegistry;
import wfcore.api.research.ResearchState;
import wfcore.client.render.WFTextures;
import wfcore.common.blocks.BlockMetalSheetCasing;
import wfcore.common.blocks.BlockRegistry;
import wfcore.common.gui.IWFGuiHolder;
import wfcore.common.gui.WFGuiFactory;
import wfcore.common.gui.research.ResearchTreeGui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 3x3x3 research multiblock. In CONTROL mode it presents the research tree and runs Factorio-style research:
 * each run consumes items (from an item input hatch) + CWU (from a computation reception hatch wired to a
 * Mainframe) at a constant power draw, advancing completion one run at a time. Completed researches are
 * written as research-id data sticks into a Data Access Hatch (Data-Bank-compatible storage), and existing
 * research data sticks are recognised on import. SLAVE units placed next to a CONTROL unit add parallel
 * research slots.
 *
 * <p>Structure: controller + aluminium sheet casing, with one Computation Reception Hatch, at least one Item
 * Input Bus and one Energy Input, and optional Data Access Hatch(es).
 */
public class MetaTileEntityResearchUnit extends MultiblockWithDisplayBase
        implements IOpticalComputationReceiver, IWFGuiHolder {

    public enum Mode { CONTROL, SLAVE }

    private static final int SYNC_MODE = 10100;
    private static final int SYNC_STATE = 10101;
    private static final int SYNC_JOBS = 10102;

    private static final int CLUSTER_SCAN_RADIUS = 4;
    private static final int MAX_SLAVES = 16;
    private static final int QUEUE_SIZE = 3;

    private Mode mode = Mode.CONTROL;
    private final ResearchState state = new ResearchState();
    private final List<Job> jobs = new ArrayList<>();
    private int slaveCount;
    private long tickCounter;

    private IEnergyContainer energyContainer;
    private IItemHandlerModifiable inputInventory;
    private IOpticalComputationProvider computationProvider;
    private List<IDataAccessHatch> dataHatches = new ArrayList<>();

    // server-side: which research the side-panel buttons act on (last clicked node). Not saved/synced.
    private String selectedResearchId;

    // client-only display caches (filled from sync)
    private final Map<String, Float> clientJobProgress = new HashMap<>();
    private final List<String> clientQueueOrder = new ArrayList<>();

    public MetaTileEntityResearchUnit(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
        this.energyContainer = new EnergyContainerList(new ArrayList<>());
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityResearchUnit(metaTileEntityId);
    }

    // ------------------------------------------------------------------ structure

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
                .or(abilities(MultiblockAbility.IMPORT_ITEMS).setMinGlobalLimited(1))
                .or(abilities(MultiblockAbility.INPUT_ENERGY).setMinGlobalLimited(1))
                .or(abilities(MultiblockAbility.COMPUTATION_DATA_RECEPTION).setExactLimit(1))
                .or(abilities(MultiblockAbility.DATA_ACCESS_HATCH).setMaxGlobalLimited(2));
    }

    private TraceabilityPredicate aluCasing() {
        return states(BlockRegistry.SHEET_CASING.getState(
                BlockMetalSheetCasing.MetalSheetCasingType.ALUMINIUM_SHEET_CASING));
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return WFTextures.ALU_SHEET;
    }

    @NotNull
    @Override
    protected ICubeRenderer getFrontOverlay() {
        return WFTextures.OVERLAY_COMPUTER;
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
        this.inputInventory = new ItemHandlerList(getAbilities(MultiblockAbility.IMPORT_ITEMS));
        List<?> receivers = getAbilities(MultiblockAbility.COMPUTATION_DATA_RECEPTION);
        this.computationProvider = receivers.isEmpty() ? null
                : (IOpticalComputationProvider) receivers.get(0);
        this.dataHatches = new ArrayList<>(getAbilities(MultiblockAbility.DATA_ACCESS_HATCH));
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.energyContainer = new EnergyContainerList(new ArrayList<>());
        this.inputInventory = null;
        this.computationProvider = null;
        this.dataHatches = new ArrayList<>();
        this.jobs.clear();
    }

    @Override
    public boolean hasMaintenanceMechanics() {
        return false;
    }

    // ------------------------------------------------------------------ compute provider

    @Override
    @Nullable
    public IOpticalComputationProvider getComputationProvider() {
        return computationProvider;
    }

    // ------------------------------------------------------------------ accessors used by the GUI

    public Mode getMode() { return mode; }
    public ResearchState getResearchState() { return state; }
    public int getJobCapacity() { return 1 + slaveCount; }
    public float getClientProgress(String researchId) { return clientJobProgress.getOrDefault(researchId, 0f); }
    public List<String> getClientQueue() { return clientQueueOrder; }

    /** Server-side: is this research anywhere in the queue. */
    public boolean isResearching(String researchId) {
        for (Job job : jobs) if (job.researchId.equals(researchId)) return true;
        return false;
    }

    /** Client-side (GUI): is this research queued. */
    public boolean isQueuedClient(String researchId) {
        return clientQueueOrder.contains(researchId);
    }

    /** Client-side (GUI): is this research currently running (within the cluster's capacity). */
    public boolean isActiveClient(String researchId) {
        int idx = clientQueueOrder.indexOf(researchId);
        return idx >= 0 && idx < getJobCapacity();
    }

    // ------------------------------------------------------------------ ticking

    @Override
    protected void updateFormedValid() {
        if (getWorld().isRemote) return;

        if (++tickCounter % 40 == 0) {
            recomputeSlaveCount();
            importCompletedFromHatches();
        }

        if (mode != Mode.CONTROL || jobs.isEmpty()) return;

        // only the first `capacity` queued researches run concurrently; the rest wait their turn
        int activeCount = Math.min(jobs.size(), getJobCapacity());
        boolean changed = false;
        for (int i = activeCount - 1; i >= 0; i--) {
            Job job = jobs.get(i);
            Research research = ResearchRegistry.get(job.researchId);
            if (research == null) {
                jobs.remove(i);
                changed = true;
                continue;
            }
            int runResult = processJob(job, research);
            if (runResult != RUN_IDLE) changed = true;
            if (state.isComplete(job.researchId)) {
                completeResearch(research);
                jobs.remove(i);
            }
        }

        if (changed) syncState();
        if (!jobs.isEmpty() && tickCounter % 10 == 0) syncJobs();
    }

    private static final int RUN_IDLE = 0;
    private static final int RUN_PROGRESS = 1;
    private static final int RUN_FINISHED = 2;

    /** Advances a job one tick. Returns RUN_IDLE/RUN_PROGRESS/RUN_FINISHED. */
    private int processJob(Job job, Research research) {
        boolean needsCompute = research.getCwuPerRun() > 0;
        if (needsCompute && (computationProvider == null
                || computationProvider.requestCWUt((int) Math.min(research.getCwuPerRun(), Integer.MAX_VALUE), true) <= 0)) {
            return RUN_IDLE; // no computation - stall without consuming items/energy
        }

        if (!job.materialsConsumed) {
            if (!consumeMaterials(research.getItemsPerRun())) return RUN_IDLE; // waiting for materials
            job.materialsConsumed = true;
        }

        if (!drawEnergy(research.getEut(), false)) return RUN_IDLE; // not enough power

        if (needsCompute) {
            long remaining = research.getCwuPerRun() - job.accumulatedCWU;
            long perTick = Math.max(1, (research.getCwuPerRun() + research.getTicksPerRun() - 1) / research.getTicksPerRun());
            int request = (int) Math.min(Math.min(perTick, remaining), Integer.MAX_VALUE);
            job.accumulatedCWU += computationProvider.requestCWUt(request, false);
        }
        job.elapsedTicks++;

        boolean enoughCompute = job.accumulatedCWU >= research.getCwuPerRun();
        boolean enoughTime = job.elapsedTicks >= research.getTicksPerRun();
        if (enoughCompute && enoughTime) {
            int completed = state.getCompletedRuns(research.getId()) + 1;
            state.setCompletedRuns(research.getId(), completed);
            state.setPartialCWU(research.getId(), 0);
            job.accumulatedCWU = 0;
            job.elapsedTicks = 0;
            job.materialsConsumed = false;
            return RUN_FINISHED;
        }
        return RUN_PROGRESS;
    }

    private void completeResearch(Research research) {
        state.setCompletedRuns(research.getId(), research.getRunsRequired());
        state.setPartialCWU(research.getId(), 0);
        if (research.hasBlueprint()) {
            writeResearchDataStick(research.getId());
        }
    }

    // ------------------------------------------------------------------ player actions (server side)

    /** Selects a research for the side panel (set by clicking a node). */
    public void setSelected(String researchId) {
        this.selectedResearchId = researchId;
    }

    /** Side-panel button: enqueue the selected research if not queued, otherwise dequeue it. */
    public boolean toggleSelected() {
        if (selectedResearchId == null) return false;
        return isResearching(selectedResearchId) ? dequeue(selectedResearchId) : enqueue(selectedResearchId);
    }

    /** Adds a research to the queue (max {@value QUEUE_SIZE}). Resumes any saved partial run. */
    public boolean enqueue(String researchId) {
        if (mode != Mode.CONTROL || !isStructureFormed() || researchId == null) return false;
        Research research = ResearchRegistry.get(researchId);
        if (research == null || state.isComplete(researchId) || !state.isUnlocked(researchId)) return false;
        if (isResearching(researchId) || jobs.size() >= QUEUE_SIZE) return false;
        Job job = new Job(researchId);
        job.accumulatedCWU = state.getPartialCWU(researchId);
        jobs.add(job);
        syncState();
        syncJobs();
        return true;
    }

    /** Removes a research from the queue, banking its mid-run progress for later resume. */
    public boolean dequeue(String researchId) {
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            if (job.researchId.equals(researchId)) {
                state.setPartialCWU(researchId, job.accumulatedCWU);
                jobs.remove(i);
                syncState();
                syncJobs();
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ data bank integration

    /** Writes a research-id data stick into a Data Access Hatch so it joins the data network / can be used. */
    private void writeResearchDataStick(String researchId) {
        if (alreadyStored(researchId)) return;
        ItemStack stick = MetaItems.TOOL_DATA_STICK.getStackForm();
        NBTTagCompound nbt = stick.hasTagCompound() ? stick.getTagCompound() : new NBTTagCompound();
        AssemblyLineManager.writeResearchToNBT(nbt, researchId);
        stick.setTagCompound(nbt);
        for (IItemHandlerModifiable inv : dataInventories()) {
            for (int slot = 0; slot < inv.getSlots(); slot++) {
                if (inv.getStackInSlot(slot).isEmpty() && inv.insertItem(slot, stick, false).isEmpty()) {
                    return;
                }
            }
        }
    }

    /** Marks any research whose data stick is present in a connected hatch as complete (external import). */
    private void importCompletedFromHatches() {
        if (mode != Mode.CONTROL) return;
        boolean changed = false;
        for (IItemHandlerModifiable inv : dataInventories()) {
            for (int slot = 0; slot < inv.getSlots(); slot++) {
                String id = AssemblyLineManager.readResearchId(inv.getStackInSlot(slot));
                if (id == null) continue;
                Research research = ResearchRegistry.get(id);
                if (research != null && !state.isComplete(id)) {
                    state.setCompletedRuns(id, research.getRunsRequired());
                    changed = true;
                }
            }
        }
        if (changed) syncState();
    }

    private boolean alreadyStored(String researchId) {
        for (IItemHandlerModifiable inv : dataInventories()) {
            for (int slot = 0; slot < inv.getSlots(); slot++) {
                if (researchId.equals(AssemblyLineManager.readResearchId(inv.getStackInSlot(slot)))) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<IItemHandlerModifiable> dataInventories() {
        List<IItemHandlerModifiable> inventories = new ArrayList<>();
        for (IDataAccessHatch hatch : dataHatches) {
            if (hatch instanceof MetaTileEntity mte) {
                inventories.add(mte.getImportItems());
            }
        }
        return inventories;
    }

    // ------------------------------------------------------------------ helpers

    private boolean consumeMaterials(List<ItemStack> costs) {
        if (costs.isEmpty() || inputInventory == null) return costs.isEmpty();
        for (ItemStack cost : costs) {
            if (countMaterial(cost) < cost.getCount()) return false;
        }
        for (ItemStack cost : costs) {
            extractMaterial(cost, cost.getCount());
        }
        return true;
    }

    private int countMaterial(ItemStack target) {
        int count = 0;
        for (int i = 0; i < inputInventory.getSlots(); i++) {
            ItemStack slot = inputInventory.getStackInSlot(i);
            if (!slot.isEmpty() && ItemStack.areItemsEqual(slot, target)
                    && ItemStack.areItemStackTagsEqual(slot, target)) {
                count += slot.getCount();
            }
        }
        return count;
    }

    private void extractMaterial(ItemStack target, int amount) {
        for (int i = 0; i < inputInventory.getSlots() && amount > 0; i++) {
            ItemStack slot = inputInventory.getStackInSlot(i);
            if (!slot.isEmpty() && ItemStack.areItemsEqual(slot, target)
                    && ItemStack.areItemStackTagsEqual(slot, target)) {
                amount -= inputInventory.extractItem(i, amount, false).getCount();
            }
        }
    }

    private boolean drawEnergy(long eut, boolean simulate) {
        if (eut <= 0) return true;
        if (energyContainer.getEnergyStored() >= eut) {
            if (!simulate) energyContainer.removeEnergy(eut);
            return true;
        }
        return false;
    }

    private void recomputeSlaveCount() {
        if (mode != Mode.CONTROL) {
            this.slaveCount = 0;
            return;
        }
        int slaves = 0;
        BlockPos origin = getPos();
        World world = getWorld();
        for (int dx = -CLUSTER_SCAN_RADIUS; dx <= CLUSTER_SCAN_RADIUS && slaves < MAX_SLAVES; dx++) {
            for (int dy = -CLUSTER_SCAN_RADIUS; dy <= CLUSTER_SCAN_RADIUS && slaves < MAX_SLAVES; dy++) {
                for (int dz = -CLUSTER_SCAN_RADIUS; dz <= CLUSTER_SCAN_RADIUS && slaves < MAX_SLAVES; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    MetaTileEntityResearchUnit unit = asResearchUnit(
                            world.getTileEntity(origin.add(dx, dy, dz)));
                    if (unit != null && unit != this && unit.mode == Mode.SLAVE && unit.isStructureFormed()) {
                        slaves++;
                    }
                }
            }
        }
        this.slaveCount = slaves;
    }

    @Nullable
    private static MetaTileEntityResearchUnit asResearchUnit(TileEntity te) {
        if (te instanceof IGregTechTileEntity gtte
                && gtte.getMetaTileEntity() instanceof MetaTileEntityResearchUnit unit) {
            return unit;
        }
        return null;
    }

    private static Mode modeFromOrdinal(int ord) {
        Mode[] values = Mode.values();
        return values[Math.floorMod(ord, values.length)];
    }

    // ------------------------------------------------------------------ interaction / GUI

    @Override
    public boolean onRightClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                codechicken.lib.raytracer.CuboidRayTraceResult hitResult) {
        if (!playerIn.isSneaking()) {
            if (!getWorld().isRemote) {
                WFGuiFactory.open(playerIn, this);
            }
            return true;
        }
        return super.onRightClick(playerIn, hand, facing, hitResult);
    }

    @Override
    public boolean onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                      codechicken.lib.raytracer.CuboidRayTraceResult hitResult) {
        if (!getWorld().isRemote) {
            this.mode = mode == Mode.CONTROL ? Mode.SLAVE : Mode.CONTROL;
            if (mode == Mode.SLAVE) jobs.clear();
            recomputeSlaveCount();
            writeCustomData(SYNC_MODE, buf -> buf.writeByte(mode.ordinal()));
            markDirty();
        }
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return ResearchTreeGui.build(this, data, syncManager, settings);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, @NotNull List<String> tooltip,
                               boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("wfcore.machine.research_unit.tooltip1"));
        tooltip.add(I18n.format("wfcore.machine.research_unit.tooltip2"));
        tooltip.add(I18n.format("wfcore.machine.research_unit.tooltip3"));
    }

    // ------------------------------------------------------------------ persistence & sync

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setByte("Mode", (byte) mode.ordinal());
        data.setTag("ResearchState", state.serializeNBT());
        NBTTagList jobList = new NBTTagList();
        for (Job job : jobs) jobList.appendTag(job.serializeNBT());
        data.setTag("Jobs", jobList);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.mode = modeFromOrdinal(data.getByte("Mode"));
        this.state.deserializeNBT(data.getCompoundTag("ResearchState"));
        this.jobs.clear();
        NBTTagList jobList = data.getTagList("Jobs", 10);
        for (int i = 0; i < jobList.tagCount(); i++) {
            this.jobs.add(Job.fromNBT(jobList.getCompoundTagAt(i)));
        }
    }

    @Override
    public void writeInitialSyncData(@NotNull PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeByte(mode.ordinal());
        buf.writeCompoundTag(state.serializeNBT());
        writeJobsTo(buf);
    }

    @Override
    public void receiveInitialSyncData(@NotNull PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.mode = modeFromOrdinal(buf.readByte());
        try {
            this.state.deserializeNBT(buf.readCompoundTag());
        } catch (Exception ignored) {}
        readJobsFrom(buf);
    }

    @Override
    public void receiveCustomData(int dataId, @NotNull PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        switch (dataId) {
            case SYNC_MODE -> this.mode = modeFromOrdinal(buf.readByte());
            case SYNC_STATE -> {
                try {
                    this.state.deserializeNBT(buf.readCompoundTag());
                } catch (Exception ignored) {}
            }
            case SYNC_JOBS -> readJobsFrom(buf);
        }
    }

    private void syncState() {
        if (getWorld() != null && !getWorld().isRemote) {
            markDirty();
            writeCustomData(SYNC_STATE, buf -> buf.writeCompoundTag(state.serializeNBT()));
        }
    }

    private void syncJobs() {
        if (getWorld() != null && !getWorld().isRemote) {
            writeCustomData(SYNC_JOBS, this::writeJobsTo);
        }
    }

    private void writeJobsTo(PacketBuffer buf) {
        buf.writeVarInt(slaveCount);
        buf.writeVarInt(jobs.size());
        for (Job job : jobs) {
            buf.writeString(job.researchId);
            buf.writeFloat(state.getProgress(job.researchId));
        }
    }

    private void readJobsFrom(PacketBuffer buf) {
        this.slaveCount = buf.readVarInt();
        clientJobProgress.clear();
        clientQueueOrder.clear();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String id = buf.readString(256);
            clientJobProgress.put(id, buf.readFloat());
            clientQueueOrder.add(id);
        }
    }

    /** Mutable per-research in-progress run worker. completedRuns lives in ResearchState. */
    public static final class Job {
        public final String researchId;
        public long accumulatedCWU;
        public int elapsedTicks;
        public boolean materialsConsumed;

        public Job(String researchId) {
            this.researchId = researchId;
        }

        public NBTTagCompound serializeNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("id", researchId);
            tag.setLong("cwu", accumulatedCWU);
            tag.setInteger("ticks", elapsedTicks);
            tag.setBoolean("mat", materialsConsumed);
            return tag;
        }

        public static Job fromNBT(NBTTagCompound tag) {
            Job job = new Job(tag.getString("id"));
            job.accumulatedCWU = tag.getLong("cwu");
            job.elapsedTicks = tag.getInteger("ticks");
            job.materialsConsumed = tag.getBoolean("mat");
            return job;
        }
    }
}
