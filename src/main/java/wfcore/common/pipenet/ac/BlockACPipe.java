package wfcore.common.pipenet.ac;

import gregtech.api.items.toolitem.ToolClasses;
import gregtech.api.items.toolitem.ToolHelper;
import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.pipenet.tile.TileEntityPipeBase;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wfcore.api.block.ICustomBlockItem;
import wfcore.common.capability.WFCapabilities;
import wfcore.common.pipenet.ac.net.WorldACPipeNet;
import wfcore.common.pipenet.ac.tile.TileEntityACPipe;

import java.util.function.Function;

/** AC cable block (one per {@link ACPipeType} thickness). Steel-wire base carries 512 EU/t at SINGLE. */
public class BlockACPipe extends BlockPipe<ACPipeType, ACPipeProperties, WorldACPipeNet> implements ICustomBlockItem {

    private static final long STEEL_BASE_THROUGHPUT = 512L;

    private final ACPipeType pipeType;
    private final ACPipeProperties properties;

    public BlockACPipe(@NotNull ACPipeType pipeType) {
        this.pipeType = pipeType;
        this.properties = new ACPipeProperties(STEEL_BASE_THROUGHPUT);
        setCreativeTab(gregtech.api.GregTechAPI.TAB_GREGTECH_CABLES);
        setHarvestLevel(ToolClasses.WIRE_CUTTER, 1);
    }

    @Override
    public Class<ACPipeType> getPipeTypeClass() {
        return ACPipeType.class;
    }

    @Override
    public WorldACPipeNet getWorldPipeNet(World world) {
        return WorldACPipeNet.getWorldPipeNet(world);
    }

    @Override
    public TileEntityPipeBase<ACPipeType, ACPipeProperties> createNewTileEntity(boolean supportsTicking) {
        return new TileEntityACPipe();
    }

    @Override
    public ACPipeProperties createProperties(IPipeTile<ACPipeType, ACPipeProperties> pipeTile) {
        ACPipeType type = pipeTile.getPipeType();
        if (type == null) return getFallbackType();
        return type.modifyProperties(properties);
    }

    @Override
    public ACPipeProperties createItemProperties(ItemStack itemStack) {
        return pipeType.modifyProperties(properties);
    }

    @Override
    public ItemStack getDropItem(IPipeTile<ACPipeType, ACPipeProperties> pipeTile) {
        return new ItemStack(this, 1, pipeType.ordinal());
    }

    @Override
    protected ACPipeProperties getFallbackType() {
        return properties;
    }

    @Override
    public ACPipeType getItemPipeType(ItemStack itemStack) {
        return pipeType;
    }

    @Override
    public void setTileEntityData(TileEntityPipeBase<ACPipeType, ACPipeProperties> pipeTile, ItemStack itemStack) {
        pipeTile.setPipeData(this, pipeType);
    }

    @Override
    public void getSubBlocks(@NotNull CreativeTabs itemIn, @NotNull NonNullList<ItemStack> items) {
        items.add(new ItemStack(this, 1, pipeType.ordinal()));
    }

    @Override
    protected boolean isPipeTool(@NotNull ItemStack stack) {
        return ToolHelper.isTool(stack, ToolClasses.WIRE_CUTTER);
    }

    @Override
    public boolean canPipesConnect(IPipeTile<ACPipeType, ACPipeProperties> selfTile, EnumFacing side,
                                   IPipeTile<ACPipeType, ACPipeProperties> sideTile) {
        return selfTile instanceof TileEntityACPipe && sideTile instanceof TileEntityACPipe;
    }

    @Override
    public boolean canPipeConnectToBlock(IPipeTile<ACPipeType, ACPipeProperties> selfTile, EnumFacing side,
                                         @Nullable TileEntity tile) {
        return tile != null &&
                tile.getCapability(WFCapabilities.CAPABILITY_AC_ENERGY, side.getOpposite()) != null;
    }

    @Override
    public boolean isHoldingPipe(EntityPlayer player) {
        if (player == null) return false;
        ItemStack stack = player.getHeldItemMainhand();
        return stack != ItemStack.EMPTY && stack.getItem() instanceof ItemBlockACPipe;
    }

    @Override
    public <T extends Block> Function<T, ItemBlock> getItemBlock() {
        return block -> new ItemBlockACPipe(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected Pair<TextureAtlasSprite, Integer> getParticleTexture(World world, BlockPos blockPos) {
        TileEntity te = world.getTileEntity(blockPos);
        if (te instanceof IPipeTile<?, ?> pipeTile) {
            return wfcore.client.render.ACPipeRenderer.INSTANCE.getParticleTexture(pipeTile);
        }
        return Pair.of(wfcore.client.render.ACPipeRenderer.INSTANCE.getParticleTexture(), 0xFFFFFF);
    }

    @Override
    @NotNull
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("deprecation")
    public EnumBlockRenderType getRenderType(@NotNull IBlockState state) {
        return wfcore.client.render.ACPipeRenderer.INSTANCE.getBlockRenderType();
    }

    @Override
    public boolean canRenderInLayer(@NotNull IBlockState state, @NotNull BlockRenderLayer layer) {
        return layer == BlockRenderLayer.SOLID || layer == BlockRenderLayer.CUTOUT;
    }
}
