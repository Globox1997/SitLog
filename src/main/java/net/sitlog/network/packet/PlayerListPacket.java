package net.sitlog.network.packet;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public record PlayerListPacket(UUID profileId, @Nullable GameProfile profile, @Nullable Text displayName, Map<UUID, UUID> sitlogMap) implements CustomPayload {

    public static final CustomPayload.Id<PlayerListPacket> PACKET_ID = new CustomPayload.Id<>(Identifier.of("sitlog", "player_list_packet"));

    public static final PacketCodec<RegistryByteBuf, PlayerListPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeUuid(value.profileId());
        buf.writeUuid(value.profile().getId());
        buf.writeString(value.profile().getName(), 16);
        PacketCodecs.PROPERTY_MAP.encode(buf, value.profile().getProperties());
        PacketByteBuf.writeNullable(buf, value.displayName(), TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC);
        buf.writeMap(value.sitlogMap(), ((buf1, value1) -> buf1.writeUuid(value1)), (buf2, value2) -> buf2.writeUuid(value2));
    }, buf -> {
        UUID uuid = buf.readUuid();
        UUID realPlayerUuid = buf.readUuid();
        GameProfile gameProfile = new GameProfile(realPlayerUuid, buf.readString(16));
        gameProfile.getProperties().putAll(PacketCodecs.PROPERTY_MAP.decode(buf));
        Text displayName = PacketByteBuf.readNullable(buf, TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC);
        Map<UUID, UUID> sitlogMap = buf.readMap(buf1 -> buf1.readUuid(), buf2 -> buf2.readUuid());

        return new PlayerListPacket(uuid, gameProfile, displayName, sitlogMap);
    });

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}

