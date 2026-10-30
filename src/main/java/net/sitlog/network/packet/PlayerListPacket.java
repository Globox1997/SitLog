package net.sitlog.network.packet;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlayerListPacket(UUID profileId, @Nullable GameProfile profile, @Nullable Component displayName, Map<UUID, UUID> sitlogMap) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PlayerListPacket> PACKET_ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("sitlog", "player_list_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerListPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeUUID(value.profileId());
        buf.writeUUID(value.profile().getId());
        buf.writeUtf(value.profile().getName(), 16);
        ByteBufCodecs.GAME_PROFILE_PROPERTIES.encode(buf, value.profile().getProperties());
        FriendlyByteBuf.writeNullable(buf, value.displayName(), ComponentSerialization.TRUSTED_STREAM_CODEC);
        buf.writeMap(value.sitlogMap(), ((buf1, value1) -> buf1.writeUUID(value1)), (buf2, value2) -> buf2.writeUUID(value2));
    }, buf -> {
        UUID uuid = buf.readUUID();
        UUID realPlayerUuid = buf.readUUID();
        GameProfile gameProfile = new GameProfile(realPlayerUuid, buf.readUtf(16));
        gameProfile.getProperties().putAll(ByteBufCodecs.GAME_PROFILE_PROPERTIES.decode(buf));
        Component displayName = FriendlyByteBuf.readNullable(buf, ComponentSerialization.TRUSTED_STREAM_CODEC);
        Map<UUID, UUID> sitlogMap = buf.readMap(buf1 -> buf1.readUUID(), buf2 -> buf2.readUUID());

        return new PlayerListPacket(uuid, gameProfile, displayName, sitlogMap);
    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}

