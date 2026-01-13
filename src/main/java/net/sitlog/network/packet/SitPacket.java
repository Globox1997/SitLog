package net.sitlog.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SitPacket(int entityId, boolean sit, int despawnTime) implements CustomPayload {

    public static final CustomPayload.Id<SitPacket> PACKET_ID = new CustomPayload.Id<>(Identifier.of("sitlog", "sit_packet"));

    public static final PacketCodec<RegistryByteBuf, SitPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeInt(value.entityId());
        buf.writeBoolean(value.sit());
        buf.writeInt(value.despawnTime());
    }, buf -> {
        return new SitPacket(buf.readInt(), buf.readBoolean(), buf.readInt());
    });

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}

