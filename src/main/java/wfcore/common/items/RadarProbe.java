package wfcore.common.items;

import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.world.IWorldNameable;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import wfcore.api.radar.MultiblockRadarLogic;
import wfcore.api.radar.RadarTargetIdentifier;

import javax.annotation.Nullable;
import java.util.List;

public class RadarProbe extends BaseItem {
    public RadarProbe(String s, String texturePath) {
        super(s, texturePath);
    }

    // called on any right click
    /*
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack heldStack = playerIn.getHeldItem(handIn);
        if (!worldIn.isRemote) { return new ActionResult<>(EnumActionResult.PASS, heldStack); }
        RayTraceResult raytraceresult = this.rayTrace(worldIn, playerIn, false);

        // intellij doesn't know what it's talking about; this can definitely occur
        if (raytraceresult == null) {
            playerIn.sendMessage(new TextComponentString("Got a null raytrace; returning").setStyle(new Style().setColor(TextFormatting.RED)));
            return new ActionResult<>(EnumActionResult.FAIL, heldStack);
        }

        // this method does not handle anything other than entity hits
        if (raytraceresult.typeOfHit != RayTraceResult.Type.ENTITY) {
            playerIn.sendMessage(new TextComponentString("Got a type of hit which was not an entity: " + raytraceresult.typeOfHit).setStyle(new Style().setColor(TextFormatting.RED)));
            return new ActionResult<>(EnumActionResult.PASS, heldStack);
        }

        // get the entity key
        ResourceLocation entityKey = EntityList.getKey(raytraceresult.entityHit);
        if (entityKey == null) { entityKey = new ResourceLocation("NULL"); }

        // get the entity string
        String entityString = EntityList.getEntityString(raytraceresult.entityHit);
        if (entityString == null) { entityString = "NULL"; }

        String entityTranslationKey = EntityList.getTranslationName(entityKey);

        ITextComponent translatedName = new TextComponentString("NULL");
        if (entityTranslationKey != null) { translatedName = new TextComponentTranslation(entityTranslationKey); }
        else { entityTranslationKey = "NULL"; }

        // send the display name
        playerIn.sendMessage(new TextComponentString("\nTarget entity's display name is:"));
        playerIn.sendMessage(translatedName.setStyle(new Style().setColor(TextFormatting.AQUA)));
        playerIn.sendMessage(new TextComponentString("\nTarget entity's string is:"));
        playerIn.sendMessage(new TextComponentString(entityString).setStyle(new Style().setColor(TextFormatting.AQUA)));
        playerIn.sendMessage(new TextComponentString("\nTarget entity's translation key is:"));
        playerIn.sendMessage(new TextComponentString(entityTranslationKey).setStyle(new Style().setColor(TextFormatting.AQUA)));

        // new line
        playerIn.sendMessage(new TextComponentString(""));

        return new ActionResult<>(EnumActionResult.SUCCESS, heldStack);
    } */

