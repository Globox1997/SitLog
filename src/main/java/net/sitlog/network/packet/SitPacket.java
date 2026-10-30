package net.sitlog.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SitPacket(int entityId, boolean sit, int despawnTime) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SitPacket> PACKET_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("sitlog", "sit_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SitPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeInt(value.entityId());
        buf.writeBoolean(value.sit());
        buf.writeInt(value.despawnTime());
    }, buf -> {
        return new SitPacket(buf.readInt(), buf.readBoolean(), buf.readInt());
    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}

