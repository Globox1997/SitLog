package net.sitlog.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CombatTagPacket(int combatTimer) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CombatTagPacket> PACKET_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("sitlog", "combat_tag_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CombatTagPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeInt(value.combatTimer());
    }, buf -> {
        return new CombatTagPacket(buf.readInt());
    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}
