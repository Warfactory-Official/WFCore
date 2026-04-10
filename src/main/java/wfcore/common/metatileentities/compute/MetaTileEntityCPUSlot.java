package wfcore.common.metatileentities.compute;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.items.itemhandlers.GTItemStackHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityMultiblockPart;
import lombok.SneakyThrows;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wfcore.client.render.WFTextures;
import wfcore.common.items.registry.CPURegistry;
import wfcore.common.metatileentities.WFCoreAbilities;
import wfcore.common.metatileentities.multi.electric.MetaTileEntityMainframe;

import javax.annotation.Nonnull;
import java.util.List;

public class MetaTileEntityCPUSlot extends MetaTileEntityMultiblockPart implements ICpuSlot, IMultiblockAbilityPart<ICpuSlot> {

    private static final int SYNC_TEXTURE = 8456;
    private final GTItemStackHandler inventory;


    public MetaTileEntityCPUSlot(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, tier);
        this.inventory = new GTItemStackHandler(this, 1) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                writeCustomData(SYNC_TEXTURE, buf -> buf.writeItemStack(this.getStackInSlot(0)));
                if (getController() instanceof MetaTileEntityMainframe mainframe)
                    mainframe.getGpcHandler().rebuild();
            }


            @Override
            protected int getStackLimit(int slot, @NotNull ItemStack stack) {
                return 1;
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return CPURegistry.isCPU(stack);
            }
        };
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityCPUSlot(metaTileEntityId, getTier());
    }


    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("wfcore.machine.cpu_slot.tooltip"));
    }

    @Override
    public void addToolUsages(ItemStack stack, @Nullable World world, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gregtech.tool_action.screwdriver.access_covers"));
        tooltip.add(I18n.format("gregtech.tool_action.wrench.set_facing"));
        super.addToolUsages(stack, world, tooltip, advanced);
    }

    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        int rowSize = (int) Math.sqrt(this.inventory.getSlots());
        return createUITemplate(entityPlayer, rowSize, rowSize == 10 ? 9 : 0)
                .build(getHolder(), entityPlayer);
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        if (this.getStats() != null)
            WFTextures.OVERLAY_CPU_SLOT_FILLED.renderSided(getFrontFacing(), renderState, translation, pipeline);
        else
            WFTextures.OVERLAY_CPU_SLOT.renderSided(getFrontFacing(), renderState, translation, pipeline);
    }

    private ModularUI.Builder createUITemplate(EntityPlayer player, int rowSize, int xOffset) {
        ModularUI.Builder builder = ModularUI.builder(GuiTextures.BACKGROUND, 176 + xOffset * 2,
                        18 + 18 * rowSize + 94)
                .label(10, 5, getMetaFullName());

        for (int y = 0; y < rowSize; y++) {
            for (int x = 0; x < rowSize; x++) {
                int index = y * rowSize + x;
                builder.widget(new SlotWidget(inventory, index,
                        (88 - rowSize * 9 + x * 18) + xOffset, 18 + y * 18, true, true)
                        .setBackgroundTexture(GuiTextures.SLOT));
            }
        }
        return builder.bindPlayerInventory(player.inventory, GuiTextures.SLOT, 7 + xOffset, 18 + 18 * rowSize + 12);
    }

    @SneakyThrows
    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == SYNC_TEXTURE)
            this.inventory.setStackInSlot(0, buf.readItemStack());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setTag("CpuSlot", inventory.serializeNBT());
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.inventory.deserializeNBT(data.getCompoundTag("CpuSlot"));
    }

    @Override
    public MultiblockAbility<ICpuSlot> getAbility() {
        return WFCoreAbilities.GPC_CPU_SLOT;
    }

    @Override
    public void registerAbilities(List<ICpuSlot> abilityList) {
        abilityList.add(this);

    }

    @Override
    public @Nullable CPURegistry.CPUEntry getStats() {
        if (CPURegistry.isCPU(inventory.getStackInSlot(0)))
            return CPURegistry.getEntry(inventory.getStackInSlot(0));
        else return null;
    }

    @Override
    public double getEstimatedCWU(int power, double temp) {
        CPURegistry.CPUEntry stats = getStats();
        if (stats == null) return 0;
        return stats.getCWU(power, temp);
    }
}
