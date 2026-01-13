package net.sitlog.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record FakePlayerPacket(int entityId, boolean sit, float yaw) implements CustomPayload {

    public static final CustomPayload.Id<FakePlayerPacket> PACKET_ID = new CustomPayload.Id<>(Identifier.of("sitlog", "fake_player_packet"));

    public static final PacketCodec<RegistryByteBuf, FakePlayerPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeInt(value.entityId());
        buf.writeBoolean(value.sit());
        buf.writeFloat(value.yaw());

    }, buf -> {
        return new FakePlayerPacket(buf.readInt(), buf.readBoolean(), buf.readFloat());
    });

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}

