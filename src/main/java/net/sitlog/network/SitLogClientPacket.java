package net.sitlog.network;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.sitlog.SitLogClient;
import net.sitlog.access.AbstractClientPlayerEntityAccess;
import net.sitlog.access.LivingEntityAccess;
import net.sitlog.access.PlayerEntityAccess;
import net.sitlog.init.ConfigInit;
import net.sitlog.mixin.client.ClientPlayNetworkHandlerAccessor;
import net.sitlog.network.packet.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public class SitLogClientPacket {

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(PlayerListPacket.PACKET_ID, (payload, context) -> {
            UUID profileId = payload.profileId();
            GameProfile profile = payload.profile();
            Component displayName = payload.displayName();
            for (Map.Entry<UUID, UUID> entry : payload.sitlogMap().entrySet()) {
                SitLogClient.CLIENT_PLAYER_MAP.forcePut(entry.getKey(), entry.getValue());
            }

            PlayerInfo playerListEntry = new PlayerInfo(profile, false);
            playerListEntry.setTabListDisplayName(displayName);
            ((ClientPlayNetworkHandlerAccessor) context.client().getConnection()).getPlayerListEntries().put(profileId, playerListEntry);
        });
        ClientPlayNetworking.registerGlobalReceiver(PlayerListSinglePacket.PACKET_ID, (payload, context) -> {
            UUID profileId = payload.profileId();
            GameProfile profile = payload.profile();
            Component displayName = payload.displayName();
            SitLogClient.CLIENT_PLAYER_MAP.forcePut(profile.getId(), profileId);

            PlayerInfo playerListEntry = new PlayerInfo(profile, false);
            playerListEntry.setTabListDisplayName(displayName);
            ((ClientPlayNetworkHandlerAccessor) context.client().getConnection()).getPlayerListEntries().putIfAbsent(profileId, playerListEntry);

            context.client().execute(() -> {
                CompletableFuture.supplyAsync(() -> {
                    return context.client().getMinecraftSessionService().fetchProfile(profile.getId(), true);
                }).thenAcceptAsync(profileResult -> {

                    GameProfile finalProfile = (profileResult != null) ? profileResult.profile() : profile;

                    PlayerInfo newPlayerListEntry = new PlayerInfo(finalProfile, false);
                    newPlayerListEntry.setTabListDisplayName(displayName);

                    ((ClientPlayNetworkHandlerAccessor) context.client().getConnection()).getPlayerListEntries().put(profileId, newPlayerListEntry);

                    Player playerEntity = context.player().level().getPlayerByUUID(profileId);
                    if (playerEntity != null && playerEntity instanceof AbstractClientPlayer abstractClientPlayerEntity) {
                        ((AbstractClientPlayerEntityAccess) abstractClientPlayerEntity).setPlayerListEntry(newPlayerListEntry);
                    }

                }, context.client());

            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SitPacket.PACKET_ID, (payload, context) -> {
            int entityId = payload.entityId();
            boolean sit = payload.sit();
            int despawnTime = payload.despawnTime();
            context.client().execute(() -> {
                if (context.player().level().getEntity(entityId) instanceof Player playerEntity) {
                    ((LivingEntityAccess) playerEntity).sitLog$setSitting(sit);
                    ((PlayerEntityAccess) playerEntity).sitLog$setDespawnTimer(despawnTime);
                    playerEntity.refreshDimensions();
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(FakePlayerPacket.PACKET_ID, (payload, context) -> {
            int entityId = payload.entityId();
            boolean sit = payload.sit();
            float yaw = payload.yaw();
            context.client().execute(() -> {
                if (context.player().level().getEntity(entityId) instanceof Player playerEntity) {
                    ((LivingEntityAccess) playerEntity).sitLog$setSitting(sit);
                    playerEntity.refreshDimensions();
                    playerEntity.setYRot(yaw);
                    playerEntity.setYHeadRot(yaw);
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(DespawnTimerPacket.PACKET_ID, (payload, context) -> {
            int despawnTimer = payload.despawnTimer();
            context.client().execute(() -> {
                ((PlayerEntityAccess) context.player()).sitLog$setDespawnTimer(despawnTimer);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPacket.PACKET_ID, (payload, context) -> {
            ConfigInit.SYNCED_SITTING_PLAYER_CAN_JUMP = payload.sittingPlayerCanJump();
            ConfigInit.SYNCED_ONLY_IN_COMBAT = payload.onlyInCombat();
        });
        ClientPlayNetworking.registerGlobalReceiver(CombatTagPacket.PACKET_ID, (payload, context) -> {
            ((PlayerEntityAccess) context.player()).sitLog$setCombatTimer(payload.combatTimer());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SitLogClient.CLIENT_PLAYER_MAP.clear();
            ConfigInit.SYNCED_SITTING_PLAYER_CAN_JUMP = null;
            ConfigInit.SYNCED_ONLY_IN_COMBAT = null;
        });
    }

}
