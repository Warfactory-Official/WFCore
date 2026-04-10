package wfcore.common.metatileentities.multi.electric;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.Fluids;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IOpticalComputationProvider;
import gregtech.api.gui.ModularUI;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.common.blocks.BlockComputerCasing;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import wfcore.common.blocks.BlockMetalSheetCasing;
import wfcore.common.blocks.BlockRegistry;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * This class contains the mte of the General Purpose Computer.
 * A GPComputer is a multiblock computer that provides computing units.
 * Must be cooled down with water, NTM Coolant or NTM Cold Perfluoromethyl.
 */
public class MetaTileEntityGPComputer extends RecipeMapMultiblockController
    implements IOpticalComputationProvider {


    



}
