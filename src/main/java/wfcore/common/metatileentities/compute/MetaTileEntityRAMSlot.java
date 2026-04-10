package wfcore.common.metatileentities.compute;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.AdvancedTextWidget;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.items.itemhandlers.GTItemStackHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.client.renderer.texture.Textures;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityMultiblockPart;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wfcore.common.items.registry.RAMRegistry;
import wfcore.common.metatileentities.WFCoreAbilities;
import wfcore.common.metatileentities.multi.electric.MetaTileEntityMainframe;

import java.util.List;

public class MetaTileEntityRAMSlot extends MetaTileEntityMultiblockPart implements IRamSlot, IMultiblockAbilityPart<IRamSlot> {


    private final GTItemStackHandler inventory;
    private long totalThroughput;

    public MetaTileEntityRAMSlot(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, tier);
        this.inventory = new GTItemStackHandler(this, Math.max(1, tier - 1)) {
            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return RAMRegistry.isRAM(stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                recalculateThroughput();
                if(getController() instanceof MetaTileEntityMainframe mainframe)
                    mainframe.getGpcHandler().rebuild();
            }

            private void recalculateThroughput() {
                int throughput = 0;
                for (ItemStack stack : this.stacks) {
                    if (RAMRegistry.isRAM(stack)) {
                        throughput += RAMRegistry.getEntry(stack).throughput();
                    }
                }
                totalThroughput = throughput;
                if (!getWorld().isRemote) {
                    writeCustomData(950, buf -> buf.writeLong(totalThroughput));
                }
            }

        };
    }

    private void addThroughputText(List<ITextComponent> textList) {
        textList.add(new TextComponentTranslation("wfcore.gui.mainframe.throughput",
                TextFormatting.LIGHT_PURPLE.toString() + totalThroughput));
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityRAMSlot(metaTileEntityId, getTier());
    }

    @Override
    public void update() {
        super.update();
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 950) {
            this.totalThroughput = buf.readLong();
        }
    }

    @Override
    public void clearMachineInventory(NonNullList<ItemStack> itemBuffer) {
        clearInventory(itemBuffer, inventory);
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        if (shouldRenderOverlay())
            Textures.MUFFLER_OVERLAY.renderSided(getFrontFacing(), renderState, translation, pipeline);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("wfcore.machine.ram_slot.tooltip1"));
    }

    @Override
    public void addToolUsages(ItemStack stack, @Nullable World world, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gregtech.tool_action.screwdriver.access_covers"));
        tooltip.add(I18n.format("gregtech.tool_action.wrench.set_facing"));
        super.addToolUsages(stack, world, tooltip, advanced);
    }

    @Override
    public MultiblockAbility<IRamSlot> getAbility() {
        return WFCoreAbilities.GPC_RAM_SLOT;
    }

    @Override
    public void registerAbilities(List<IRamSlot> abilityList) {
        abilityList.add(this);
    }

    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        int rowSize = inventory.getSlots();

        return createUITemplate(entityPlayer, rowSize, rowSize == 10 ? 9 : 0)
                .build(getHolder(), entityPlayer);
    }

    private ModularUI.Builder createUITemplate(EntityPlayer player, int count, int xOffset) {
        int guiWidth = 176 + xOffset * 2;
        int guiHeight = 18 + 18 + 94;

        ModularUI.Builder builder = ModularUI.builder(GuiTextures.BACKGROUND, guiWidth, guiHeight)
                .label(10, 5, getMetaFullName());

        builder.widget(new AdvancedTextWidget(10, 38, this::addThroughputText, 0xFFFFFF));
        int totalSlotsWidth = count * 18;

        int startX = (guiWidth / 2) - (totalSlotsWidth / 2);
        int startY = 18;

        for (int i = 0; i < count; i++) {
            builder.widget(new SlotWidget(inventory, i,
                    startX + (i * 18), startY, true, true)
                    .setBackgroundTexture(GuiTextures.SLOT));
        }

        return builder.bindPlayerInventory(player.inventory, GuiTextures.SLOT,
                (guiWidth / 2) - (9 * 18 / 2), guiHeight - 82);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setLong("throughput", totalThroughput);
        data.setTag("RamInventory", inventory.serializeNBT());
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.totalThroughput = data.getLong("throughput");
        this.inventory.deserializeNBT(data.getCompoundTag("RamInventory"));
    }

    @Override
    public long getTotalThroughput() {
        return totalThroughput;
    }
}
