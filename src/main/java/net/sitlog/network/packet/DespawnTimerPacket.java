package net.sitlog.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DespawnTimerPacket(int despawnTimer) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DespawnTimerPacket> PACKET_ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("sitlog", "despawn_timer_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DespawnTimerPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeInt(value.despawnTimer());
    }, buf -> {
        return new DespawnTimerPacket( buf.readInt());
    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}

