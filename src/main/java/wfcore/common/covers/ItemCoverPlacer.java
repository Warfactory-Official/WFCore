package wfcore.common.covers;

import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.cover.Cover;
import gregtech.api.cover.CoverDefinition;
import gregtech.api.cover.CoverHolder;
import gregtech.api.cover.CoverRayTracer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import wfcore.common.items.BaseItem;

/**
 * Plain Forge item that places a {@link CoverCoolingFan} onto a cover-holding machine. Mirrors GregTech's
 * {@code CoverItemBehavior} (which only attaches to GregTech MetaItems) for WFCore's plain item registry.
 */
public class ItemCoverPlacer extends BaseItem {

    private CoverDefinition definition;

    public ItemCoverPlacer(String registryName, String texturePath) {
        super(registryName, texturePath);
    }

    public void setDefinition(CoverDefinition definition) {
        this.definition = definition;
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
                                           float hitX, float hitY, float hitZ, EnumHand hand) {
        if (definition == null) return EnumActionResult.PASS;
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity == null) return EnumActionResult.PASS;

        CoverHolder coverHolder = tileEntity.getCapability(GregtechTileCapabilities.CAPABILITY_COVER_HOLDER, null);
        if (coverHolder == null) return EnumActionResult.PASS;

        EnumFacing coverSide = CoverRayTracer.rayTraceCoverableSide(coverHolder, player);
        if (coverSide == null || coverHolder.hasCover(coverSide)) return EnumActionResult.PASS;

        if (world.isRemote) return EnumActionResult.SUCCESS;

        Cover cover = definition.createCover(coverHolder, coverSide);
        if (!coverHolder.canPlaceCoverOnSide(coverSide) || !cover.canAttach(coverHolder, coverSide)) {
            return EnumActionResult.PASS;
        }

        ItemStack itemStack = player.getHeldItem(hand);
        coverHolder.addCover(coverSide, cover);
        cover.onAttachment(coverHolder, coverSide, player, itemStack);
        if (!player.isCreative()) {
            if (itemStack.isEmpty()) return EnumActionResult.FAIL;
            itemStack.shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }
}
