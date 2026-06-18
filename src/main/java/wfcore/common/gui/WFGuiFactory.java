package wfcore.common.gui;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.AbstractUIFactory;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.factory.PosGuiData;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * ModularUI 3.x factory that opens panels for {@link MetaTileEntity}s implementing {@link IGuiHolder}.
 * GregTech's TileEntity is the holder, not the MTE itself, so the built-in {@code TileEntityGuiFactory}
 * can't be used directly; this resolves the MTE off the holder.
 */
public final class WFGuiFactory extends AbstractUIFactory<PosGuiData> {

    public static final WFGuiFactory INSTANCE = new WFGuiFactory();

    private WFGuiFactory() {
        super("wfcore:mte");
    }

    public static <T extends MetaTileEntity & IGuiHolder<PosGuiData>> void open(EntityPlayer player, T mte) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(mte);
        if (!mte.isValid()) throw new IllegalArgumentException("Can't open invalid MetaTileEntity GUI!");
        if (player.world != mte.getWorld()) {
            throw new IllegalArgumentException("MetaTileEntity must share the player's dimension!");
        }
        BlockPos pos = mte.getPos();
        PosGuiData data = new PosGuiData(player, pos.getX(), pos.getY(), pos.getZ());
        GuiManager.open(INSTANCE, data, verifyServerSide(player));
    }

    @Override
    public @NotNull IGuiHolder<PosGuiData> getGuiHolder(PosGuiData data) {
        TileEntity te = data.getTileEntity();
        if (te instanceof IGregTechTileEntity gtte) {
            MetaTileEntity mte = gtte.getMetaTileEntity();
            return Objects.requireNonNull(castGuiHolder(mte), "MetaTileEntity is not a ModularUI gui holder!");
        }
        throw new IllegalStateException("TileEntity is not a MetaTileEntity holder!");
    }

    @Override
    public boolean canInteractWith(EntityPlayer player, PosGuiData guiData) {
        return player == guiData.getPlayer()
                && guiData.getTileEntity() instanceof IGregTechTileEntity
                && guiData.getSquaredDistance(player) <= 64;
    }

    @Override
    public void writeGuiData(PosGuiData guiData, PacketBuffer buffer) {
        buffer.writeVarInt(guiData.getX());
        buffer.writeVarInt(guiData.getY());
        buffer.writeVarInt(guiData.getZ());
    }

    @Override
    public @NotNull PosGuiData readGuiData(EntityPlayer player, PacketBuffer buffer) {
        return new PosGuiData(player, buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }
}
