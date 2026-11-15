package net.sitlog.init;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.sitlog.SitlogMain;
import net.sitlog.access.FakePlayerAccess;
import net.sitlog.access.LivingEntityAccess;
import net.sitlog.access.PlayerEntityAccess;
import net.sitlog.entity.SitLogBody;
import net.sitlog.network.packet.ConfigSyncPacket;
import net.sitlog.network.packet.DespawnTimerPacket;
import net.sitlog.network.packet.FakePlayerPacket;
import net.sitlog.network.packet.SitPacket;
import net.sitlog.platform.Services;
import net.sitlog.state.SittingPlayerState;
import net.sitlog.util.SitLogUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EventInit {

    public static void onStartTracking(Entity entity, ServerPlayer serverPlayerEntity) {
        if (entity instanceof SitLogBody body && SitLogUtil.isBody(body)) {
            Services.network().sendToPlayer(serverPlayerEntity, new FakePlayerPacket(body.getId(), true, body.getYRot()));
        } else if (entity instanceof ServerPlayer otherServerPlayerEntity && ((LivingEntityAccess) otherServerPlayerEntity).sitLog$getSitting()) {
            Services.network().sendToPlayer(serverPlayerEntity, new SitPacket(otherServerPlayerEntity.getId(), true, ConfigInit.CONFIG.despawnTime > 0 ? ConfigInit.CONFIG.despawnTime : 0));
            otherServerPlayerEntity.refreshDimensions();
        }
    }

    public static void onPlayerJoin(ServerPlayer serverPlayerEntity) {
        Services.network().sendToPlayer(serverPlayerEntity, new ConfigSyncPacket(ConfigInit.CONFIG.sittingPlayerCanJump, ConfigInit.CONFIG.onlyInCombat));
        if (SitLogUtil.usesDespawnTimer(serverPlayerEntity)) {
            ((PlayerEntityAccess) serverPlayerEntity).sitLog$setDespawnTimer(-1);
            Services.network().sendToPlayer(serverPlayerEntity, new DespawnTimerPacket(-1));
        }
    }

    public static void onPlayerRespawn(ServerPlayer newPlayer) {
        if (SitLogUtil.usesDespawnTimer(newPlayer)) {
            ((PlayerEntityAccess) newPlayer).sitLog$setDespawnTimer(-1);
            Services.network().sendToPlayer(newPlayer, new DespawnTimerPacket(-1));
        }
    }

    public static void onServerStarting(MinecraftServer server) {
        SitlogMain.SITLOG_PLAYER_MAP.clear();
    }

    public static void onServerStarted(MinecraftServer server) {
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
    }

    public static void onServerStopping(MinecraftServer server) {
        SittingPlayerState.getSittingPlayerState(server);
    }

    public static void onServerStopped(MinecraftServer server) {
        SitlogMain.SITLOG_PLAYER_MAP.clear();
    }

}
