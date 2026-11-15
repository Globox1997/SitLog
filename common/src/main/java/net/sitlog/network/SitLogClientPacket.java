package net.sitlog.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
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

public class SitLogClientPacket {

    public static void handlePlayerList(PlayerListPacket payload) {
        Minecraft client = Minecraft.getInstance();
        UUID profileId = payload.profileId();
        GameProfile profile = payload.profile();
        Component displayName = payload.displayName();
        for (Map.Entry<UUID, UUID> entry : payload.sitlogMap().entrySet()) {
            SitLogClient.CLIENT_PLAYER_MAP.forcePut(entry.getKey(), entry.getValue());
        }

        PlayerInfo playerListEntry = new PlayerInfo(profile, false);
        playerListEntry.setTabListDisplayName(displayName);
        ((ClientPlayNetworkHandlerAccessor) client.getConnection()).getPlayerListEntries().put(profileId, playerListEntry);
    }

    public static void handlePlayerListSingle(PlayerListSinglePacket payload) {
        Minecraft client = Minecraft.getInstance();
        UUID profileId = payload.profileId();
        GameProfile profile = payload.profile();
        Component displayName = payload.displayName();
        SitLogClient.CLIENT_PLAYER_MAP.forcePut(profile.id(), profileId);

        PlayerInfo playerListEntry = new PlayerInfo(profile, false);
        playerListEntry.setTabListDisplayName(displayName);
        ((ClientPlayNetworkHandlerAccessor) client.getConnection()).getPlayerListEntries().putIfAbsent(profileId, playerListEntry);

        client.execute(() -> {
            CompletableFuture.supplyAsync(() -> {
                return client.services().sessionService().fetchProfile(profile.id(), true);
            }).thenAcceptAsync(profileResult -> {

                GameProfile finalProfile = (profileResult != null) ? profileResult.profile() : profile;

                PlayerInfo newPlayerListEntry = new PlayerInfo(finalProfile, false);
                newPlayerListEntry.setTabListDisplayName(displayName);

                if (client.getConnection() == null || client.player == null) {
                    return;
                }
                ((ClientPlayNetworkHandlerAccessor) client.getConnection()).getPlayerListEntries().put(profileId, newPlayerListEntry);

                Player playerEntity = client.player.level().getPlayerByUUID(profileId);
                if (playerEntity != null && playerEntity instanceof AbstractClientPlayer abstractClientPlayerEntity) {
                    ((AbstractClientPlayerEntityAccess) abstractClientPlayerEntity).setPlayerListEntry(newPlayerListEntry);
                }

            }, client);

        });
    }

    public static void handleSit(SitPacket payload) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.level().getEntity(payload.entityId()) instanceof Player playerEntity) {
            ((LivingEntityAccess) playerEntity).sitLog$setSitting(payload.sit());
            ((PlayerEntityAccess) playerEntity).sitLog$setDespawnTimer(payload.despawnTime());
            playerEntity.refreshDimensions();
        }
    }

    public static void handleFakePlayer(FakePlayerPacket payload) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.level().getEntity(payload.entityId()) instanceof Player playerEntity) {
            ((LivingEntityAccess) playerEntity).sitLog$setSitting(payload.sit());
            playerEntity.refreshDimensions();
            playerEntity.setYRot(payload.yaw());
            playerEntity.setYHeadRot(payload.yaw());
        }
    }

    public static void handleDespawnTimer(DespawnTimerPacket payload) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            ((PlayerEntityAccess) client.player).sitLog$setDespawnTimer(payload.despawnTimer());
        }
    }

    public static void handleConfigSync(ConfigSyncPacket payload) {
        ConfigInit.SYNCED_SITTING_PLAYER_CAN_JUMP = payload.sittingPlayerCanJump();
        ConfigInit.SYNCED_ONLY_IN_COMBAT = payload.onlyInCombat();
    }

    public static void handleCombatTag(CombatTagPacket payload) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            ((PlayerEntityAccess) client.player).sitLog$setCombatTimer(payload.combatTimer());
        }
    }

    public static void onDisconnect() {
        SitLogClient.CLIENT_PLAYER_MAP.clear();
        ConfigInit.SYNCED_SITTING_PLAYER_CAN_JUMP = null;
        ConfigInit.SYNCED_ONLY_IN_COMBAT = null;
    }

}
