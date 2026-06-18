package wfcore.common.metatileentities.electric;

import gregtech.api.GTValues;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.items.itemhandlers.GTItemStackHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.TieredMetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandlerModifiable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wfcore.api.radar.RadarBookFactory;
import wfcore.api.util.math.ClusterData;
import wfcore.common.items.registry.DataHolderRegistry;
import wfcore.common.managers.RadarSavedData;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

/**
 * Single-block printer: consumes paper + a radar data stick and EU, printing a written book listing
 * the bases recorded on the data stick (ranked by richness and player count, with estimated centers).
 */
public class MetaTileEntityPrinter extends TieredMetaTileEntity {

    public static final int MAX_PROGRESS = 200;

    private final long energyPerTick;
    private int progress;

    public MetaTileEntityPrinter(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, tier);
        this.energyPerTick = GTValues.VA[tier];
        initializeInventory();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityPrinter(metaTileEntityId, getTier());
    }

    @Override
    protected IItemHandlerModifiable createImportItemHandler() {
        return new GTItemStackHandler(this, 2) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return slot == 0 ? stack.getItem() == Items.PAPER : DataHolderRegistry.isAllowed(stack);
            }
        };
    }

    @Override
    protected IItemHandlerModifiable createExportItemHandler() {
        return new GTItemStackHandler(this, 1);
    }

    @Override
    public void update() {
        super.update();
        if (getWorld().isRemote) return;

        ItemStack paper = importItems.getStackInSlot(0);
        ItemStack data = importItems.getStackInSlot(1);
        if (paper.isEmpty() || !DataHolderRegistry.isAllowed(data) || !hasScan(data)
                || !exportItems.getStackInSlot(0).isEmpty()) {
            progress = 0;
        } else if (energyContainer.getEnergyStored() >= energyPerTick) {
            energyContainer.removeEnergy(energyPerTick);
            if (++progress >= MAX_PROGRESS) {
                progress = 0;
                ItemStack book = buildBook(data);
                if (!book.isEmpty()) {
                    exportItems.setStackInSlot(0, book);
                    paper.shrink(1);
                }
            }
        }

        if (getOffsetTimer() % 5 == 0) {
            pushItemsIntoNearbyHandlers(getFrontFacing());
        }
    }

    private static boolean hasScan(ItemStack data) {
        return data.hasTagCompound() && data.getTagCompound().hasUniqueId("TargetUUID");
    }

    @NotNull
    private ItemStack buildBook(ItemStack data) {
        UUID uuid = data.getTagCompound().getUniqueId("TargetUUID");
        List<ClusterData> clusters = RadarSavedData.get().getScan(uuid);
        if (clusters == null) {
            return ItemStack.EMPTY;
        }
        return RadarBookFactory.createReport(clusters);
    }

    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        return ModularUI.builder(GuiTextures.BACKGROUND, 176, 166)
                .label(10, 5, getMetaFullName())
                .widget(new SlotWidget(importItems, 0, 52, 30, true, true).setBackgroundTexture(GuiTextures.SLOT))
                .widget(new SlotWidget(importItems, 1, 52, 52, true, true).setBackgroundTexture(GuiTextures.SLOT))
                .widget(new SlotWidget(exportItems, 0, 107, 41, true, false).setBackgroundTexture(GuiTextures.SLOT))
                .bindPlayerInventory(entityPlayer.inventory, GuiTextures.SLOT, 7, 84)
                .build(getHolder(), entityPlayer);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, @NotNull List<String> tooltip,
                               boolean advanced) {
        tooltip.add(I18n.format("wfcore.machine.printer.tooltip"));
        tooltip.add(I18n.format("gregtech.universal.tooltip.voltage_in", energyContainer.getInputVoltage(),
                GTValues.VNF[getTier()]));
        super.addInformation(stack, player, tooltip, advanced);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("PrintProgress", progress);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.progress = data.getInteger("PrintProgress");
    }
}
