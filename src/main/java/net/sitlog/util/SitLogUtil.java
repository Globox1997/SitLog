package net.sitlog.util;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueInput;
import net.sitlog.SitlogMain;
import net.sitlog.access.FakePlayerAccess;
import net.sitlog.access.LivingEntityAccess;
import net.sitlog.access.PlayerEntityAccess;
import net.sitlog.init.ConfigInit;
import net.sitlog.mixin.access.MinecraftServerAccessor;
import net.sitlog.mixin.access.PlayerEntityAccessor;
import net.sitlog.mixin.access.PlayerManagerAccessor;
import net.sitlog.mixin.access.ServerPlayerInteractionManagerAccessor;
import net.sitlog.network.packet.CombatTagPacket;
import net.sitlog.network.packet.PlayerListPacket;
import net.sitlog.network.packet.PlayerListSinglePacket;
import net.sitlog.network.packet.SitPacket;
import net.sitlog.state.SittingPlayerState;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class SitLogUtil {

    public static boolean isBody(Entity entity) {
        return SitlogMain.SITLOG_PLAYER_MAP.inverse().containsKey(entity.getUUID());
    }

    public static boolean usesDespawnTimer(ServerPlayer player) {
        return ConfigInit.CONFIG.despawnTime > 0 && !player.level().getServer().isSingleplayerOwner(new NameAndId(player.getGameProfile()));
    }

    public static void setSitting(ServerPlayer player, boolean sit) {
        ((LivingEntityAccess) player).sitLog$setSitting(sit);
        player.refreshDimensions();

        int despawnTimer = 0;
        if (usesDespawnTimer(player)) {
            despawnTimer = sit ? ConfigInit.CONFIG.despawnTime : -1;
            ((PlayerEntityAccess) player).sitLog$setDespawnTimer(despawnTimer);
        }

        SitPacket sitPacket = new SitPacket(player.getId(), sit, despawnTimer);
        ServerPlayNetworking.send(player, sitPacket);
        for (ServerPlayer trackingPlayer : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(trackingPlayer, sitPacket);
        }
    }

    public static void onDamage(LivingEntity target, DamageSource source) {
        if (!ConfigInit.CONFIG.onlyInCombat || !(source.getEntity() instanceof LivingEntity attacker) || attacker == target) {
            return;
        }
        if (ConfigInit.CONFIG.combatOnlyAgainstPlayers && !(target instanceof Player && attacker instanceof Player)) {
            return;
        }
        tagCombat(target);
        tagCombat(attacker);
    }

    public static boolean isInCombat(ServerPlayer player) {
        return ((PlayerEntityAccess) player).sitLog$getCombatTimer() > 0;
    }

    private static void tagCombat(LivingEntity entity) {
        if (entity instanceof ServerPlayer player && !(player instanceof FakePlayer) && ConfigInit.CONFIG.combatTagTime > 0) {
            ((PlayerEntityAccess) player).sitLog$setCombatTimer(ConfigInit.CONFIG.combatTagTime);
            ServerPlayNetworking.send(player, new CombatTagPacket(ConfigInit.CONFIG.combatTagTime));
        }
    }

    public static void createBody(MinecraftServer server, ServerPlayer player) {
        UUID oldBodyUuid = SitlogMain.SITLOG_PLAYER_MAP.get(player.getUUID());
        if (oldBodyUuid != null) {
            removeBody(server, player.getUUID(), oldBodyUuid);
        }

        ServerLevel serverWorld = player.level();
        FakePlayer body = FakePlayer.get(serverWorld, new GameProfile(UUID.randomUUID(), player.getName().getString()));
        detachFromPlayerManager(server, body);

        byte playerModelParts = player.getEntityData().get(PlayerEntityAccessor.getPLAYER_MODEL_PARTS());
        body.getEntityData().set(PlayerEntityAccessor.getPLAYER_MODEL_PARTS(), playerModelParts);

        copyPlayerState(player, body);
        body.setServerLevel(serverWorld);
        body.snapTo(player.position(), player.getYRot(), player.getXRot());

        if (ConfigInit.CONFIG.bodyLifetime > 0) {
            ((FakePlayerAccess) body).sitLog$setBodyDespawnTimer(ConfigInit.CONFIG.bodyLifetime);
        }

        SitlogMain.SITLOG_PLAYER_MAP.put(player.getUUID(), body.getUUID());
        for (ServerPlayer serverPlayerEntity : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(serverPlayerEntity, new PlayerListPacket(body.getUUID(), player.getGameProfile(), player.getName(), Map.of(player.getUUID(), body.getUUID())));
        }

        spawnBody(server, body);
        saveBody(server, body);
        SittingPlayerState.getSittingPlayerState(server);
    }

    public static void spawnBody(MinecraftServer server, ServerPlayer body) {
        ((LivingEntityAccess) body).sitLog$setSitting(true);
        body.refreshDimensions();

        ((PlayerManagerAccessor) server.getPlayerList()).getPlayerMap().put(body.getUUID(), body);
        body.level().addNewPlayer(body);
    }

    @Nullable
    public static ServerPlayer loadBody(MinecraftServer server, UUID bodyUuid, String fallbackName) {
        Optional<CompoundTag> optional = loadBodyData(server, bodyUuid);
        if (optional.isEmpty()) {
            return null;
        }
        CompoundTag nbt = optional.get();

        ResourceKey<Level> registryKey = nbt.read("Dimension", Level.RESOURCE_KEY_CODEC).orElse(Level.OVERWORLD);
        ServerLevel serverWorld = server.getLevel(registryKey);
        if (serverWorld == null) {
            return null;
        }

        String name = nbt.getStringOr("SittingName", fallbackName);
        FakePlayer body = FakePlayer.get(serverWorld, new GameProfile(bodyUuid, name));
        detachFromPlayerManager(server, body);
        body.load(TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), nbt));
        body.setServerLevel(serverWorld);
        return body;
    }

    public static void restoreFromBody(MinecraftServer server, ServerPlayer player) {
        UUID bodyUuid = SitlogMain.SITLOG_PLAYER_MAP.get(player.getUUID());
        if (bodyUuid == null) {
            return;
        }

        ServerPlayer body = ((PlayerManagerAccessor) server.getPlayerList()).getPlayerMap().get(bodyUuid);
        if (body == null) {
            body = loadBody(server, bodyUuid, player.getName().getString());
        }

        if (body != null) {
            copyPlayerState(body, player);

            if (body.isDeadOrDying()) {
                player.removeAllEffects();
                player.setRemainingFireTicks(0);
            }
            if (body instanceof FakePlayerAccess fakePlayerAccess) {
                sendOfflineNotice(player, body.isDeadOrDying(), fakePlayerAccess);
            }

            player.teleportTo(body.level(), body.getX(), body.getY(), body.getZ(), Set.of(), body.getYRot(), body.getXRot(), false);
        }

        removeBody(server, player.getUUID(), bodyUuid);
    }

    private static void sendOfflineNotice(ServerPlayer player, boolean dead, FakePlayerAccess body) {
        if (dead) {
            Component deathMessage = body.sitLog$getDeathMessage();
            if (deathMessage != null) {
                player.sendSystemMessage(Component.translatableWithFallback("text.sitlog.body_killed", "Your body was killed while you were offline: %s", deathMessage));
            } else {
                player.sendSystemMessage(Component.translatableWithFallback("text.sitlog.body_killed_unknown", "Your body was killed while you were offline"));
            }
        } else if (body.sitLog$isDespawned()) {
            player.sendSystemMessage(Component.translatableWithFallback("text.sitlog.body_despawned", "Your body despawned while you were offline"));
        }

        if (body.sitLog$getDamageTaken() > 0.0F) {
            String hearts = String.format(Locale.ROOT, "%.1f", body.sitLog$getDamageTaken() / 2.0F);
            Collection<String> attackers = body.sitLog$getAttackers();
            if (attackers.isEmpty()) {
                player.sendSystemMessage(Component.translatableWithFallback("text.sitlog.body_damaged", "Your body took %s hearts of damage while you were offline", hearts));
            } else {
                player.sendSystemMessage(Component.translatableWithFallback("text.sitlog.body_damaged_by", "Your body took %s hearts of damage from %s while you were offline", hearts, String.join(", ", attackers)));
            }
        }
    }

    public static void unloadBody(ServerPlayer body, Entity.RemovalReason reason) {
        MinecraftServer server = body.level().getServer();
        saveBody(server, body);

        if (!body.isRemoved()) {
            body.level().removePlayerImmediately(body, reason);
        }
        ((PlayerManagerAccessor) server.getPlayerList()).getPlayerMap().remove(body.getUUID(), body);
        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(body.getUUID())));
    }

    public static void removeBody(MinecraftServer server, UUID playerUuid, UUID bodyUuid) {
        if (((PlayerManagerAccessor) server.getPlayerList()).getPlayerMap().remove(bodyUuid) instanceof FakePlayer body && !body.isRemoved()) {
            body.level().removePlayerImmediately(body, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
        }

        SitlogMain.SITLOG_PLAYER_MAP.remove(playerUuid);
        deleteBodyFiles(server, bodyUuid);
        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(bodyUuid)));
        SittingPlayerState.getSittingPlayerState(server);
    }

    public static void sendBodies(MinecraftServer server, ServerPlayer player) {
        Map<UUID, ServerPlayer> playerMap = ((PlayerManagerAccessor) server.getPlayerList()).getPlayerMap();
        for (Map.Entry<UUID, UUID> entry : SitlogMain.SITLOG_PLAYER_MAP.entrySet()) {
            if (playerMap.get(entry.getValue()) instanceof FakePlayer body && body.isAlive()) {
                ServerPlayNetworking.send(player, new PlayerListSinglePacket(body.getUUID(), new GameProfile(entry.getKey(), body.getName().getString()), body.getName()));
            }
        }
    }

    public static void saveBody(MinecraftServer server, ServerPlayer body) {
        ((MinecraftServerAccessor) server).getSaveHandler().save(body);
    }

    public static boolean hasBodyData(MinecraftServer server, UUID bodyUuid) {
        return loadBodyData(server, bodyUuid).isPresent();
    }

    private static Optional<CompoundTag> loadBodyData(MinecraftServer server, UUID bodyUuid) {
        try {
            return ((MinecraftServerAccessor) server).getSaveHandler().load(new NameAndId(bodyUuid, bodyUuid.toString())).filter(nbt -> nbt.getBooleanOr("SittingBody", false));
        } catch (Exception exception) {
            SitlogMain.LOGGER.warn("Failed to load sitting player data {}", bodyUuid, exception);
        }
        return Optional.empty();
    }

    private static void deleteBodyFiles(MinecraftServer server, UUID bodyUuid) {
        Path playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        List<Path> paths = List.of(playerDataDir.resolve(bodyUuid + ".dat"), playerDataDir.resolve(bodyUuid + ".dat_old"), server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(bodyUuid + ".json"),
                server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(bodyUuid + ".json"));
        for (Path path : paths) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException exception) {
                SitlogMain.LOGGER.warn("Failed to delete {}", path, exception);
            }
        }
    }

    private static void detachFromPlayerManager(MinecraftServer server, ServerPlayer body) {
        PlayerManagerAccessor playerManagerAccessor = (PlayerManagerAccessor) server.getPlayerList();
        playerManagerAccessor.getStatisticsMap().remove(body.getUUID());
        PlayerAdvancements advancementTracker = playerManagerAccessor.getAdvancementTrackers().remove(body.getUUID());
        if (advancementTracker != null) {
            advancementTracker.clearTriggers();
        }
    }

    private static void copyPlayerState(ServerPlayer from, ServerPlayer to) {
        to.getInventory().replaceWith(from.getInventory());
        to.setHealth(from.getHealth());

        to.setAirSupply(from.getAirSupply());
        to.setRemainingFireTicks(from.getRemainingFireTicks());
        to.setGlowingTag(from.isCurrentlyGlowing());
        to.setPermanentlyInvulnerable(from.isPermanentlyInvulnerable());
        ((ServerPlayerInteractionManagerAccessor) to.gameMode).callSetGameMode(from.gameMode.getGameModeForPlayer(), from.gameMode.getPreviousGameModeForPlayer());

        to.removeAllEffects();
        for (MobEffectInstance statusEffectInstance : from.getActiveEffects()) {
            to.addEffect(new MobEffectInstance(statusEffectInstance));
        }
        to.experienceLevel = from.experienceLevel;
        to.totalExperience = from.totalExperience;
        to.experienceProgress = from.experienceProgress;
        to.setScore(from.getScore());
        to.portalProcess = from.portalProcess;
    }
}
