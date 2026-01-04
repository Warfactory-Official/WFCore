package wfcore.common.metatileentities.multi.electric;

import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.pattern.PatternMatchContext;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.util.RelativeDirection;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.MetaBlocks;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import wfcore.api.capability.data.IData;
import wfcore.client.render.WFTextures;

import java.util.UUID;

// TODO: make explicit components placed into controller to make computer work, rather than recipes
// This could be a lot cooler if I had more time
public class MetaTileEntityComputer extends RecipeMapMultiblockController {
    private long tickCounter = 0;
    private final UUID computerId;
    private UUID currentHostId;

    private int targetSlotId = -1;
    private float targetProgress = 0;

    protected IItemHandlerModifiable inputInventory;
    protected IMultipleTankHandler inputFluidInventory;
    protected IEnergyContainer energyContainer;

    private static Object2ObjectOpenHashMap<UUID, MetaTileEntityComputer> HOST_MAP = new Object2ObjectOpenHashMap<>();

    public MetaTileEntityComputer(ResourceLocation metaTileEntityId, RecipeMap<?> recipeMap) {
        super(metaTileEntityId, recipeMap);
        computerId = UUID.randomUUID();
    }

    @Override
    protected @NotNull BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                .aisle("IV", "CE", "F ")
                .where('C', selfPredicate())
                .where('I', abilities(MultiblockAbility.IMPORT_ITEMS))
                .where('E', abilities(MultiblockAbility.INPUT_ENERGY))
                .where('F', abilities(MultiblockAbility.IMPORT_FLUIDS))
                .where('S', states(MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.STEEL_SOLID)))
                .where('V', states(MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.STEEL_PIPE)))
                .where(' ', air())
                .build();
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return Textures.COMPUTER_CASING;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityRadar(this.metaTileEntityId);
    }

    @NotNull
    @Override
    protected ICubeRenderer getFrontOverlay() {
        return WFTextures.OVERLAY_COMPUTER;
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        initializeAbilities();
    }

    protected void initializeAbilities() {
        this.inputInventory = new ItemHandlerList(getAbilities(MultiblockAbility.IMPORT_ITEMS));
        this.inputFluidInventory = new FluidTankList(true,
                getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        this.energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
    }

    public UUID getHostId() {
        return currentHostId;
    }

    public boolean hasTarget() {
        return hasLocalTarget() || currentHostId != null;
    }

    // TODO: cache host target status if necessary
    public boolean hasHostTarget() {
        return currentHostId != null && HOST_MAP.containsKey(currentHostId) && HOST_MAP.get(currentHostId).hasLocalTarget();
    }

    public boolean hasLocalTarget() {
        return inputInventory != null && targetSlotId > 0 && inputInventory.getSlots() > targetSlotId && isTargetValidData();
    }

    // assumes has target data is true
    private boolean isTargetValidData() {
        var targetStack = inputInventory.getStackInSlot(targetSlotId);
        return targetStack.getItem() instanceof IData;
    }

    public void invalidateTarget() {
        targetProgress = 0;
        targetSlotId = -1;
        this.recipeMapWorkable.setWorkingEnabled(false);
    }

    @Override
    public void update() {
        if (getWorld().isRemote) { return; }

        // update tick counter
        ++tickCounter;
        tickCounter &= 1L << 63;

        // only update once a second
        if (tickCounter % 20 != 0) { return; }

        // if we are running a recipe, check if the targeted data object is present
        if (isActive()) {
            // target missing, reset
            if (!hasTarget()) {

            }
        }
    }

}
