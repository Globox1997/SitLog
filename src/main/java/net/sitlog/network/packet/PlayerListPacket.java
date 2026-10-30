package net.sitlog.network.packet;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Nullable;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlayerListPacket(UUID profileId, @Nullable GameProfile profile, @Nullable Component displayName, Map<UUID, UUID> sitlogMap) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PlayerListPacket> PACKET_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("sitlog", "player_list_packet"));

    private static final StreamCodec<ByteBuf, Map<UUID, UUID>> SITLOG_MAP_CODEC = ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, UUIDUtil.STREAM_CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerListPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeUUID(value.profileId());
        buf.writeUUID(value.profile().id());
        buf.writeUtf(value.profile().name(), 16);
        ByteBufCodecs.GAME_PROFILE_PROPERTIES.encode(buf, value.profile().properties());
        FriendlyByteBuf.writeNullable(buf, value.displayName(), ComponentSerialization.TRUSTED_STREAM_CODEC);
        SITLOG_MAP_CODEC.encode(buf, value.sitlogMap());
    }, buf -> {
        UUID uuid = buf.readUUID();
        UUID realPlayerUuid = buf.readUUID();
        String name = buf.readUtf(16);
        GameProfile gameProfile = new GameProfile(realPlayerUuid, name, ByteBufCodecs.GAME_PROFILE_PROPERTIES.decode(buf));
        Component displayName = FriendlyByteBuf.readNullable(buf, ComponentSerialization.TRUSTED_STREAM_CODEC);
        Map<UUID, UUID> sitlogMap = SITLOG_MAP_CODEC.decode(buf);

        return new PlayerListPacket(uuid, gameProfile, displayName, sitlogMap);
    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}

