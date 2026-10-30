package net.sitlog.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ConfigSyncPacket(boolean sittingPlayerCanJump, boolean onlyInCombat) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ConfigSyncPacket> PACKET_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("sitlog", "config_sync_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeBoolean(value.sittingPlayerCanJump());
        buf.writeBoolean(value.onlyInCombat());
    }, buf -> {
        return new ConfigSyncPacket(buf.readBoolean(), buf.readBoolean());
    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}
