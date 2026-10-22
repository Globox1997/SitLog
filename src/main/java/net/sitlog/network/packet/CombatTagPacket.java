package net.sitlog.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CombatTagPacket(int combatTimer) implements CustomPayload {

    public static final CustomPayload.Id<CombatTagPacket> PACKET_ID = new CustomPayload.Id<>(Identifier.of("sitlog", "combat_tag_packet"));

    public static final PacketCodec<RegistryByteBuf, CombatTagPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeInt(value.combatTimer());
    }, buf -> {
        return new CombatTagPacket(buf.readInt());
    });

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
