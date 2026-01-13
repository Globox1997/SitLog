package net.sitlog.init;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Dynamic;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.sitlog.SitlogMain;
import net.sitlog.access.LivingEntityAccess;
import net.sitlog.access.PlayerEntityAccess;
import net.sitlog.mixin.access.MinecraftServerAccessor;
import net.sitlog.mixin.access.PlayerManagerAccessor;
import net.sitlog.mixin.access.PlayerSaveHandlerAccessor;
import net.sitlog.network.packet.DespawnTimerPacket;
import net.sitlog.network.packet.FakePlayerPacket;
import net.sitlog.network.packet.SitPacket;
import net.sitlog.state.SittingPlayerState;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class EventInit {

    public static void init() {
        EntityTrackingEvents.START_TRACKING.register((entity, serverPlayerEntity) -> {
            if (entity instanceof FakePlayer fakePlayer && SitlogMain.SITLOG_PLAYER_MAP.inverse().containsKey(fakePlayer.getUuid())) {
                ((LivingEntityAccess) fakePlayer).sitLog$setSitting(true);
                ServerPlayNetworking.send(serverPlayerEntity, new FakePlayerPacket(fakePlayer.getId(), true, fakePlayer.getYaw()));
                fakePlayer.calculateDimensions();
            } else if (entity instanceof ServerPlayerEntity otherServerPlayerEntity && ((LivingEntityAccess) otherServerPlayerEntity).sitLog$getSitting()) {
                ServerPlayNetworking.send(serverPlayerEntity, new SitPacket(otherServerPlayerEntity.getId(), true, ConfigInit.CONFIG.despawnTime > 0 ? ConfigInit.CONFIG.despawnTime : 0));
                otherServerPlayerEntity.calculateDimensions();
            }
        });

        ServerPlayerEvents.JOIN.register((serverPlayerEntity) -> {
            if (ConfigInit.CONFIG.despawnTime > 0) {
                ((PlayerEntityAccess) serverPlayerEntity).sitLog$setDespawnTimer(-1);
                ServerPlayNetworking.send(serverPlayerEntity,new DespawnTimerPacket(-1));
            }
        });
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (ConfigInit.CONFIG.despawnTime > 0) {
                ((PlayerEntityAccess) newPlayer).sitLog$setDespawnTimer(((PlayerEntityAccess) oldPlayer).sitLog$getDespawnTimer());
                ServerPlayNetworking.send(newPlayer,new DespawnTimerPacket(((PlayerEntityAccess) oldPlayer).sitLog$getDespawnTimer()));
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            SittingPlayerState.getSittingPlayerState(server);

            if (ConfigInit.CONFIG.keepAfterRestart && ConfigInit.CONFIG.despawnTime <= 0) {
                File file = ((PlayerSaveHandlerAccessor) ((MinecraftServerAccessor) server).getSaveHandler()).getPlayerDataDir();

                for (Map.Entry<UUID, UUID> entry : SitlogMain.SITLOG_PLAYER_MAP.entrySet()) {
                    Optional<NbtCompound> optional = loadPlayerData(file, entry.getValue());
                    if (optional.isPresent()) {

                        RegistryKey<World> registryKey = optional.flatMap(
                                        nbt -> DimensionType.worldFromDimensionNbt(new Dynamic<>(NbtOps.INSTANCE, nbt.get("Dimension"))).resultOrPartial(SitlogMain.LOGGER::error))
                                .orElse(World.OVERWORLD);
                        ServerWorld serverWorld = server.getWorld(registryKey);
                        if (serverWorld != null) {
                            FakePlayer fakePlayer = FakePlayer.get(serverWorld, new GameProfile(entry.getValue(), optional.get().getString("SittingName")));
                            fakePlayer.readNbt(optional.get());

                            fakePlayer.setServerWorld(serverWorld);

                            if (server.getPlayerManager() instanceof PlayerManagerAccessor playerManagerAccessor) {
                                // playerManagerAccessor.getPlayers().add(fakePlayer); not required
                                playerManagerAccessor.getPlayerMap().put(fakePlayer.getUuid(), fakePlayer);
                            }

                            serverWorld.onPlayerConnected(fakePlayer);
                        }
                    }
                }
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(SittingPlayerState::getSittingPlayerState);
    }

    private static Optional<NbtCompound> loadPlayerData(File playerDataDir, UUID uuid) {

        File file = new File(playerDataDir, uuid.toString() + ".dat");
        if (file.exists() && file.isFile()) {
            try {
                return Optional.of(NbtIo.readCompressed(file.toPath(), NbtSizeTracker.ofUnlimitedBytes()));
            } catch (Exception var5) {
                 // LOGGER.warn("Failed to load player data for {}", player.getName().getString());
            }
        }

        return Optional.empty();
    }

}
