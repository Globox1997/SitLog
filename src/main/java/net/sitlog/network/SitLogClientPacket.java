package net.sitlog.network;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.sitlog.SitlogMain;
import net.sitlog.access.AbstractClientPlayerEntityAccess;
import net.sitlog.access.LivingEntityAccess;
import net.sitlog.access.PlayerEntityAccess;
import net.sitlog.mixin.client.ClientPlayNetworkHandlerAccessor;
import net.sitlog.network.packet.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public class SitLogClientPacket {

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(PlayerListPacket.PACKET_ID, (payload, context) -> {
            UUID profileId = payload.profileId();
            GameProfile profile = payload.profile();
            Text displayName = payload.displayName();
            SitlogMain.SITLOG_PLAYER_MAP.clear();
            SitlogMain.SITLOG_PLAYER_MAP.putAll(payload.sitlogMap());

            PlayerListEntry playerListEntry = new PlayerListEntry(profile, false);
            playerListEntry.setDisplayName(displayName);
            ((ClientPlayNetworkHandlerAccessor) context.client().getNetworkHandler()).getPlayerListEntries().put(profileId, playerListEntry);
//            context.client().execute(() -> {
//            });
        });
        ClientPlayNetworking.registerGlobalReceiver(PlayerListSinglePacket.PACKET_ID, (payload, context) -> {
            UUID profileId = payload.profileId();
            GameProfile profile = payload.profile();
            Text displayName = payload.displayName();

            PlayerListEntry playerListEntry = new PlayerListEntry(profile, false);
            playerListEntry.setDisplayName(displayName);
            ((ClientPlayNetworkHandlerAccessor) context.client().getNetworkHandler()).getPlayerListEntries().putIfAbsent(profileId, playerListEntry);

            context.client().execute(() -> {
                CompletableFuture.supplyAsync(() -> {
                    return context.client().getSessionService().fetchProfile(profile.getId(), true);
                }).thenAcceptAsync(profileResult -> {

                    GameProfile finalProfile = (profileResult != null) ? profileResult.profile() : profile;

                    PlayerListEntry newPlayerListEntry = new PlayerListEntry(finalProfile, false);
                    newPlayerListEntry.setDisplayName(displayName);

                    ((ClientPlayNetworkHandlerAccessor) context.client().getNetworkHandler()).getPlayerListEntries().put(profileId, newPlayerListEntry);

                    PlayerEntity playerEntity = context.player().getWorld().getPlayerByUuid(profileId);
                    if (playerEntity != null && playerEntity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity) {
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
                if (context.player().getWorld().getEntityById(entityId) instanceof PlayerEntity playerEntity) {
                    ((LivingEntityAccess) playerEntity).sitLog$setSitting(sit);
                    ((PlayerEntityAccess) playerEntity).sitLog$setDespawnTimer(despawnTime);
                    playerEntity.calculateDimensions();
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(FakePlayerPacket.PACKET_ID, (payload, context) -> {
            int entityId = payload.entityId();
            boolean sit = payload.sit();
            float yaw = payload.yaw();
            context.client().execute(() -> {
                if (context.player().getWorld().getEntityById(entityId) instanceof PlayerEntity playerEntity) {
                    ((LivingEntityAccess) playerEntity).sitLog$setSitting(sit);
                    playerEntity.calculateDimensions();
                    playerEntity.setYaw(yaw);
                    playerEntity.setHeadYaw(yaw);
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(DespawnTimerPacket.PACKET_ID, (payload, context) -> {
            int despawnTimer = payload.despawnTimer();
            context.client().execute(() -> {
                ((PlayerEntityAccess) context.player()).sitLog$setDespawnTimer(despawnTimer);
            });
        });
    }

}
