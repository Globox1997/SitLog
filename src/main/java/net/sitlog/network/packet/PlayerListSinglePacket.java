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

import java.util.UUID;

public record PlayerListSinglePacket(UUID profileId, @Nullable GameProfile profile, @Nullable Text displayName) implements CustomPayload {

    public static final CustomPayload.Id<PlayerListSinglePacket> PACKET_ID = new CustomPayload.Id<>(Identifier.of("sitlog", "player_list_single_packet"));

    public static final PacketCodec<RegistryByteBuf, PlayerListSinglePacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeUuid(value.profileId());
        buf.writeUuid(value.profile().getId());
        buf.writeString(value.profile().getName(), 16);
        PacketCodecs.PROPERTY_MAP.encode(buf, value.profile().getProperties());
        PacketByteBuf.writeNullable(buf, value.displayName(), TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC);
    }, buf -> {
        UUID uuid = buf.readUuid();
        UUID realPlayerUuid = buf.readUuid();
        GameProfile gameProfile = new GameProfile(realPlayerUuid, buf.readString(16));
        gameProfile.getProperties().putAll(PacketCodecs.PROPERTY_MAP.decode(buf));
        Text displayName = PacketByteBuf.readNullable(buf, TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC);

        return new PlayerListSinglePacket(uuid, gameProfile, displayName);
    });

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}

