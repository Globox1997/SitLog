package net.sitlog.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityPose;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sitlog.access.LivingEntityAccess;
import net.sitlog.access.PlayerEntityAccess;
import net.sitlog.init.ConfigInit;
import net.sitlog.network.packet.*;

public class SitLogServerPacket {

    public static void init() {
        PayloadTypeRegistry.playS2C().register(PlayerListPacket.PACKET_ID, PlayerListPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(PlayerListSinglePacket.PACKET_ID, PlayerListSinglePacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(SitPacket.PACKET_ID, SitPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(FakePlayerPacket.PACKET_ID, FakePlayerPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(SitPacket.PACKET_ID, SitPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(DespawnTimerPacket.PACKET_ID, DespawnTimerPacket.PACKET_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SitPacket.PACKET_ID, (payload, context) -> {
            int entityId = payload.entityId();
            boolean sit = payload.sit();
            int despawnTime = payload.despawnTime(); // Unused from client side, use config entry
            context.server().execute(() -> {
                if (sit && context.player().hasVehicle()) {
                    return;
                }
                if (!context.player().isSpectator() && context.player().isAlive() && context.player().getPose().equals(EntityPose.STANDING)) {
                    ((LivingEntityAccess) context.player()).sitLog$setSitting(sit);
                    context.player().calculateDimensions();
                    if (ConfigInit.CONFIG.despawnTime > 0) {
                        ((PlayerEntityAccess) context.player()).sitLog$setDespawnTimer(sit ? ConfigInit.CONFIG.despawnTime : -1);
                    }
                    for (ServerPlayerEntity serverPlayerEntity : context.player().getServerWorld().getPlayers()) {
                        ServerPlayNetworking.send(serverPlayerEntity, new SitPacket(entityId, sit, ConfigInit.CONFIG.despawnTime > 0 ? (sit ? ConfigInit.CONFIG.despawnTime : -1) : 0));
                    }
                }
            });
        });
    }

}
