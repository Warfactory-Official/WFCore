package wfcore.common.metatileentities.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.google.common.collect.Lists;
import com.hbm.blocks.ModBlocks;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.AdvancedTextWidget;
import gregtech.api.gui.widgets.ClickButtonWidget;
import gregtech.api.gui.widgets.IndicatorImageWidget;
import gregtech.api.items.itemhandlers.GTItemStackHandler;
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
import gregtech.api.unification.material.Materials;
import gregtech.api.util.GTUtility;
import gregtech.api.util.RelativeDirection;
import gregtech.api.util.TextComponentUtil;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import wfcore.api.capability.data.IDataStorage;
import wfcore.api.metatileentity.IAnimatedMTE;
import wfcore.api.radar.MultiblockRadarLogic;
import wfcore.api.util.math.ClusterData;
import wfcore.client.render.WFTextures;
import wfcore.common.blocks.BlockBoltableCasing;
import wfcore.common.blocks.BlockMetalSheetCasing;
import wfcore.common.blocks.BlockRegistry;
import wfcore.common.materials.WFMaterials;
import wfcore.common.metatileentities.multi.WFPredicates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

;

public class MetaTileEntityRadar extends MultiblockWithDisplayBase implements IAnimatedMTE {
    private final MultiblockRadarLogic logic = new MultiblockRadarLogic(this);  // this should be created/modified whenever structure is formed/modified

    protected IItemHandlerModifiable inputInventory;
    protected IItemHandlerModifiable outputInventory;
    protected IMultipleTankHandler inputFluidInventory;
    protected IMultipleTankHandler outputFluidInventory;
    protected IEnergyContainer energyContainer;
    @Getter
    String animState = "idle";
    @Getter
    long animEpoch = 0l;
    private long tickCounter = 0;
    private boolean tryWrite = false;

