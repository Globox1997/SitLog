package net.sitlog.init;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.sitlog.SitlogMain;
import net.sitlog.access.FakePlayerAccess;
import net.sitlog.access.LivingEntityAccess;
import net.sitlog.access.PlayerEntityAccess;
import net.sitlog.network.packet.ConfigSyncPacket;
import net.sitlog.network.packet.DespawnTimerPacket;
import net.sitlog.network.packet.FakePlayerPacket;
import net.sitlog.network.packet.SitPacket;
import net.sitlog.state.SittingPlayerState;
import net.sitlog.util.SitLogUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EventInit {

    public static void init() {
        EntityTrackingEvents.START_TRACKING.register((entity, serverPlayerEntity) -> {
            if (entity instanceof FakePlayer fakePlayer && SitLogUtil.isBody(fakePlayer)) {
                ServerPlayNetworking.send(serverPlayerEntity, new FakePlayerPacket(fakePlayer.getId(), true, fakePlayer.getYRot()));
            } else if (entity instanceof ServerPlayer otherServerPlayerEntity && ((LivingEntityAccess) otherServerPlayerEntity).sitLog$getSitting()) {
                ServerPlayNetworking.send(serverPlayerEntity, new SitPacket(otherServerPlayerEntity.getId(), true, ConfigInit.CONFIG.despawnTime > 0 ? ConfigInit.CONFIG.despawnTime : 0));
                otherServerPlayerEntity.refreshDimensions();
            }
        });

        ServerPlayerEvents.JOIN.register((serverPlayerEntity) -> {
            ServerPlayNetworking.send(serverPlayerEntity, new ConfigSyncPacket(ConfigInit.CONFIG.sittingPlayerCanJump, ConfigInit.CONFIG.onlyInCombat));
            if (SitLogUtil.usesDespawnTimer(serverPlayerEntity)) {
                ((PlayerEntityAccess) serverPlayerEntity).sitLog$setDespawnTimer(-1);
                ServerPlayNetworking.send(serverPlayerEntity, new DespawnTimerPacket(-1));
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (SitLogUtil.usesDespawnTimer(newPlayer)) {
                ((PlayerEntityAccess) newPlayer).sitLog$setDespawnTimer(-1);
                ServerPlayNetworking.send(newPlayer, new DespawnTimerPacket(-1));
            }
        });

        ServerLifecycleEvents.SERVER_STARTING.register((server) -> SitlogMain.SITLOG_PLAYER_MAP.clear());

        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            SittingPlayerState.getSittingPlayerState(server);

            boolean spawnBodies = ConfigInit.CONFIG.keepAfterRestart;
            for (Map.Entry<UUID, UUID> entry : new HashMap<>(SitlogMain.SITLOG_PLAYER_MAP).entrySet()) {
                if (!SitLogUtil.hasBodyData(server, entry.getValue())) {
                    SitLogUtil.removeBody(server, entry.getKey(), entry.getValue());
                } else if (spawnBodies) {
                    ServerPlayer body = SitLogUtil.loadBody(server, entry.getValue(), "");
                    if (body != null && body.isAlive() && !((FakePlayerAccess) body).sitLog$isDespawned()) {
                        SitLogUtil.spawnBody(server, body);
                    }
                }
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(SittingPlayerState::getSittingPlayerState);
        ServerLifecycleEvents.SERVER_STOPPED.register((server) -> SitlogMain.SITLOG_PLAYER_MAP.clear());
    }

}
