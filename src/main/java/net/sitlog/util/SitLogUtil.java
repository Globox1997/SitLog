package net.sitlog.util;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Dynamic;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.sitlog.SitlogMain;
import net.sitlog.access.FakePlayerAccess;
import net.sitlog.access.LivingEntityAccess;
import net.sitlog.access.PlayerEntityAccess;
import net.sitlog.init.ConfigInit;
import net.sitlog.mixin.access.MinecraftServerAccessor;
import net.sitlog.mixin.access.PlayerEntityAccessor;
import net.sitlog.mixin.access.PlayerManagerAccessor;
import net.sitlog.mixin.access.ServerPlayerInteractionManagerAccessor;
import net.sitlog.network.packet.PlayerListPacket;
import net.sitlog.network.packet.PlayerListSinglePacket;
import net.sitlog.network.packet.SitPacket;
import net.sitlog.state.SittingPlayerState;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SitLogUtil {

    public static boolean isBody(Entity entity) {
        return SitlogMain.SITLOG_PLAYER_MAP.inverse().containsKey(entity.getUuid());
    }

    public static boolean usesDespawnTimer(ServerPlayerEntity player) {
        return ConfigInit.CONFIG.despawnTime > 0 && !player.server.isHost(player.getGameProfile());
    }

    public static void setSitting(ServerPlayerEntity player, boolean sit) {
        ((LivingEntityAccess) player).sitLog$setSitting(sit);
        player.calculateDimensions();

        int despawnTimer = 0;
        if (usesDespawnTimer(player)) {
            despawnTimer = sit ? ConfigInit.CONFIG.despawnTime : -1;
            ((PlayerEntityAccess) player).sitLog$setDespawnTimer(despawnTimer);
        }

        SitPacket sitPacket = new SitPacket(player.getId(), sit, despawnTimer);
        ServerPlayNetworking.send(player, sitPacket);
        for (ServerPlayerEntity trackingPlayer : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(trackingPlayer, sitPacket);
        }
    }

    public static void createBody(MinecraftServer server, ServerPlayerEntity player) {
        UUID oldBodyUuid = SitlogMain.SITLOG_PLAYER_MAP.get(player.getUuid());
        if (oldBodyUuid != null) {
            removeBody(server, player.getUuid(), oldBodyUuid);
        }

        ServerWorld serverWorld = player.getServerWorld();
        FakePlayer body = FakePlayer.get(serverWorld, new GameProfile(UUID.randomUUID(), player.getName().getString()));
        detachFromPlayerManager(server, body);

        byte playerModelParts = player.getDataTracker().get(PlayerEntityAccessor.getPLAYER_MODEL_PARTS());
        body.getDataTracker().set(PlayerEntityAccessor.getPLAYER_MODEL_PARTS(), playerModelParts);

        copyPlayerState(player, body);
        body.setServerWorld(serverWorld);
        body.refreshPositionAndAngles(player.getPos(), player.getYaw(), player.getPitch());

        if (ConfigInit.CONFIG.despawnTime > 0) {
            ((FakePlayerAccess) body).sitLog$setBodyDespawnTimer(ConfigInit.CONFIG.despawnTime);
        }

        SitlogMain.SITLOG_PLAYER_MAP.put(player.getUuid(), body.getUuid());
        for (ServerPlayerEntity serverPlayerEntity : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(serverPlayerEntity, new PlayerListPacket(body.getUuid(), player.getGameProfile(), player.getName(), Map.of(player.getUuid(), body.getUuid())));
        }

        spawnBody(server, body);
        saveBody(server, body);
        SittingPlayerState.getSittingPlayerState(server);
    }

    public static void spawnBody(MinecraftServer server, ServerPlayerEntity body) {
        ((LivingEntityAccess) body).sitLog$setSitting(true);
        body.calculateDimensions();

        ((PlayerManagerAccessor) server.getPlayerManager()).getPlayerMap().put(body.getUuid(), body);
        body.getServerWorld().onPlayerConnected(body);
    }

    @Nullable
    public static ServerPlayerEntity loadBody(MinecraftServer server, UUID bodyUuid, String fallbackName) {
        Optional<NbtCompound> optional = loadBodyData(server, bodyUuid);
        if (optional.isEmpty()) {
            return null;
        }
        NbtCompound nbt = optional.get();

        RegistryKey<World> registryKey = DimensionType.worldFromDimensionNbt(new Dynamic<>(NbtOps.INSTANCE, nbt.get("Dimension"))).resultOrPartial(SitlogMain.LOGGER::error).orElse(World.OVERWORLD);
        ServerWorld serverWorld = server.getWorld(registryKey);
        if (serverWorld == null) {
            return null;
        }

        String name = nbt.contains("SittingName") ? nbt.getString("SittingName") : fallbackName;
        FakePlayer body = FakePlayer.get(serverWorld, new GameProfile(bodyUuid, name));
        detachFromPlayerManager(server, body);
        body.readNbt(nbt);
        body.setServerWorld(serverWorld);
        return body;
    }

    public static void restoreFromBody(MinecraftServer server, ServerPlayerEntity player) {
        UUID bodyUuid = SitlogMain.SITLOG_PLAYER_MAP.get(player.getUuid());
        if (bodyUuid == null) {
            return;
        }

        ServerPlayerEntity body = ((PlayerManagerAccessor) server.getPlayerManager()).getPlayerMap().get(bodyUuid);
        if (body == null) {
            body = loadBody(server, bodyUuid, player.getName().getString());
        }

        if (body != null) {
            copyPlayerState(body, player);

            if (body.isDead()) {
                player.clearStatusEffects();
                player.setFireTicks(0);

                Text deathMessage = body instanceof FakePlayerAccess fakePlayerAccess ? fakePlayerAccess.sitLog$getDeathMessage() : null;
                if (deathMessage != null) {
                    player.sendMessage(Text.translatableWithFallback("text.sitlog.body_killed", "Your body was killed while you were offline: %s", deathMessage), false);
                } else {
                    player.sendMessage(Text.translatableWithFallback("text.sitlog.body_killed_unknown", "Your body was killed while you were offline"), false);
                }
            }

            player.teleport(body.getServerWorld(), body.getX(), body.getY(), body.getZ(), body.getYaw(), body.getPitch());
        }

        removeBody(server, player.getUuid(), bodyUuid);
    }

    public static void unloadBody(ServerPlayerEntity body, Entity.RemovalReason reason) {
        MinecraftServer server = body.server;
        saveBody(server, body);

        if (!body.isRemoved()) {
            body.getServerWorld().removePlayer(body, reason);
        }
        ((PlayerManagerAccessor) server.getPlayerManager()).getPlayerMap().remove(body.getUuid(), body);
        server.getPlayerManager().sendToAll(new PlayerRemoveS2CPacket(List.of(body.getUuid())));
    }

    public static void removeBody(MinecraftServer server, UUID playerUuid, UUID bodyUuid) {
        if (((PlayerManagerAccessor) server.getPlayerManager()).getPlayerMap().remove(bodyUuid) instanceof FakePlayer body && !body.isRemoved()) {
            body.getServerWorld().removePlayer(body, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
        }

        SitlogMain.SITLOG_PLAYER_MAP.remove(playerUuid);
        deleteBodyFiles(server, bodyUuid);
        server.getPlayerManager().sendToAll(new PlayerRemoveS2CPacket(List.of(bodyUuid)));
        SittingPlayerState.getSittingPlayerState(server);
    }

    public static void sendBodies(MinecraftServer server, ServerPlayerEntity player) {
        Map<UUID, ServerPlayerEntity> playerMap = ((PlayerManagerAccessor) server.getPlayerManager()).getPlayerMap();
        for (Map.Entry<UUID, UUID> entry : SitlogMain.SITLOG_PLAYER_MAP.entrySet()) {
            if (playerMap.get(entry.getValue()) instanceof FakePlayer body && body.isAlive()) {
                ServerPlayNetworking.send(player, new PlayerListSinglePacket(body.getUuid(), new GameProfile(entry.getKey(), body.getName().getString()), body.getName()));
            }
        }
    }

    public static void saveBody(MinecraftServer server, ServerPlayerEntity body) {
        ((MinecraftServerAccessor) server).getSaveHandler().savePlayerData(body);
    }

    public static boolean hasBodyData(MinecraftServer server, UUID bodyUuid) {
        return loadBodyData(server, bodyUuid).isPresent();
    }

    private static Optional<NbtCompound> loadBodyData(MinecraftServer server, UUID bodyUuid) {
        Path path = server.getSavePath(WorldSavePath.PLAYERDATA).resolve(bodyUuid + ".dat");
        if (Files.isRegularFile(path)) {
            try {
                NbtCompound nbt = NbtIo.readCompressed(path, NbtSizeTracker.ofUnlimitedBytes());
                if (nbt.getBoolean("SittingBody")) {
                    return Optional.of(nbt);
                }
            } catch (Exception exception) {
                SitlogMain.LOGGER.warn("Failed to load sitting player data {}", bodyUuid, exception);
            }
        }
        return Optional.empty();
    }

    private static void deleteBodyFiles(MinecraftServer server, UUID bodyUuid) {
        Path playerDataDir = server.getSavePath(WorldSavePath.PLAYERDATA);
        List<Path> paths = List.of(playerDataDir.resolve(bodyUuid + ".dat"), playerDataDir.resolve(bodyUuid + ".dat_old"), server.getSavePath(WorldSavePath.STATS).resolve(bodyUuid + ".json"),
                server.getSavePath(WorldSavePath.ADVANCEMENTS).resolve(bodyUuid + ".json"));
        for (Path path : paths) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException exception) {
                SitlogMain.LOGGER.warn("Failed to delete {}", path, exception);
            }
        }
    }

    private static void detachFromPlayerManager(MinecraftServer server, ServerPlayerEntity body) {
        PlayerManagerAccessor playerManagerAccessor = (PlayerManagerAccessor) server.getPlayerManager();
        playerManagerAccessor.getStatisticsMap().remove(body.getUuid());
        PlayerAdvancementTracker advancementTracker = playerManagerAccessor.getAdvancementTrackers().remove(body.getUuid());
        if (advancementTracker != null) {
            advancementTracker.clearCriteria();
        }
    }

    private static void copyPlayerState(ServerPlayerEntity from, ServerPlayerEntity to) {
        to.getInventory().clone(from.getInventory());
        to.setHealth(from.getHealth());

        to.setAir(from.getAir());
        to.setFireTicks(from.getFireTicks());
        to.setGlowing(from.isGlowing());
        to.setInvulnerable(from.isInvulnerable());
        ((ServerPlayerInteractionManagerAccessor) to.interactionManager).callSetGameMode(from.interactionManager.getGameMode(), from.interactionManager.getPreviousGameMode());

        to.clearStatusEffects();
        for (StatusEffectInstance statusEffectInstance : from.getStatusEffects()) {
            to.addStatusEffect(new StatusEffectInstance(statusEffectInstance));
        }
        to.experienceLevel = from.experienceLevel;
        to.totalExperience = from.totalExperience;
        to.experienceProgress = from.experienceProgress;
        to.setScore(from.getScore());
        to.portalManager = from.portalManager;
    }
}
