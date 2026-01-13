package net.sitlog.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sitlog.SitlogMain;
import net.sitlog.access.FakePlayerAccess;
import net.sitlog.access.PlayerEntityAccess;
import net.sitlog.init.ConfigInit;
import net.sitlog.mixin.access.EntityAccessor;
import net.sitlog.mixin.access.PlayerManagerAccessor;
import net.sitlog.mixin.access.ServerPlayerInteractionManagerAccessor;
import net.sitlog.network.packet.PlayerListPacket;
import net.sitlog.network.packet.PlayerListSinglePacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Shadow
    @Mutable
    @Final
    private MinecraftServer server;
    @Shadow
    @Mutable
    @Final
    private List<ServerPlayerEntity> players;

    @Shadow
    @Final
    private Map<UUID, ServerPlayerEntity> playerMap;

    @Inject(method = "remove", at = @At("TAIL"))
    private void removeMixin(ServerPlayerEntity player, CallbackInfo info) {
        if (!this.server.isHost(player.getGameProfile()) && !(player instanceof FakePlayer)) {

            UUID uuid;
            if (SitlogMain.SITLOG_PLAYER_MAP.containsKey(player.getUuid())) {
                uuid = SitlogMain.SITLOG_PLAYER_MAP.get(player.getUuid());
            } else {
                uuid = UUID.randomUUID();
                SitlogMain.SITLOG_PLAYER_MAP.put(player.getUuid(), uuid);
            }

            if (ConfigInit.CONFIG.despawnTime > 0 || player.isCreativeLevelTwoOp()) {
                if (((PlayerEntityAccess) player).sitLog$getDespawnTimer() == 0 || player.isCreativeLevelTwoOp()) {
                    return;
                }
            }

            FakePlayer fakePlayer = FakePlayer.get(player.getServerWorld(), new GameProfile(uuid, player.getName().getString()));

            ((EntityAccessor) fakePlayer).setRemovalReason(null);
            fakePlayer.getInventory().clone(player.getInventory());
            fakePlayer.setHealth(player.getHealth());

            fakePlayer.setAir(player.getAir());
            fakePlayer.setFireTicks(player.getFireTicks());
            fakePlayer.setGlowing(player.isGlowing());
            fakePlayer.setInvulnerable(player.isInvulnerable());
            ((ServerPlayerInteractionManagerAccessor) fakePlayer.interactionManager).callSetGameMode(player.interactionManager.getGameMode(), player.interactionManager.getPreviousGameMode());

            for (StatusEffectInstance statusEffectInstance : player.getStatusEffects()) {
                fakePlayer.addStatusEffect(new StatusEffectInstance(statusEffectInstance));
            }
            fakePlayer.experienceLevel = player.experienceLevel;
            fakePlayer.totalExperience = player.totalExperience;
            fakePlayer.experienceProgress = player.experienceProgress;
            fakePlayer.setScore(player.getScore());
            fakePlayer.portalManager = player.portalManager;

            fakePlayer.setServerWorld(player.getServerWorld());

            fakePlayer.setPosition(player.getX(), player.getY(), player.getZ());

            fakePlayer.refreshPositionAndAngles(player.getPos(), player.getYaw(), player.getPitch());


            if (this instanceof PlayerManagerAccessor playerManagerAccessor) {
                // playerManagerAccessor.getPlayers().add(fakePlayer);
                playerManagerAccessor.getPlayerMap().put(fakePlayer.getUuid(), fakePlayer);
            }

            for (ServerPlayerEntity serverPlayerEntity : this.players) {
                ServerPlayNetworking.send(serverPlayerEntity, new PlayerListPacket(fakePlayer.getUuid(), player.getGameProfile(), player.getName(), SitlogMain.SITLOG_PLAYER_MAP));
            }

            player.getServerWorld().onPlayerConnected(fakePlayer);

            if (ConfigInit.CONFIG.despawnTime > 0) {
                ((FakePlayerAccess) fakePlayer).sitLog$setDespawnTimer(ConfigInit.CONFIG.despawnTime);
            }
        }
    }

    @WrapOperation(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V", ordinal = 5))
    private void onPlayerConnectMixin(ServerPlayNetworkHandler instance, Packet<?> packet, Operation<Void> original) {

        List<ServerPlayerEntity> realPlayers = new ArrayList<>();
        List<ServerPlayerEntity> fakePlayers = new ArrayList<>();
        for (ServerPlayerEntity player : this.playerMap.values()) {
            if (SitlogMain.SITLOG_PLAYER_MAP.inverse().containsKey(player.getUuid())) {
                fakePlayers.add(player);
            } else {
                realPlayers.add(player);
            }
        }

        instance.sendPacket(PlayerListS2CPacket.entryFromPlayer(realPlayers));

        for (ServerPlayerEntity fakePlayer : fakePlayers) {
            UUID realPlayerUuid = SitlogMain.SITLOG_PLAYER_MAP.inverse().get(fakePlayer.getUuid());
            if (fakePlayer.isRemoved() || !fakePlayer.isAlive() || server.getPlayerManager().getPlayer(realPlayerUuid) != null || realPlayerUuid.equals(instance.getPlayer().getUuid())) {
                continue;
            }
            ServerPlayNetworking.send(instance.getPlayer(), new PlayerListSinglePacket(fakePlayer.getUuid(), new GameProfile(realPlayerUuid, fakePlayer.getName().getString()), fakePlayer.getName()));
        }
    }

    @Inject(method = "saveAllPlayerData", at = @At("TAIL"))
    private void saveAllPlayerDataMixin(CallbackInfo info) {
        for (UUID fakePlayerUuid : SitlogMain.SITLOG_PLAYER_MAP.values()) {
            if (this.playerMap.containsKey(fakePlayerUuid)) {
                this.savePlayerData(this.playerMap.get(fakePlayerUuid));
            }
        }
    }

    @Shadow
    protected void savePlayerData(ServerPlayerEntity player) {
    }

}
