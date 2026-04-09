package wfcore.common.network.drone;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import wfcore.common.drones.EntitySuicideDrone;

import java.util.Objects;

public class PacketDroneLaunch implements IMessage {

    // Attributes
    @Getter private int entityId;

    // Constructors
    public PacketDroneLaunch() {}
    public PacketDroneLaunch(int entityId) {
        this.entityId = entityId;
    }

    // Override Methods
    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
    }
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
    }

    // Handler
    public static class Handler implements IMessageHandler<PacketDroneLaunch, IMessage> {
        @Override
        public IMessage onMessage(PacketDroneLaunch msg, MessageContext ctx) {
            EntityPlayer player = ctx.getServerHandler().player;
            Objects.requireNonNull(player.getServer()).addScheduledTask(() -> {
                Entity e = player.world.getEntityByID(msg.entityId);
                if (e instanceof EntitySuicideDrone drone) {
                    drone.tryLaunch();
                }
            });
            return null;
        }
    }
}