    public MetaTileEntityRadar(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    // I think this is called when the multiblock is formed and valid, but allows extra checks such as muffler blocking
    @Override
    protected void updateFormedValid() {

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
    public void checkStructurePattern() {
        super.checkStructurePattern();
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        initializeAbilities();
        logic.structureFormed();
    }

    protected @NotNull BlockPattern createStructurePattern() {
        return
                FactoryBlockPattern.start(RelativeDirection.FRONT,RelativeDirection.UP,RelativeDirection.RIGHT)
                        .aisle("                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "      JJJJ      ", "      JJJJ      ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ")
                        .aisle(" GGGCG    GCGGG ", "    F      F    ", "    F      F    ", "    F      F    ", "    F      F    ", "    F      F    ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "      JJJJ      ", "    JJ    JJ    ", "    JJ    JJ    ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ")
                        .aisle(" GFCGG    GGCFG ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "    FF    FF    ", "    F F  F F    ", "    F  FF  F    ", "    F  FF  F    ", "    F F  F F    ", "   KKKKKKKKKK   ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "    JJJJJJJJ    ", "   J   EE   J   ", "   J   EE   J   ", "       EE       ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ")
                        .aisle(" GCGG      GGCG ", "  F          F  ", "  F          F  ", "  F          F  ", "  F          F  ", "  F          F  ", "                ", "                ", "                ", "                ", "                ", "  KKGGGGGGGGKK  ", "   GG      GG   ", "    F      F    ", "    FF    FF    ", "    F F  F F    ", "    F  FF  F    ", "    F  FF  F    ", "    F F  F F    ", "    FF    FF    ", "    FFFFFFFF    ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "   JJJJJJJJJJ   ", "  J          J  ", "  J          J  ", "                ", "       EE       ", "       EE       ", "       EE       ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ")
                        .aisle(" CGG        GGC ", " F            F ", " F            F ", " F            F ", " F            F ", " F            F ", "  F          F  ", "  F          F  ", "  F          F  ", "  F          F  ", "  F          F  ", "  KGGGGGGGGGGK  ", "   GG      GG   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "    F      F    ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KBBK      ", "       KK       ", "                ", "     JJJJJJ     ", "  JJJ      JJJ  ", " J            J ", " J            J ", "                ", "                ", "                ", "                ", "       EE       ", "       EE       ", "                ", "                ", "                ", "                ", "                ", "                ")
                        .aisle(" GG    KKK   GG ", "       KIK      ", "       KIK      ", "       KKK      ", "                ", "                ", "  F          F  ", "                ", "                ", "                ", "                ", "  KGGGGGGGGGGK  ", "                ", "                ", "   F        F   ", "                ", "                ", "                ", "                ", "   F        F   ", "   F        F   ", "                ", "     FKKHKF     ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KBBK      ", "       KK       ", "                ", "    JJJJJJJJ    ", "  JJ        JJ  ", " J            J ", " J            J ", "                ", "                ", "                ", "                ", "                ", "                ", "       EE       ", "       EE       ", "       EE       ", "                ", "                ", "                ")
                        .aisle("       KKK      ", "       KHI      ", "       KHI      ", "       KKK      ", "                ", "                ", "                ", "  F          F  ", "                ", "                ", "  F          F  ", "  KGGGGGGGGGGK  ", "           K    ", "                ", "                ", "   F        F   ", "                ", "                ", "   F        F   ", "                ", "   F        F   ", "                ", "    KKKKHKKK    ", "                ", "                ", "      DDDD      ", "       DD       ", "       DD       ", "       BB       ", "       BB       ", "      JJJJ      ", "    JJ    JJ    ", " JJJ        JJJ ", "J              J", "J              J", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "       EE       ", "                ", "                ")
                        .aisle("       KKK      ", "       KHI      ", "       KHI      ", "       KHK      ", "        H       ", "        H       ", "        H       ", "        H       ", "  F     H    F  ", "  F     H    F  ", "        H       ", "  KGGGGGHHHHGK  ", "        H  A    ", "        H       ", "        H       ", "        H       ", "   F    H   F   ", "   F    H   F   ", "        H       ", "        H       ", "   F    H   F   ", "   F    H   F   ", "    KKKKHKKK    ", "                ", "                ", "      DDDD      ", "       DD       ", "       DD       ", "       BB       ", "       BB       ", "      JJJJ      ", "    JJ    JJ    ", " JJJ        JJJ ", "J E          E J", "J E          E J", "  E          E  ", "   E        E   ", "   E        E   ", "   E        E   ", "    E      E    ", "    E      E    ", "     E    E     ", "     E    E     ", "     E    E     ", "      E  E      ", "       BB       ", "       BB       ")
                        .aisle("       KKK      ", "       KKK      ", "       KKK      ", "       KKK      ", "        K       ", "        K       ", "        K       ", "        K       ", "  F     K    F  ", "  F     K    F  ", "        K       ", "  KGGGGGGGGGGK  ", "           K    ", "                ", "                ", "                ", "   F        F   ", "   F        F   ", "                ", "                ", "   F        F   ", "   F        F   ", "    KKKKKKKK    ", "                ", "                ", "      DDDD      ", "       DD       ", "       DD       ", "       BB       ", "       BB       ", "      JJJJ      ", "    JJ    JJ    ", " JJJ        JJJ ", "J E          E J", "J E          E J", "  E          E  ", "   E        E   ", "   E        E   ", "   E        E   ", "    E      E    ", "    E      E    ", "     E    E     ", "     E    E     ", "     E    E     ", "      E  E      ", "       BB       ", "       BB       ")
                        .aisle("                ", "                ", "                ", "                ", "                ", "                ", "                ", "  F          F  ", "                ", "                ", "  F          F  ", "  KGGGGG GGGGK  ", "           K    ", "                ", "                ", "   F        F   ", "                ", "                ", "   F        F   ", "                ", "   F        F   ", "                ", "    KKKKKKKK    ", "                ", "                ", "      DDDD      ", "       DD       ", "       DD       ", "       BB       ", "       BB       ", "      JJJJ      ", "    JJ    JJ    ", " JJJ        JJJ ", "J              J", "J              J", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "       EE       ", "                ", "                ")
                        .aisle(" GG          GG ", "                ", "                ", "                ", "                ", "                ", "  F          F  ", "                ", "                ", "                ", "                ", "  KGGGGGGGGGGK  ", "                ", "                ", "   F        F   ", "                ", "                ", "                ", "                ", "   F        F   ", "   F        F   ", "                ", "     FKKKKF     ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KBBK      ", "       KK       ", "                ", "    JJJJJJJJ    ", "  JJ        JJ  ", " J            J ", " J            J ", "                ", "                ", "                ", "                ", "                ", "                ", "       EE       ", "       EE       ", "       EE       ", "                ", "                ", "                ")
                        .aisle(" CGG        GGC ", " F            F ", " F            F ", " F            F ", " F            F ", " F            F ", "  F          F  ", "  F          F  ", "  F          F  ", "  F          F  ", "  F          F  ", "  KGGGGGGGGGGK  ", "   GG      GG   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "    F      F    ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KKKK      ", "      KBBK      ", "       KK       ", "                ", "     JJJJJJ     ", "  JJJ      JJJ  ", " J            J ", " J            J ", "                ", "                ", "                ", "                ", "       EE       ", "       EE       ", "                ", "                ", "                ", "                ", "                ", "                ")
                        .aisle(" GCGG      GGCG ", "  F          F  ", "  F          F  ", "  F          F  ", "  F          F  ", "  F          F  ", "                ", "                ", "                ", "                ", "                ", "  KKGGGGGGGGKK  ", "   GG      GG   ", "    F      F    ", "    FF    FF    ", "    F F  F F    ", "    F  FF  F    ", "    F  FF  F    ", "    F F  F F    ", "    FF    FF    ", "    FFFFFFFF    ", "       FF       ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "   JJJJJJJJJJ   ", "  J          J  ", "  J          J  ", "                ", "       EE       ", "       EE       ", "       EE       ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ")
                        .aisle(" GFCGG    GGCFG ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "   F        F   ", "    FF    FF    ", "    F F  F F    ", "    F  FF  F    ", "    F  FF  F    ", "    F F  F F    ", "   KKKKKKKKKK   ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "    JJJJJJJJ    ", "   J   EE   J   ", "   J   EE   J   ", "       EE       ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ")
                        .aisle(" GGGCG    GCGGG ", "    F      F    ", "    F      F    ", "    F      F    ", "    F      F    ", "    F      F    ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "      JJJJ      ", "    JJ    JJ    ", "    JJ    JJ    ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ")
                        .aisle("                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "      JJJJ      ", "      JJJJ      ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ", "                ")
                        .where('A', selfPredicate())
                        .where('B', WFPredicates.compressedBlocks(Materials.Aluminium))
                        .where('D', WFPredicates.compressedBlocks(Materials.Lead))
                        .where('E', frames(Materials.Aluminium))
                        .where('G', blocks(ModBlocks.concrete_smooth))
                        .where('H', blocks(ModBlocks.deco_red_copper))
                        .where('J', boltable())
                        .where('C', WFPredicates.compressedBlocks(Materials.Steel))
                        .where('F', frames(WFMaterials.GalvanizedSteel))
                        .where('I', abilities(MultiblockAbility.INPUT_ENERGY, MultiblockAbility.IMPORT_ITEMS)) //FIXME
                        .where('K', aluCasing())
                        .where('#', any())
                        .where(' ', air())
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
    }

    public int getTier() {
        if (energyContainer == null) {
            return -1;
        }

        return GTUtility.getTierByVoltage(energyContainer.getInputVoltage());
    }

    // do radar stuff every tick on the server
    @Override
    public void update() {
        super.update();  // YOU MUST DO THIS FOR THE STRUCTURE TO FORM AND OTHER IMPORTANT STUFF TO OCCUR

        // ignore clients
        if (this.getWorld().isRemote) {
            return;
        }

        // update tick counter
        ++tickCounter;
        tickCounter &= 1L << 63;

        // only update once a second
        if (tickCounter % 20 != 0) {
            return;
        }

        // no items to process or not allowed to process
        if (inputInventory == null || inputInventory.getSlots() == 0 || !tryWrite) {
            return;
        }

        // check all slots
        for (int slotId = 0; slotId < inputInventory.getSlots(); ++slotId) {
            // check if there is a data storage device/ stack
            var slotStack = inputInventory.getStackInSlot(slotId);  // DO NOT MODIFY STACK
            if (!(slotStack.getItem() instanceof IDataStorage storage)) {
                return;
            }

            // check if we have data to store
            if (logic.lastScan != null) {
                // write the last scan to the data storage item
                var stackToWrite = slotStack.copy();  // create copy that we are allowed to modify
                var unwrittenIt = logic.lastScan.iterator();
                while (unwrittenIt.hasNext() && storage.writeData(stackToWrite, unwrittenIt.next())) {}

                // probably shouldn't try to rewrite all the data if it fails, but should be fine
                tryWrite = unwrittenIt.hasNext();  // update tryWrite on successful writes only
                inputInventory.setStackInSlot(slotId, stackToWrite);
            }
        }
    }

    @Override
    public boolean isActive() {
        return logic != null && logic.isActive();
    }

    protected void initializeAbilities() {
        this.inputInventory = new ItemHandlerList(getAbilities(MultiblockAbility.IMPORT_ITEMS));
        this.inputFluidInventory = new FluidTankList(true,
                getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        this.outputInventory = new ItemHandlerList(getAbilities(MultiblockAbility.EXPORT_ITEMS));
        this.outputFluidInventory = new FluidTankList(true,
                getAbilities(MultiblockAbility.EXPORT_FLUIDS));
        this.energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
    }

    private void resetTileAbilities() {
        this.inputInventory = new GTItemStackHandler(this, 0);
        this.inputFluidInventory = new FluidTankList(true);
        this.outputInventory = new GTItemStackHandler(this, 0);
        this.outputFluidInventory = new FluidTankList(true);
        this.energyContainer = new EnergyContainerList(Lists.newArrayList());
    }

    @Override
    public boolean hasMaintenanceMechanics() {
        return false;
    }

    private void onScanClick(Widget.ClickData data) {
        logic.performScan();
    }

    private void onWriteToggleClick(Widget.ClickData data) {
        tryWrite = !tryWrite;
    }

    protected void addDisplayText(List<ITextComponent> textList) {
        MultiblockDisplayText.builder(textList, this.isStructureFormed()).addCustom(tl -> {
            // get the cluster data to use and set a default
            List<ClusterData> data = logic.lastScan;
            String dataString = new TextComponentTranslation("info.data.no_data").getFormattedText();

            // if data is present, begin converting it, incrementing index every 5 seconds
            if (data != null && !data.isEmpty()) {
                int clusterIdx = ((int) tickCounter / 100) % data.size();
                ClusterData targetData = data.get(clusterIdx);
                dataString = targetData.toString();
            }

            // add text
            ITextComponent scanResults = TextComponentUtil.stringWithColor(TextFormatting.AQUA, dataString);
            tl.add(scanResults);
        });
    }

    private void addWriteInfoText(List<ITextComponent> textList) {
        if (tryWrite) {
            textList.add(TextComponentUtil.translationWithColor(TextFormatting.GREEN, "info.data.try_write"));
        } else {
            textList.add(TextComponentUtil.translationWithColor(TextFormatting.RED, "info.data.not_try_write"));
        }
    }

    @Override
    protected ModularUI.Builder createUITemplate(EntityPlayer entityPlayer) {
        ModularUI.Builder builder = ModularUI
                .builder(GuiTextures.BORDERED_BACKGROUND, 256/*176*/, 208);
        builder.shouldColor(false);
        builder.image(4, 4, 248, 117, GuiTextures.DISPLAY);
        builder.label(9, 9, getMetaFullName(), 0xFFFFFF);
        builder.widget(new ClickButtonWidget(9, 96, 60, 20, "SCAN", this::onScanClick));
        builder.widget(new ClickButtonWidget(71, 96, 80, 20, "WRITE TOGGLE", this::onWriteToggleClick));
        builder.widget(new AdvancedTextWidget(155, 106, this::addWriteInfoText, 0xFFFFFF));
        builder.widget(new AdvancedTextWidget(9, 20, this::addDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(162)
                .setClickHandler(this::handleDisplayClick));
        builder.widget(new IndicatorImageWidget(232, 101, 17, 17, getLogo())
                .setWarningStatus(getWarningLogo(), this::addWarningText)
                .setErrorStatus(getErrorLogo(), this::addErrorText));
        builder.bindPlayerInventory(entityPlayer.inventory,
                GuiTextures.SLOT, 47, 125);
        return builder;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        BlockPos p = getPos();
        AxisAlignedBB bb = new AxisAlignedBB(
                p.getX() - 10, p.getY(),
                p.getZ() - 10,
                p.getX() + 10, p.getY() + 1,
                p.getZ() + 10
        );
        return bb;
    }

    @Override
    public Vec3d getTransform() {
        return switch (this.getFrontFacing()) {
            case WEST ->
                    new Vec3d(4, 10, 0);
            case EAST ->
                    new Vec3d(-4, 10, 0);
            case NORTH ->
                    new Vec3d(0, 10, -3);
            case SOUTH ->
                    new Vec3d(0, 10, 4);
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
        return new ArrayList<>();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        logic.writeToNBT(data);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        logic.readFromNBT(data);
    }


    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        getFrontOverlay().renderSided(getFrontFacing(), renderState, translation, pipeline);
    }
}
