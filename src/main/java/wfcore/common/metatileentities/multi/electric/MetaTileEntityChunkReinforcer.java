package wfcore.common.metatileentities.multi.electric;

import com.flansmod.warforge.api.WarForgeCapabilities;
import com.flansmod.warforge.api.interfaces.IChunkReinforcer;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;
import gregtech.api.capability.GregtechDataCodes;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockDisplayText;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.pattern.PatternMatchContext;
import gregtech.api.pattern.TraceabilityPredicate;
import gregtech.api.util.RelativeDirection;
import gregtech.client.renderer.ICubeRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wfcore.client.render.WFTextures;
import wfcore.common.blocks.BlockMetalSheetCasing;
import wfcore.common.blocks.BlockRegistry;

import java.util.List;

/**
 * Chunk Reinforcer: a 3x3x3 multiblock that runs like a food generator. While it sits in a claimed chunk and
 * has food fuel burning, it raises the siege difficulty of every claimed chunk within {@code radius} (WarForge
 * 2.1.0 {@link IChunkReinforcer}). Radius and the defence bonus are fixed per voltage tier in the constructor.
 */
public class MetaTileEntityChunkReinforcer extends MultiblockWithDisplayBase implements IChunkReinforcer {

    /** Ticks of burn granted per point of food hunger restored. */
    private static final int BURN_TICKS_PER_HUNGER = 200;
    private static final int CLAIM_RECHECK_INTERVAL = 20;
    private static final int SYNC_STATE = GregtechDataCodes.assignId();

    private final int tier;
    private final int reinforcementRadius;
    private final int reinforcementBonus;

    private ItemHandlerList inputInventory;
    private int burnTime;
    private boolean isActive;
    private boolean inClaimedChunk;
    private boolean syncedActive;
    private boolean syncedClaimed;

    public MetaTileEntityChunkReinforcer(ResourceLocation metaTileEntityId, int tier, int radius, int bonus) {
        super(metaTileEntityId);
        this.tier = tier;
        this.reinforcementRadius = radius;
        this.reinforcementBonus = bonus;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityChunkReinforcer(metaTileEntityId, tier, reinforcementRadius, reinforcementBonus);
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
        return aluCasing().setMinGlobalLimited(10)
                .or(abilities(MultiblockAbility.IMPORT_ITEMS).setMinGlobalLimited(1));
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
        this.inputInventory = new ItemHandlerList(getAbilities(MultiblockAbility.IMPORT_ITEMS));
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.inputInventory = null;
        this.burnTime = 0;
        this.isActive = false;
        this.inClaimedChunk = false;
        pushState();
    }

    @Override
    public boolean hasMaintenanceMechanics() {
        return false;
    }

    // ------------------------------------------------------------------ logic

    @Override
    protected void updateFormedValid() {
        if (getWorld().isRemote) return;

        if (getOffsetTimer() % CLAIM_RECHECK_INTERVAL == 0) {
            inClaimedChunk = computeClaimed();
        }

        if (!inClaimedChunk) {
            burnTime = 0;
            isActive = false;
        } else {
            if (burnTime > 0) burnTime--;
            if (burnTime <= 0) burnTime = consumeFood();
            isActive = burnTime > 0;
        }

        if (isActive != syncedActive || inClaimedChunk != syncedClaimed) {
            pushState();
        }
    }

    private boolean computeClaimed() {
        World world = getWorld();
        if (world == null) return false;
        DimChunkPos pos = new DimChunkPos(world.provider.getDimension(), getPos());
        return !Faction.nullUuid.equals(WarForgeMod.FACTIONS.getClaim(pos));
    }

    /** Consumes one food item from the input bus, returning the burn ticks it grants (0 if none). */
    private int consumeFood() {
        if (inputInventory == null) return 0;
        for (int slot = 0; slot < inputInventory.getSlots(); slot++) {
            ItemStack stack = inputInventory.getStackInSlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemFood)) continue;
            int heal = ((ItemFood) stack.getItem()).getHealAmount(stack);
            if (heal <= 0) continue;
            inputInventory.extractItem(slot, 1, false);
            return heal * BURN_TICKS_PER_HUNGER;
        }
        return 0;
    }

    // ------------------------------------------------------------------ IChunkReinforcer

    @Override
    public boolean isReinforcementActive() {
        return isStructureFormed() && isActive;
    }

    @Override
    public int getReinforcementRadius() {
        return reinforcementRadius;
    }

    @Override
    public int getReinforcementBonus() {
        return reinforcementBonus;
    }

    @Override
    public boolean stacksWithOthers() {
        return false;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == WarForgeCapabilities.CHUNK_REINFORCER) {
            return WarForgeCapabilities.CHUNK_REINFORCER.cast(this);
        }
        return super.getCapability(capability, side);
    }

    // ------------------------------------------------------------------ rendering / sync

    @Override
    public boolean isActive() {
        return super.isActive() && isActive;
    }

    private void pushState() {
        this.syncedActive = isActive;
        this.syncedClaimed = inClaimedChunk;
        markDirty();
        if (getWorld() != null && !getWorld().isRemote) {
            writeCustomData(SYNC_STATE, buf -> {
                buf.writeBoolean(isActive);
                buf.writeBoolean(inClaimedChunk);
            });
        }
    }

    @Override
    public void writeInitialSyncData(@NotNull PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeBoolean(isActive);
        buf.writeBoolean(inClaimedChunk);
    }

    @Override
    public void receiveInitialSyncData(@NotNull PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.isActive = buf.readBoolean();
        this.inClaimedChunk = buf.readBoolean();
    }

    @Override
    public void receiveCustomData(int dataId, @NotNull PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == SYNC_STATE) {
            this.isActive = buf.readBoolean();
            this.inClaimedChunk = buf.readBoolean();
            scheduleRenderUpdate();
        }
    }

    // ------------------------------------------------------------------ persistence

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("BurnTime", burnTime);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.burnTime = data.getInteger("BurnTime");
    }

    // ------------------------------------------------------------------ display

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        MultiblockDisplayText.builder(textList, isStructureFormed())
                .setWorkingStatus(true, isActive)
                .addCustom(tl -> {
                    if (!isStructureFormed()) return;
                    tl.add(new TextComponentTranslation("wfcore.machine.chunk_reinforcer.radius", reinforcementRadius));
                    tl.add(new TextComponentTranslation("wfcore.machine.chunk_reinforcer.bonus", reinforcementBonus));
                    tl.add(new TextComponentTranslation(inClaimedChunk
                            ? "wfcore.machine.chunk_reinforcer.claimed"
                            : "wfcore.machine.chunk_reinforcer.unclaimed"));
                    tl.add(new TextComponentTranslation("wfcore.machine.chunk_reinforcer.fuel", burnTime / 20));
                })
                .addWorkingStatusLine();
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, @NotNull List<String> tooltip,
                               boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("wfcore.machine.chunk_reinforcer.tooltip1"));
        tooltip.add(I18n.format("wfcore.machine.chunk_reinforcer.tooltip2",
                reinforcementRadius, reinforcementBonus));
    }
}
