package wfcore.common.metatileentities.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.google.common.collect.Lists;
import com.hbm.blocks.ModBlocks;
import gregtech.api.GTValues;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.Widget;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.gui.widgets.*;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.*;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.pattern.PatternMatchContext;
import gregtech.api.pattern.TraceabilityPredicate;
import gregtech.api.unification.material.Materials;
import gregtech.api.util.GTUtility;
import gregtech.api.util.RelativeDirection;
import gregtech.api.util.TextComponentUtil;
import gregtech.client.renderer.ICubeRenderer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wfcore.api.metatileentity.IAnimatedMTE;
import wfcore.api.radar.MultiblockRadarLogic;
import wfcore.client.render.WFTextures;
import wfcore.common.audio.WFSounds;
import wfcore.common.blocks.BlockBoltableCasing;
import wfcore.common.blocks.BlockMetalSheetCasing;
import wfcore.common.blocks.BlockRegistry;
import wfcore.common.items.registry.DataHolderRegistry;
import wfcore.common.managers.RadarSavedData;
import wfcore.common.materials.WFMaterials;
import wfcore.common.metatileentities.multi.WFPredicates;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
public class MetaTileEntityRadar extends MultiblockWithDisplayBase implements IAnimatedMTE, IProgressBarMultiblock {
    private static final String[][] patternAisles = {
            {"                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "      JJJJ      ", "      JJJJ      ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                "},
            {" GGGCG    GCGGG ", "    F      F    ", "    F      F    ", "    F      F    ", "    F      F    ", "    F      F    ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "      JJJJ      ", "    JJ    JJ    ", "    JJ    JJ    ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                "},
            {" GFCGG    GGCFG ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "    FF    FF    ", "    F F  F F    ", "    F  FF  F    ", "    F  FF  F    ", "    F F  F F    ", "   KKKKKKKKKK   ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "    JJJJJJJJ    ", "   J   EE   J   ", "   J   EE   J   ", "       EE       ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                "},
            {" GCGG      GGCG ", "  F          F  ", "  F          F  ", "  F          F  ", "  F          F  ", "  F          F  ", "                ", "                ", "                ", "                ", "                ", "  KKGGGGGGGGKK  ", "   GG      GG   ", "    F      F    ", "    FF    FF    ", "    F F  F F    ", "    F  FF  F    ", "    F  FF  F    ", "    F F  F F    ", "    FF    FF    ", "    FFFFFFFF    ", "       FF       ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "   JJJJJJJJJJ   ", "  J          J  ", "  J          J  ", "                ", "       EE       ", "       EE       ", "       EE       ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                "},
            {" CGG        GGC ", " F            F ", " F            F ", " F            F ", " F            F ", " F            F ", "  F          F  ", "  F          F  ", "  F          F  ", "  F          F  ", "  F          F  ", "  KGGGGGGGGGGK  ", "   GG      GG   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "    F      F    ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KBBK      ", "       KK       ", "                ", "     JJJJJJ     ", "  JJJ      JJJ  ", " J            J ", " J            J ", "                ", "                ", "                ", "                ", "       EE       ", "       EE       ", "                ", "                ", "                ", "                ", "                ", "                "},
            {" GG    KKK   GG ", "       KIK      ", "       KIK      ", "       KKK      ", "                ", "                ", "  F          F  ", "                ", "                ", "                ", "                ", "  KGGGGGGGGGGK  ", "                ", "                ", "   F        F   ", "                ", "                ", "                ", "                ", "   F        F   ", "   F        F   ", "                ", "     FKKHKF     ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KBBK      ", "       KK       ", "                ", "    JJJJJJJJ    ", "  JJ        JJ  ", " J            J ", " J            J ", "                ", "                ", "                ", "                ", "                ", "                ", "       EE       ", "       EE       ", "       EE       ", "                ", "                ", "                "},
            {"       KKK      ", "       KHI      ", "       KHI      ", "       KKK      ", "                ", "                ", "                ", "  F          F  ", "                ", "                ", "  F          F  ", "  KGGGGGGGGGGK  ", "           K    ", "                ", "                ", "   F        F   ", "                ", "                ", "   F        F   ", "                ", "   F        F   ", "                ", "    KKKKHKKK    ", "                ", "                ", "      DDDD      ", "       DD       ", "       DD       ", "       BB       ", "       BB       ", "      JJJJ      ", "    JJ    JJ    ", " JJJ        JJJ ", "J              J", "J              J", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "       EE       ", "                ", "                "},
            {"       KKK      ", "       KHI      ", "       KHI      ", "       KHK      ", "        H       ", "        H       ", "        H       ", "        H       ", "  F     H    F  ", "  F     H    F  ", "        H       ", "  KGGGGGHHHHGK  ", "        H  A    ", "        H       ", "        H       ", "        H       ", "   F    H   F   ", "   F    H   F   ", "        H       ", "        H       ", "   F    H   F   ", "   F    H   F   ", "    KKKKHKKK    ", "                ", "                ", "      DDDD      ", "       DD       ", "       DD       ", "       BB       ", "       BB       ", "      JJJJ      ", "    JJ    JJ    ", " JJJ        JJJ ", "J E          E J", "J E          E J", "  E          E  ", "   E        E   ", "   E        E   ", "   E        E   ", "    E      E    ", "    E      E    ", "     E    E     ", "     E    E     ", "     E    E     ", "      E  E      ", "       BB       ", "       BB       "},
            {"       KKK      ", "       KKK      ", "       KKK      ", "       KKK      ", "        K       ", "        K       ", "        K       ", "        K       ", "  F     K    F  ", "  F     K    F  ", "        K       ", "  KGGGGGGGGGGK  ", "           K    ", "                ", "                ", "                ", "   F        F   ", "   F        F   ", "                ", "                ", "   F        F   ", "   F        F   ", "    KKKKKKKK    ", "                ", "                ", "      DDDD      ", "       DD       ", "       DD       ", "       BB       ", "       BB       ", "      JJJJ      ", "    JJ    JJ    ", " JJJ        JJJ ", "J E          E J", "J E          E J", "  E          E  ", "   E        E   ", "   E        E   ", "   E        E   ", "    E      E    ", "    E      E    ", "     E    E     ", "     E    E     ", "     E    E     ", "      E  E      ", "       BB       ", "       BB       "},
            {"                ", "                ", "                ", "                ", "                ", "                ", "                ", "  F          F  ", "                ", "                ", "  F          F  ", "  KGGGGG GGGGK  ", "           K    ", "                ", "                ", "   F        F   ", "                ", "                ", "   F        F   ", "                ", "   F        F   ", "                ", "    KKKKKKKK    ", "                ", "                ", "      DDDD      ", "       DD       ", "       DD       ", "       BB       ", "       BB       ", "      JJJJ      ", "    JJ    JJ    ", " JJJ        JJJ ", "J              J", "J              J", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "       EE       ", "                ", "                "},
            {" GG          GG ", "                ", "                ", "                ", "                ", "                ", "  F          F  ", "                ", "                ", "                ", "                ", "  KGGGGGGGGGGK  ", "                ", "                ", "   F        F   ", "                ", "                ", "                ", "                ", "   F        F   ", "   F        F   ", "                ", "     FKKKKF     ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KBBK      ", "       KK       ", "                ", "    JJJJJJJJ    ", "  JJ        JJ  ", " J            J ", " J            J ", "                ", "                ", "                ", "                ", "                ", "                ", "       EE       ", "       EE       ", "       EE       ", "                ", "                ", "                "},
            {" CGG        GGC ", " F            F ", " F            F ", " F            F ", " F            F ", " F            F ", "  F          F  ", "  F          F  ", "  F          F  ", "  F          F  ", "  F          F  ", "  KGGGGGGGGGGK  ", "   GG      GG   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "    F      F    ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KBBK      ", "       KK       ", "                ", "     JJJJJJ     ", "  JJJ      JJJ  ", " J            J ", " J            J ", "                ", "                ", "                ", "                ", "       EE       ", "       EE       ", "                ", "                ", "                ", "                ", "                ", "                "},
            {" GCGG      GGCG ", "  F          F  ", "  F          F  ", "  F          F  ", "  F          F  ", "  F          F  ", "                ", "                ", "                ", "                ", "                ", "  KKGGGGGGGGKK  ", "   GG      GG   ", "    F      F    ", "    FF    FF    ", "    F F  F F    ", "    F  FF  F    ", "    F  FF  F    ", "    F F  F F    ", "    FF    FF    ", "    FFFFFFFF    ", "       FF       ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "   JJJJJJJJJJ   ", "  J          J  ", "  J          J  ", "                ", "       EE       ", "       EE       ", "       EE       ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                "},
            {" GFCGG    GGCFG ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "    FF    FF    ", "    F F  F F    ", "    F  FF  F    ", "    F  FF  F    ", "    F F  F F    ", "   KKKKKKKKKK   ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "    JJJJJJJJ    ", "   J   EE   J   ", "   J   EE   J   ", "       EE       ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                "},
            {" GGGCG    GCGGG ", "    F      F    ", "    F      F    ", "    F      F    ", "    F      F    ", "    F      F    ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "      JJJJ      ", "    JJ    JJ    ", "    JJ    JJ    ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                "},
            {"                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "      JJJJ      ", "      JJJJ      ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                "}
    };
    private static final ArrayList<int[]> ANIM_BLOCKS_CONTROLLER_OFFSETS = new ArrayList<>();  // char offset, string offset, aisle offset
    private static final RelativeDirection charRelDir = RelativeDirection.FRONT;
    private static final RelativeDirection stringRelDir = RelativeDirection.UP;
    private static final RelativeDirection aisleRelDir = RelativeDirection.RIGHT;
    private static final int SYNC_RADAR_PROGRESS = 900;
    private static final int SYNC_RADAR_STATE = 901;
    private static final int tier = GTValues.EV;

    static {
        final int animatedLayerIndex = 22;
        final int controllerAisleIdx = 8;
        final int controllerStringIdx = 12;
        final int controllerCharIdx = 4;
        for (int aisleIdx = 0; aisleIdx < patternAisles.length; ++aisleIdx) {
            String[] aisle = patternAisles[aisleIdx];
            for (int stringIdx = animatedLayerIndex; stringIdx < aisle.length; ++stringIdx) {
                String currLayer = aisle[stringIdx];
                for (int charIdx = 0; charIdx < currLayer.length(); ++charIdx) {
                    if (currLayer.charAt(charIdx) == ' ' || currLayer.charAt(charIdx) == '#') {
                        continue;
                    }
                    ANIM_BLOCKS_CONTROLLER_OFFSETS.add(new int[]{charIdx - controllerCharIdx, stringIdx - controllerStringIdx, aisleIdx - controllerAisleIdx});
                }
            }
        }
    }

    private final MultiblockRadarLogic logic = new MultiblockRadarLogic(this);  // this should be created/modified whenever structure is formed/modified
    private final ItemStackHandler dataStickSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            markDirty();
        }

        @Override
        @Nonnull
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // If the radar is currently scanning, block all extraction
            if (isActive()) {
                return ItemStack.EMPTY;
            }
            logic.finished = false;
            return super.extractItem(slot, amount, simulate);
        }

        protected int getStackLimit(int slot, @NotNull ItemStack stack) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return DataHolderRegistry.isAllowed(stack);
        }
    };
    private final Collection<BlockPos> ANIMATED_BLOCKS;
    @Getter
    protected IEnergyContainer energyContainer;
    @Getter
    private String animState = "idle";
    @Getter
    private long animEpoch = 0L;

    public MetaTileEntityRadar(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
        ANIMATED_BLOCKS = new ArrayList<>(ANIM_BLOCKS_CONTROLLER_OFFSETS.size());
    }

    public void setAnimState(String state) {
        if(this.animState.equals(state)) return;
        this.animState = state;
        this.animEpoch = getWorld().getTotalWorldTime();
        if(!getWorld().isRemote){
            writeCustomData(CHANGE_ANIM, buf -> {
                buf.writeString(animState);
                buf.writeLong(animEpoch);
            });
        }

    }
    public void setAnimEpoch() {
        this.animEpoch = getWorld().getTotalWorldTime();
        if(!getWorld().isRemote){
            writeCustomData(CHANGE_ANIM, buf -> {
                buf.writeString(animState);
                buf.writeLong(animEpoch);
            });;
        }

    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.animState = buf.readString(32); // Limit string length for safety
        this.animEpoch = buf.readLong();
    }


    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);

        switch (dataId) {
            case SYNC_RADAR_PROGRESS -> this.logic.setClientProgress(buf.readInt());
            case SYNC_RADAR_STATE -> this.logic.setActive(buf.readBoolean());
            case CHANGE_ANIM -> {
                this.animState = buf.readString(32);
                this.animEpoch = buf.readLong();
            }
        }
    }

    @Override
    protected void addWarningText(List<ITextComponent> textList) {
        MultiblockDisplayText.builder(textList, isStructureFormed(), false)
                .addLowPowerLine(logic.isHasNotEnoughEnergy())
                .addMaintenanceProblemLines(getMaintenanceProblems());
    }

    public long getEnergyInputPerSecond() {
        return energyContainer.getInputPerSec();
    }

    public int getEnergyTier() {
        int currentTier;
        if (energyContainer == null) {
            currentTier = tier;
        } else {
            // Original logic: clamping voltage-based tier between this.tier and this.tier + 1
            currentTier = Math.min(tier + 1,
                    Math.max(tier, GTUtility.getFloorTierByVoltage(energyContainer.getInputVoltage())));
        }

        // Ensure the result is at least 4 (EV)
        return Math.max(GTValues.EV, currentTier);
    }

    public boolean drainEnergy(boolean simulate) {
        long energyToDrain = GTValues.VA[getEnergyTier()];
        long resultEnergy = energyContainer.getEnergyStored() - energyToDrain;
        if (resultEnergy >= 0L && resultEnergy <= energyContainer.getEnergyCapacity()) {
            if (!simulate)
                energyContainer.changeEnergy(-energyToDrain);
            return true;
        }
        return false;
    }

    @Override
    protected void updateFormedValid() {
        // I think this is called when the multiblock is formed and valid, but allows extra checks such as muffler blocking
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, @NotNull List<String> tooltip, boolean advanced) {
        super.addInformation(stack, world, tooltip, advanced);
        tooltip.add(I18n.format("wfcore.machine.radar.tooltip.1"));
        tooltip.add(I18n.format("wfcore.machine.radar.tooltip.2"));
        tooltip.add(I18n.format("wfcore.machine.radar.tooltip.3"));
        tooltip.add(I18n.format("wfcore.machine.radar.tooltip.4"));
        tooltip.add(I18n.format("wfcore.machine.radar.tooltip.5"));
    }

    @NotNull
    @Override
    protected ICubeRenderer getFrontOverlay() {
        return WFTextures.OVERLAY_RADAR;
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return WFTextures.ALU_SHEET;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity iGregTechTileEntity) {
        return new MetaTileEntityRadar(this.metaTileEntityId);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        initializeAbilities();
        initAnimatedBlocks();
        logic.structureFormed();
        var world = getWorld();
        if (world != null && !world.isRemote) {
            disableBlockRendering(true);
        }
    }

    @Override
    public void checkStructurePattern() {
        super.checkStructurePattern();
        boolean correctY = isCorrectY();
        if (this.isStructureFormed() && !(correctY && hasSkylightAccess())) {
            this.invalidateStructure();
        }

        if (this.isStructureFormed()) {
            if (getTier() < GTValues.EV) {
                this.invalidateStructure();
            }
        }
    }

    public float getVolume() {
        return 6.0f;
    }

    private boolean isCorrectY() {
        return this.getPos().getY() >= 100;
    }

    public boolean hasSkylightAccess() {
        BlockPos pos = getPos().up(35);
        int topBlockY = getWorld().getHeight(pos.getX(), pos.getZ());
        return topBlockY <= pos.getY() + 35;
    }

    private void initAnimatedBlocks() {
        for (int[] blockOffset : ANIM_BLOCKS_CONTROLLER_OFFSETS) {
            // shift by the stored amount of char, string, and aisle; should automatically handle negatives
            ANIMATED_BLOCKS.add(getPos()
                    .offset(charRelDir.getActualFacing(getFrontFacing()), blockOffset[0])
                    .offset(stringRelDir.getActualFacing(getFrontFacing()), blockOffset[1])
                    .offset(aisleRelDir.getActualFacing(getFrontFacing()), blockOffset[2])
            );
        }
    }

    @Override
    protected @NotNull BlockPattern createStructurePattern() {
        // don't change the relative directions without also updating the animated blocks initializer
        return FactoryBlockPattern.start(charRelDir, stringRelDir, aisleRelDir)
                .aisle(patternAisles[0])
                .aisle(patternAisles[1])
                .aisle(patternAisles[2])
                .aisle(patternAisles[3])
                .aisle(patternAisles[4])
                .aisle(patternAisles[5])
                .aisle(patternAisles[6])
                .aisle(patternAisles[7])
                .aisle(patternAisles[8])
                .aisle(patternAisles[9])
                .aisle(patternAisles[10])
                .aisle(patternAisles[11])
                .aisle(patternAisles[12])
                .aisle(patternAisles[13])
                .aisle(patternAisles[14])
                .aisle(patternAisles[15])
                .where('A', selfPredicate())
                .where('B', WFPredicates.compressedBlocks(Materials.Aluminium))
                .where('D', WFPredicates.compressedBlocks(Materials.Lead))
                .where('E', frames(Materials.Aluminium))
                .where('G', blocks(ModBlocks.concrete_smooth))
                .where('H', blocks(ModBlocks.deco_red_copper))
                .where('J', boltable())
                .where('C', WFPredicates.compressedBlocks(Materials.Steel))
                .where('F', frames(WFMaterials.GalvanizedSteel))
                .where('I', abilities(MultiblockAbility.INPUT_ENERGY, MultiblockAbility.COMPUTATION_DATA_RECEPTION).or(aluCasing().setMaxGlobalLimited(4)))
                .where('K', aluCasing())
                .where('#', any())
                .where(' ', any())
                .build();
    }

    public TraceabilityPredicate aluCasing() {
        return states(BlockRegistry.SHEET_CASING.getState(BlockMetalSheetCasing.MetalSheetCasingType.ALUMINIUM_SHEET_CASING));
    }

    public TraceabilityPredicate boltable() {
        return states(BlockRegistry.BOLTABLE_CASING.getState(BlockBoltableCasing.BoltableCasingType.BORON_COATED_BOLTED));
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        resetTileAbilities();
        logic.invalidateStructure();
        disableBlockRendering(false);
        if (!getWorld().isRemote) {
            setAnimState("idle");
            writeCustomData(SYNC_RADAR_STATE, buf -> buf.writeBoolean(false));
            writeCustomData(SYNC_RADAR_PROGRESS, buf -> buf.writeInt(0));
        }
    }

    public int getTier() {
        if (energyContainer == null) {
            return -1;
        }
        return GTUtility.getTierByVoltage(energyContainer.getInputVoltage());
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeString(animState);
        buf.writeLong(animEpoch);
        var world = getWorld();
        if (world != null && !world.isRemote) {
            disableBlockRendering(isStructureFormed());
        }
    }

    @Override
    public void update() {
        super.update();
        if (getWorld().isRemote)
            return;

        String targetState = isActive() ? "running" : "idle";
        if (!this.animState.equals(targetState)) {
            setAnimState(targetState);
        }

        if (logic.tickScan()) {
            completeScan();
        }

        if (logic.getScanProgress() % 20 == 0) {
            writeCustomData(SYNC_RADAR_PROGRESS, buf -> buf.writeInt(logic.getScanProgress()));
        }
    }

    @SideOnly(Side.CLIENT)
    public SoundEvent getSound() {
        return WFSounds.RADAR_LOOP;
    }

    public boolean hasDataStick() {
        return !dataStickSlot.getStackInSlot(0).isEmpty();
    }

    private void completeScan() {
        logic.setActive(false);
        ItemStack stick = dataStickSlot.getStackInSlot(0);

        if (!stick.isEmpty() && logic.lastScan != null) {


            NBTTagCompound nbt = stick.hasTagCompound() ? stick.getTagCompound() : new NBTTagCompound();

            assert nbt != null;
            if (nbt.hasKey("TargetUUID")) {
                var uuid = nbt.getUniqueId("TargetUUID");
                RadarSavedData.get().rmScan(uuid);
            }

            nbt.setUniqueId("TargetUUID", logic.lastScan);
            nbt.setBoolean("is_analyzed", false);
            nbt.setString("title", "Radar Scan: " + logic.lastScan.toString().substring(0, 8));
            stick.setTagCompound(nbt);
        }

        writeCustomData(SYNC_RADAR_STATE, buf -> buf.writeBoolean(false));
        writeCustomData(SYNC_RADAR_PROGRESS, buf -> buf.writeInt(0));
        logic.lastScan = null;
        markDirty();
    }


    @Override
    public boolean isActive() {
        return logic != null && logic.isActive();
    }

    protected void initializeAbilities() {
        this.energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
    }

    private void resetTileAbilities() {
        this.energyContainer = new EnergyContainerList(Lists.newArrayList());
    }

    @Override
    public boolean hasMaintenanceMechanics() {
        return false;
    }

    private void onScanClick(Widget.ClickData data) {
        if (!getWorld().isRemote && !isActive() && logic.canScan()) {
            logic.startScan();
            writeCustomData(SYNC_RADAR_STATE, buf -> buf.writeBoolean(true));
            markDirty();
        }
    }


    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        boolean flag = false;
        if (!isCorrectY()) {
            textList.add(TextComponentUtil.stringWithColor(TextFormatting.RED,
                    new TextComponentTranslation("wfcore.multiblock.error.height_limit").getFormattedText()));
            flag = true;
        }
        if (!hasSkylightAccess()) {
            textList.add(TextComponentUtil.stringWithColor(TextFormatting.RED,
                    new TextComponentTranslation("wfcore.multiblock.error.no_sky").getFormattedText()));

            flag = true;
        }
        if (flag) return;

        MultiblockDisplayText.builder(textList, this.isStructureFormed())
                .addEnergyUsageLine(energyContainer)
                .addCustom(tl -> {

                    if (!this.isStructureFormed()) return;


                    boolean isScanning = logic.isActive();

                    if (isScanning) {
                        // Progress State
                        String progress = String.format("Scanning... %f%%", logic.getProgressPercent());
                        tl.add(TextComponentUtil.stringWithColor(TextFormatting.YELLOW, progress));
                    } else if (!logic.finished) {
                        // Idle/Waiting State
                        if (!hasDataStick()) {
                            tl.add(TextComponentUtil.stringWithColor(TextFormatting.YELLOW,
                                    new TextComponentTranslation("wfcore.gui.info.status.insert.datastick").getFormattedText()));
                        } else
                            tl.add(TextComponentUtil.stringWithColor(TextFormatting.GREEN,
                                    new TextComponentTranslation("wfcore.gui.info.status.ready").getFormattedText()));
                        tl.add(TextComponentUtil.stringWithColor(TextFormatting.AQUA,
                                new TextComponentTranslation("wfcore.gui.info.status.total_time", logic.getScanDurationTicks() / 20).getFormattedText()));
                    } else {
                        //Finished state
                        tl.add(TextComponentUtil.stringWithColor(TextFormatting.AQUA, new TextComponentTranslation("wfcore.gui.info.status.saved").getFormattedText()));
                    }
                });
    }


    protected ModularUI.Builder createUITemplate(EntityPlayer entityPlayer) {
        ModularUI.Builder builder = ModularUI.builder(GuiTextures.BACKGROUND, 198, 208);
        IProgressBarMultiblock progressMulti = this;


        builder.image(4, 4, 190, 117, GuiTextures.DISPLAY);
        builder.widget(new IndicatorImageWidget(174, 101, 17, 17, getLogo())
                .setWarningStatus(getWarningLogo(), this::addWarningText)
                .setErrorStatus(getErrorLogo(), this::addErrorText));

        builder.label(9, 9, getMetaFullName(), 0xFFFFFF);
        builder.widget(new AdvancedTextWidget(9, 20, this::addDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(181)
                .setClickHandler(this::handleDisplayClick));

        builder.widget(new SlotWidget(dataStickSlot, 0, 173, 161).setBackgroundTexture(GuiTextures.SLOT)
                .setTooltipText("wfcore.gui.tooltips.data_stick_slot"));

        builder.widget(new ToggleButtonWidget(173, 183, 18, 18,
                GuiTextures.BUTTON_POWER,
                this.logic::isActive,
                _ -> this.onScanClick(null)));
        builder.widget(new ImageWidget(173, 201, 18, 6, GuiTextures.BUTTON_POWER_DETAIL));
        ProgressWidget progressBar = new ProgressWidget(
                () -> progressMulti.getFillPercentage(0),
                20, 110, 94, 7,
                progressMulti.getProgressBarTexture(0), ProgressWidget.MoveType.HORIZONTAL)
                .setHoverTextConsumer(list -> progressMulti.addBarHoverText(list, 0));
        builder.widget(progressBar);

        builder.bindPlayerInventory(entityPlayer.inventory, 125);
        return builder;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        EnumFacing relativeRight = RelativeDirection.RIGHT.getRelativeFacing(getFrontFacing(), getUpwardsFacing(),
                isFlipped());
        EnumFacing relativeBack = RelativeDirection.FRONT.getRelativeFacing(getFrontFacing(), getUpwardsFacing(),
                isFlipped());

        return new AxisAlignedBB(
                this.getPos().offset(relativeBack, 11).offset(relativeRight.getOpposite(), 8),
                this.getPos().offset(relativeBack.getOpposite(), 5).offset(relativeRight, 8).offset(EnumFacing.UP, 38));
    }

    @Override
    public Vec3d getTransform() {
        return switch (this.getFrontFacing()) {
            case WEST -> new Vec3d(-3, 10, 1);
            case EAST -> new Vec3d(4, 10, 0);
            case NORTH -> new Vec3d(0, 10, -3);
            case SOUTH -> new Vec3d(1, 10, 4);
            default -> Vec3d.ZERO;
        };
    }

    public BlockPos getLightPos() {
        return thisObject().getPos().up(15);
    }

    @Override
    public boolean shouldRender() {
        return isStructureFormed();
    }

    @Override
    public boolean allowsExtendedFacing() {
        return false;
    }

    @Override
    public Collection<BlockPos> getHiddenBlocks() {
        return ANIMATED_BLOCKS;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        logic.writeToNBT(data);
        data.setTag("SlotDataStick", dataStickSlot.serializeNBT());
        data.setString("AnimState", this.animState);
        data.setLong("AnimEpoch", this.animEpoch);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        logic.readFromNBT(data);
        this.animState = data.getString("AnimState");
        this.animEpoch = data.getLong("AnimEpoch");
        if (data.hasKey("SlotDataStick")) {
            dataStickSlot.deserializeNBT(data.getCompoundTag("SlotDataStick"));
        }
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        getFrontOverlay().renderSided(getFrontFacing(), renderState, translation, pipeline);
        super.renderMetaTileEntity(renderState, translation, pipeline);
    }


    public double getRenderDistanceSqared() {
        return 262144D; //512
    }

    @Override
    public double getFillPercentage(int index) {
        return logic.getProgressPercent() / 100;
    }

    @Override
    public TextureArea getProgressBarTexture(int index) {
        return GuiTextures.PROGRESS_BAR_HPCA_COMPUTATION;
    }
}