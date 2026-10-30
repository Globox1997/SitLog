package net.sitlog.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.sitlog.network.packet.*;
import net.sitlog.util.SitLogUtil;

public class SitLogServerPacket {

    public static void init() {
        PayloadTypeRegistry.playS2C().register(PlayerListPacket.PACKET_ID, PlayerListPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(PlayerListSinglePacket.PACKET_ID, PlayerListSinglePacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(SitPacket.PACKET_ID, SitPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(FakePlayerPacket.PACKET_ID, FakePlayerPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(SitPacket.PACKET_ID, SitPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(DespawnTimerPacket.PACKET_ID, DespawnTimerPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(ConfigSyncPacket.PACKET_ID, ConfigSyncPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(CombatTagPacket.PACKET_ID, CombatTagPacket.PACKET_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SitPacket.PACKET_ID, (payload, context) -> {
            boolean sit = payload.sit();
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                if (sit && player.isPassenger()) {
                    return;
                }
                if (!player.isSpectator() && player.isAlive() && player.getPose().equals(Pose.STANDING)) {
                    SitLogUtil.setSitting(player, sit);
                }
            });
        });
    }

}
