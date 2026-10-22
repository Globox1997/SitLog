package net.sitlog.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ConfigSyncPacket(boolean sittingPlayerCanJump) implements CustomPayload {

    public static final CustomPayload.Id<ConfigSyncPacket> PACKET_ID = new CustomPayload.Id<>(Identifier.of("sitlog", "config_sync_packet"));

    public static final PacketCodec<RegistryByteBuf, ConfigSyncPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeBoolean(value.sittingPlayerCanJump());
    }, buf -> {
        return new ConfigSyncPacket(buf.readBoolean());
    });

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
