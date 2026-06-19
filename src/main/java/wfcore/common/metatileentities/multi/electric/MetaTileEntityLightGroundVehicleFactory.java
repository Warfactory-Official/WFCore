package wfcore.common.metatileentities.multi.electric;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.util.RelativeDirection;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;
import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.MetaBlocks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import com.norwood.mcheli.aircraft.MCH_BoundingBox;
import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.aircraft.MCH_ItemAircraft;
import org.jetbrains.annotations.NotNull;
import wfcore.api.metatileentity.IAnimatedMTE;
import wfcore.common.recipe.WFCoreMachineRecipes;

import java.util.Collection;
import java.util.Collections;

/**
 * MV light ground vehicle factory: spawns an MCHeli vehicle (e.g. the Growler ITV) from the recipe's
 * vehicle item output. Obstruction is tested against the vehicle's MCHeli hitboxes (with a fallback
 * to the entity AABB for definitions that ship no per-part hitboxes).
 */
public class MetaTileEntityLightGroundVehicleFactory extends MetaTileEntityVehicleFactory
                                                     implements IAnimatedMTE {

    public MetaTileEntityLightGroundVehicleFactory(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, WFCoreMachineRecipes.VEHICLE_ASSEMBLER);
    }

    // ---- glTF rendering (McGLTF, via GenericGLTF registered in TERegistry); model authored by hand ----

    @Override
    public Collection<BlockPos> getHiddenBlocks() {
        return Collections.emptyList();
    }

    @Override
    public String getAnimState() {
        return isActive() ? "running" : "idle";
    }

    @Override
    public boolean shouldRender() {
        return isStructureFormed();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(getPos()).grow(8, 6, 8);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityLightGroundVehicleFactory(metaTileEntityId);
    }

    @Override
    protected @NotNull BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                .aisle("KKKKKKKK", "KKKKKKKK", "KKKKKKKK", "KKKSKKKK")
                .aisle("KKKKKKKK", "K      K", "K      K", "KKKKKKKK")
                .aisle("KKKKKKKK", "K      K", "K      K", "KKKKKKKK")
                .aisle("KKKKKKKK", "K      K", "K      K", "KKKKKKKK")
                .aisle("KKKKKKKK", "K      K", "K      K", "KKKKKKKK")
                .aisle("KKKKKKKK", "K      K", "K      K", "KKKKKKKK")
                .aisle("KKKKKKKK", "K      K", "K      K", "KKKKKKKK")
                .aisle("KKKKKKKK", "KKKKKKKK", "KKKKKKKK", "KKKKKKKK")
                .where('S', selfPredicate())
                .where('K', states(getCasingState())
                        .or(abilities(MultiblockAbility.IMPORT_ITEMS).setMaxGlobalLimited(4))
                        .or(abilities(MultiblockAbility.IMPORT_FLUIDS).setMaxGlobalLimited(4))
                        .or(abilities(MultiblockAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(2)))
                .where(' ', any())
                .build();
    }

    private static IBlockState getCasingState() {
        return MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.STEEL_SOLID);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return Textures.SOLID_STEEL_CASING;
    }

    @Override
    protected boolean deploy(@NotNull WorldServer world, @NotNull BlockPos pos, @NotNull ItemStack vehicleItem) {
        if (!(vehicleItem.getItem() instanceof MCH_ItemAircraft itemAircraft)) {
            return false;
        }
        MCH_EntityAircraft aircraft = itemAircraft.createAircraft(world,
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, vehicleItem);
        if (aircraft == null) {
            return false;
        }
        aircraft.initRotationYaw(getFrontFacing().getHorizontalAngle());
        if (!isAreaClear(world, aircraft)) {
            return false;
        }
        return world.spawnEntity(aircraft);
    }

    private boolean isAreaClear(WorldServer world, MCH_EntityAircraft aircraft) {
        aircraft.updateExtraBoundingBox();
        MCH_BoundingBox[] boxes = aircraft.extraBoundingBox;
        if (boxes != null && boxes.length > 0) {
            for (MCH_BoundingBox box : boxes) {
                if (box != null && box.boundingBox != null
                        && !world.getCollisionBoxes(aircraft, box.boundingBox).isEmpty()) {
                    return false;
                }
            }
            return true;
        }
        // Definitions without per-part hitboxes (e.g. the Growler): fall back to the entity AABB.
        return world.getCollisionBoxes(aircraft, aircraft.getEntityBoundingBox()).isEmpty();
    }
}
