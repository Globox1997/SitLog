package net.sitlog.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DespawnTimerPacket(int despawnTimer) implements CustomPayload {

    public static final CustomPayload.Id<DespawnTimerPacket> PACKET_ID = new CustomPayload.Id<>(Identifier.of("sitlog", "despawn_timer_packet"));

    public static final PacketCodec<RegistryByteBuf, DespawnTimerPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeInt(value.despawnTimer());
    }, buf -> {
        return new DespawnTimerPacket( buf.readInt());
    });

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}

