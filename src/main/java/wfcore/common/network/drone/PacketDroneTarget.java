package wfcore.common.network.drone;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import wfcore.common.drones.EntitySuicideDrone;

import java.util.Objects;

public class PacketDroneTarget implements IMessage {

    // Attributes
    public int entityId;
    public double targetX, targetY, targetZ;

    // Constructors
    public PacketDroneTarget() {}
    public PacketDroneTarget(int entityId, double targetX, double targetY, double targetZ) {
        this.entityId = entityId;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        targetX = buf.readDouble();
        targetY = buf.readDouble();
        targetZ = buf.readDouble();
    }
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeDouble(targetX);
        buf.writeDouble(targetY);
        buf.writeDouble(targetZ);
    }

    // Handler
    public static class Handler implements IMessageHandler<PacketDroneTarget, IMessage> {
        @Override
        public IMessage onMessage(PacketDroneTarget msg, MessageContext ctx) {
            EntityPlayer player = ctx.getServerHandler().player;
            Objects.requireNonNull(player.getServer()).addScheduledTask(() -> {
                Entity e = player.world.getEntityByID(msg.entityId);
                if (e instanceof EntitySuicideDrone drone) {
                    drone.setTarget(msg.targetX, msg.targetY, msg.targetZ);
                }
            });
            return null;
        }
    }
}
