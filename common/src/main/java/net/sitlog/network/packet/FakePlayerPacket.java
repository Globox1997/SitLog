package net.sitlog.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FakePlayerPacket(int entityId, boolean sit, float yaw) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FakePlayerPacket> PACKET_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("sitlog", "fake_player_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FakePlayerPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeInt(value.entityId());
        buf.writeBoolean(value.sit());
        buf.writeFloat(value.yaw());

    }, buf -> {
        return new FakePlayerPacket(buf.readInt(), buf.readBoolean(), buf.readFloat());
    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}

