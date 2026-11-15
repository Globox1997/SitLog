package net.sitlog.entity;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jspecify.annotations.Nullable;

public class SitLogBodyPacketListener extends ServerGamePacketListenerImpl {

    public SitLogBodyPacketListener(ServerPlayer body) {
        super(body.level().getServer(), new Connection(PacketFlow.CLIENTBOUND), body, CommonListenerCookie.createInitial(body.getGameProfile(), false));
    }

    @Override
    public void send(Packet<?> packet) {
    }

    @Override
    public void send(Packet<?> packet, @Nullable ChannelFutureListener listener) {
    }
}
