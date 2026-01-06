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
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import wfcore.api.capability.data.IData;
import wfcore.api.capability.data.IDataStorage;
import wfcore.api.capability.data.NBTFileSys;
import wfcore.client.render.WFTextures;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import static wfcore.common.recipe.WFCoreMachineRecipes.COMPUTER_RECIPE_MAP;

// TODO: make explicit components placed into controller to make computer work, rather than recipes
// This could be a lot cooler if I had more time
public class MetaTileEntityComputer extends RecipeMapMultiblockController {
    private long tickCounter = 0;
    private final UUID computerId;
    private UUID currentHostId;

    private int targetSlotId = -1;
    private float targetProgress = 0;
    private BigInteger accumulatedOps = BigInteger.ZERO;
    private BigInteger cachedOpsReq = null;

    protected IItemHandlerModifiable inputInventory;
    protected IMultipleTankHandler inputFluidInventory;
    protected IEnergyContainer energyContainer;

    private static Object2ObjectOpenHashMap<UUID, MetaTileEntityComputer> COMPUTERS = new Object2ObjectOpenHashMap<>();

    public MetaTileEntityComputer(ResourceLocation metaTileEntityId, RecipeMap<?> recipeMap) {
        super(metaTileEntityId, recipeMap);
        computerId = UUID.randomUUID();
        COMPUTERS.put(computerId, this);
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
        return new MetaTileEntityComputer(this.metaTileEntityId, COMPUTER_RECIPE_MAP);
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
        // we want to act whenever a target is modified
        this.inputInventory = new ItemHandlerList(getAbilities(MultiblockAbility.IMPORT_ITEMS)) {
            @Override
            public void setStackInSlot(int slot, @NotNull ItemStack stack) {
                super.setStackInSlot(slot, stack);
                if (slot == targetSlotId) { invalidateTarget(); }
            }

            @NotNull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                var stack = super.extractItem(slot, amount, simulate);
                if (slot == targetSlotId) { invalidateTarget(); }
                return stack;
            }
        };

        this.inputFluidInventory = new FluidTankList(true,
                getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        this.energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
    }

    public UUID getHostId() {
        return currentHostId;
    }

    public boolean hasHostTarget() {
        return currentHostId != null && COMPUTERS.containsKey(currentHostId) && COMPUTERS.get(currentHostId).hasLocalTarget();
    }

    public boolean hasLocalTarget() {
        if (inputInventory == null || targetSlotId < 0 || inputInventory.getSlots() <= targetSlotId) { return false; }
        return getLocalTarget().getItem() instanceof IDataStorage;
    }

    // assumes has target data is true
    private ItemStack getLocalTarget() {
        return inputInventory.getStackInSlot(targetSlotId);
    }

    public void invalidateTarget() {
        targetProgress = 0;
        accumulatedOps = BigInteger.ZERO;
        targetSlotId = -1;
        this.recipeMapWorkable.setWorkingEnabled(false);
        recipeMapWorkable.invalidate();
    }

    public void initializeTarget() {
        targetProgress = 0;
        accumulatedOps = BigInteger.ZERO;
        this.recipeMapWorkable.setWorkingEnabled(true);
    }

    public void initializeTarget(int targetSlotId) {
        this.currentHostId = null;
        this.targetSlotId = targetSlotId;
        initializeTarget();
    }

    public void initializeTarget(UUID hostId) {
        this.targetSlotId = -1;
        this.currentHostId = hostId;
        initializeTarget();
    }

    public void supplyCompute(BigInteger ops) {
        accumulatedOps = accumulatedOps.add(ops);
    }

    // TODO: let user see filesystem and extra specific data of interest
    public void onDataExtracted(ArrayList<IData> data) {

    }

    @Override
    public void update() {
        super.update();
        if (getWorld().isRemote) { return; }

        // update tick counter
        ++tickCounter;
        tickCounter &= 1L << 63;

        // only update once a second
        if (tickCounter % 20 != 0) { return; }

        // if we have a local target, check if we have enough compute to pull all the data off of it
        if (hasLocalTarget()) {
            if (cachedOpsReq != null) {
                // not enough compute to finish; don't bother checking
                if (accumulatedOps.compareTo(cachedOpsReq) < 0) {
                    return;
                }


            }

            // if we have enough compute to finish, or did not have a value cached, we must check again
            // TODO: don't just target root; take and display paths
            var target = getLocalTarget();
            var targetData = ((IDataStorage) target.getItem()).readData(target, NBTFileSys.ROOT_PATH);
            cachedOpsReq = BigInteger.ZERO;
            for (var data : targetData) {
                cachedOpsReq = cachedOpsReq.add(data.numOpsToExtract());
            }

            // finish the computation
            if (accumulatedOps.compareTo(cachedOpsReq) >= 0) {
                onDataExtracted(targetData);
                invalidateTarget();
            }

            return;
        }

        // if we are running a recipe, or could, leave handling to recipe logic
        if (isActive() || recipeMapWorkable.isWorkingEnabled()) {
            return;
        }

        // if we don't have a recipe, see if the host is ready to receive compute work
        if (hasHostTarget()) {
            initializeTarget(currentHostId);
            return;
        }

        if (inputInventory == null) { return; }

        // the machine can't be not working while having a local target, so we assume there is no local target and search
        for (int slotId = 0; slotId < inputInventory.getSlots(); ++slotId) {
            // check if there is a data item to target
            var slotStack = inputInventory.getStackInSlot(slotId);
            if (!(slotStack.getItem() instanceof IDataStorage dataStorage)) { continue; }

            if (Objects.equals(dataStorage.numBitsTaken(slotStack), BigInteger.ZERO)) { continue; }  // no data to read
        }
    }

}