    // called when an entity is clicked (presumably something extending entitylivingbase only?)
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target, EnumHand hand) {
        // get the entity key
        ResourceLocation entityKey = EntityList.getKey(target);
        if (entityKey == null) { entityKey = new ResourceLocation("NULL"); }

        // get the entity string
        String entityString = EntityList.getEntityString(target);
        if (entityString == null) { entityString = "NULL"; }

        String entityTranslationKey = EntityList.getTranslationName(entityKey);

        ITextComponent translatedName = new TextComponentString("NULL");
        if (entityTranslationKey != null) { translatedName = new TextComponentTranslation(entityTranslationKey); }
        else { entityTranslationKey = "NULL"; }

        // send the display name
        player.sendMessage(new TextComponentString("\nTarget entity's display name is:"));
        player.sendMessage(translatedName.setStyle(new Style().setColor(TextFormatting.AQUA)));
        player.sendMessage(new TextComponentString("\nTarget entity's resource location is:"));
        player.sendMessage(new TextComponentString(entityKey.toString()).setStyle(new Style().setColor(TextFormatting.AQUA)));
        player.sendMessage(new TextComponentString("\nTarget entity's string is:"));
        player.sendMessage(new TextComponentString(entityString).setStyle(new Style().setColor(TextFormatting.AQUA)));
        player.sendMessage(new TextComponentString("\nTarget entity's translation key is:"));
        player.sendMessage(new TextComponentString(entityTranslationKey).setStyle(new Style().setColor(TextFormatting.AQUA)));

        // new line
        player.sendMessage(new TextComponentString(""));
        player.sendMessage(new TextComponentString("Radar Target Identifier result: "));
        player.sendMessage(new TextComponentString(RadarTargetIdentifier.getBestIdentifier(target).toString()).setStyle(new Style().setColor(TextFormatting.BLUE)));
        player.sendMessage(new TextComponentString(""));

        return true;
    }

    // called when clicking on a block
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (!world.isRemote) { return EnumActionResult.PASS; }

        // get the targeted blockstate and check if a tile entity could be associated; if not, indicate and return
        IBlockState targState = world.getBlockState(pos);
        TileEntity targTE = world.getTileEntity(pos);
        if (targTE == null) {
            player.sendMessage(new TextComponentString("Target <" + targState + "> does not have an associated TE").setStyle(new Style().setColor(TextFormatting.RED)));
            return EnumActionResult.PASS;
        }

        String teResource = TileEntity.getKey(targTE.getClass()).toString();
        String gtMeta = "NULL";
        if(targTE instanceof IGregTechTileEntity mte)
            gtMeta = mte.getMetaTileEntity().metaTileEntityId.toString();

        String block = ForgeRegistries.BLOCKS.getKey(targState.getBlock()).toString();
        boolean isSharedID = teResource.equals(block);
        boolean isWhitelisted = MultiblockRadarLogic.isOnTEWhitelist(targTE);

        // send the display name
        // Priority 1: GregTech MTE Check
        if (targTE instanceof IGregTechTileEntity ) {
            player.sendMessage(new TextComponentString("\nTarget is a: ").setStyle(new Style().setColor(TextFormatting.GOLD))
                    .appendSibling(new TextComponentString("GregTech MetaTileEntity").setStyle(new Style().setColor(TextFormatting.YELLOW))));

            player.sendMessage(new TextComponentString("Meta Tile ID: ").setStyle(new Style().setColor(TextFormatting.GOLD))
                    .appendSibling(new TextComponentString(gtMeta).setStyle(new Style().setColor(TextFormatting.WHITE))));
        } else {
            player.sendMessage(new TextComponentString("\nTarget is a: ").setStyle(new Style().setColor(TextFormatting.DARK_AQUA))
                    .appendSibling(new TextComponentString("Standard TileEntity").setStyle(new Style().setColor(TextFormatting.AQUA))));
        }

        // Priority 2: TE Registry Name
        player.sendMessage(new TextComponentString("Target's TE's resource location is:").setStyle(new Style().setColor(TextFormatting.GRAY)));
        player.sendMessage(new TextComponentString(teResource).setStyle(new Style().setColor(TextFormatting.AQUA)));

        // Priority 3: Block Registry Name
        player.sendMessage(new TextComponentString("Target's Block ID is:").setStyle(new Style().setColor(TextFormatting.GRAY)));
        player.sendMessage(new TextComponentString(block).setStyle(new Style().setColor(TextFormatting.AQUA)));

        if (!isSharedID) {
            player.sendMessage(new TextComponentString("⚠ WARNING: TE/Block ID Mismatch!").setStyle(new Style().setColor(TextFormatting.RED).setBold(true)));
            player.sendMessage(new TextComponentString("Suggestion: Use the 'State' option in config to ensure correct identification.")
                    .setStyle(new Style().setColor(TextFormatting.YELLOW).setItalic(true)));
        }
        player.sendMessage(new TextComponentString(""));
        player.sendMessage(new TextComponentString("Radar Target Identifier result: "));
        player.sendMessage(new TextComponentString(RadarTargetIdentifier.getBestIdentifier(targTE).toString()).setStyle(new Style().setColor(TextFormatting.BLUE)));
        player.sendMessage(new TextComponentString(""));

        if (isWhitelisted) {
            player.sendMessage(new TextComponentString("✔ Already in Config").setStyle(new Style().setColor(TextFormatting.DARK_GREEN)));
        } else {
            player.sendMessage(new TextComponentString("✖ Not in Radar Config").setStyle(new Style().setColor(TextFormatting.RED)));
        }

        return EnumActionResult.SUCCESS;
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(new TextComponentTranslation("info.radar_probe.default").setStyle(
                new Style().setColor(TextFormatting.GRAY)).getFormattedText());

        if (flagIn.isAdvanced()) {
            tooltip.add(new TextComponentTranslation("info.radar_probe.advanced").setStyle(
                    new Style().setColor(TextFormatting.AQUA).setBold(true)).getFormattedText());
        }
    }

}
