package net.sitlog.network.packet;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlayerListSinglePacket(UUID profileId, @Nullable GameProfile profile, @Nullable Component displayName) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PlayerListSinglePacket> PACKET_ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("sitlog", "player_list_single_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerListSinglePacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeUUID(value.profileId());
        buf.writeUUID(value.profile().getId());
        buf.writeUtf(value.profile().getName(), 16);
        ByteBufCodecs.GAME_PROFILE_PROPERTIES.encode(buf, value.profile().getProperties());
        FriendlyByteBuf.writeNullable(buf, value.displayName(), ComponentSerialization.TRUSTED_STREAM_CODEC);
    }, buf -> {
        UUID uuid = buf.readUUID();
        UUID realPlayerUuid = buf.readUUID();
        GameProfile gameProfile = new GameProfile(realPlayerUuid, buf.readUtf(16));
        gameProfile.getProperties().putAll(ByteBufCodecs.GAME_PROFILE_PROPERTIES.decode(buf));
        Component displayName = FriendlyByteBuf.readNullable(buf, ComponentSerialization.TRUSTED_STREAM_CODEC);

        return new PlayerListSinglePacket(uuid, gameProfile, displayName);
    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}

