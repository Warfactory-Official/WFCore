package wfcore.common.pipenet.ac.tile;

import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.pipenet.tile.TileEntityPipeBase;
import gregtech.api.util.TaskScheduler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wfcore.api.capability.IACEnergyContainer;
import wfcore.common.capability.WFCapabilities;
import wfcore.common.pipenet.ac.ACPipeProperties;
import wfcore.common.pipenet.ac.ACPipeType;
import wfcore.common.pipenet.ac.net.ACNetHandler;
import wfcore.common.pipenet.ac.net.ACPipeNet;
import wfcore.common.pipenet.ac.net.WorldACPipeNet;

import java.lang.ref.WeakReference;
import java.util.EnumMap;

public class TileEntityACPipe extends TileEntityPipeBase<ACPipeType, ACPipeProperties> {

    private static final int AC_ACTIVE = 27001;

    private final EnumMap<EnumFacing, ACNetHandler> handlers = new EnumMap<>(EnumFacing.class);
    private WeakReference<ACPipeNet> currentPipeNet = new WeakReference<>(null);
    private ACNetHandler defaultHandler;

    private int ticksActive = 0;
    private int activeDuration = 0;
    private boolean isActive = false;

    @Override
    public Class<ACPipeType> getPipeTypeClass() {
        return ACPipeType.class;
    }

    @Override
    public boolean supportsTicking() {
        return false;
    }

    @Override
    public boolean canHaveBlockedFaces() {
        return false;
    }

    private void initHandlers() {
        ACPipeNet net = getACPipeNet();
        if (net == null) return;
        for (EnumFacing facing : EnumFacing.VALUES) {
            handlers.put(facing, new ACNetHandler(net, this, facing));
        }
        defaultHandler = new ACNetHandler(net, this, null);
    }

    @Override
    public <T> T getCapabilityInternal(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == WFCapabilities.CAPABILITY_AC_ENERGY) {
            if (world.isRemote) {
                return WFCapabilities.CAPABILITY_AC_ENERGY.cast(IACEnergyContainer.DEFAULT);
            }
            if (handlers.isEmpty()) {
                initHandlers();
            }
            checkNetwork();
            return WFCapabilities.CAPABILITY_AC_ENERGY.cast(handlers.getOrDefault(facing, defaultHandler));
        }
        return super.getCapabilityInternal(capability, facing);
    }

    public void checkNetwork() {
        if (defaultHandler != null) {
            ACPipeNet current = getACPipeNet();
            if (defaultHandler.getNet() != current) {
                defaultHandler.updateNetwork(current);
                for (ACNetHandler handler : handlers.values()) {
                    handler.updateNetwork(current);
                }
            }
        }
    }

    public ACPipeNet getACPipeNet() {
        if (world == null || world.isRemote) {
            return null;
        }
        ACPipeNet currentPipeNet = this.currentPipeNet.get();
        if (currentPipeNet != null && currentPipeNet.isValid() && currentPipeNet.containsNode(getPipePos())) {
            return currentPipeNet;
        }
        WorldACPipeNet worldNet = (WorldACPipeNet) getPipeBlock().getWorldPipeNet(getPipeWorld());
        currentPipeNet = worldNet.getNetFromPos(getPipePos());
        if (currentPipeNet != null) {
            this.currentPipeNet = new WeakReference<>(currentPipeNet);
        }
        return currentPipeNet;
    }

    @Override
    public void transferDataFrom(IPipeTile<ACPipeType, ACPipeProperties> tileEntity) {
        super.transferDataFrom(tileEntity);
        if (getACPipeNet() == null) return;
        TileEntityACPipe pipe = (TileEntityACPipe) tileEntity;
        if (!pipe.handlers.isEmpty() && pipe.defaultHandler != null) {
            handlers.clear();
            handlers.putAll(pipe.handlers);
            defaultHandler = pipe.defaultHandler;
        } else {
            initHandlers();
        }
    }

    @Override
    public void setConnection(EnumFacing side, boolean connected, boolean fromNeighbor) {
        // point-to-point: a cable may only connect along a single straight line (like laser pipes)
        if (!getWorld().isRemote && connected && !fromNeighbor) {
            int connections = getConnections();
            connections &= ~(1 << side.getIndex());
            connections &= ~(1 << side.getOpposite().getIndex());
            if (connections != 0) return;

            TileEntity tile = getWorld().getTileEntity(getPos().offset(side));
            if (tile instanceof IPipeTile<?, ?> pipeTile &&
                    pipeTile.getPipeType().getClass() == this.getPipeType().getClass()) {
                connections = pipeTile.getConnections();
                connections &= ~(1 << side.getIndex());
                connections &= ~(1 << side.getOpposite().getIndex());
                if (connections != 0) return;
            }
        }
        super.setConnection(side, connected, fromNeighbor);
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active, int duration) {
        if (this.isActive != active) {
            this.isActive = active;
            notifyBlockUpdate();
            markDirty();
            writeCustomData(AC_ACTIVE, buf -> buf.writeBoolean(this.isActive));
            if (active && duration != this.activeDuration) {
                TaskScheduler.scheduleTask(getWorld(), this::queueDisconnect);
            }
        }
        this.activeDuration = duration;
        if (duration > 0 && active) {
            this.ticksActive = 0;
        }
    }

    public boolean queueDisconnect() {
        if (activeDuration <= 0) return false;
        if (++this.ticksActive % activeDuration == 0) {
            this.ticksActive = 0;
            setActive(false, -1);
            return false;
        }
        return true;
    }

    @Override
    public void receiveCustomData(int discriminator, PacketBuffer buf) {
        super.receiveCustomData(discriminator, buf);
        if (discriminator == AC_ACTIVE) {
            this.isActive = buf.readBoolean();
            scheduleChunkForRenderUpdate();
        }
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeBoolean(this.isActive);
        if (isActive) {
            activeDuration = 100;
            TaskScheduler.scheduleTask(getWorld(), this::queueDisconnect);
        }
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.isActive = buf.readBoolean();
        scheduleChunkForRenderUpdate();
    }

    @NotNull
    @Override
    public NBTTagCompound writeToNBT(@NotNull NBTTagCompound compound) {
        compound.setBoolean("Active", isActive);
        return super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Active", Constants.NBT.TAG_BYTE)) {
            isActive = compound.getBoolean("Active");
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        this.handlers.clear();
    }
}
