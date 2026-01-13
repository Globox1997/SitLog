package net.sitlog.mixin;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.c2s.config.ReadyC2SPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.sitlog.SitlogMain;
import net.sitlog.mixin.access.PlayerManagerAccessor;
import net.sitlog.mixin.access.ServerPlayerInteractionManagerAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.UUID;

@Mixin(ServerConfigurationNetworkHandler.class)
public class ServerConfigurationNetworkHandlerMixin {

    @Shadow
    @Mutable
    @Final
    private GameProfile profile;

//    @Inject(method = "onReady", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;getPlayer(Ljava/util/UUID;)Lnet/minecraft/server/network/ServerPlayerEntity;"), locals = LocalCapture.CAPTURE_FAILSOFT)
//    private void onReadyMixin(ReadyC2SPacket packet, CallbackInfo info, PlayerManager playerManager) {
//        if (playerManager instanceof PlayerManagerAccessor playerManagerAccessor) {
//            if (SitlogMain.SITLOG_PLAYER_MAP.containsKey(this.profile.getId())) {
//                UUID uuid = SitlogMain.SITLOG_PLAYER_MAP.get(this.profile.getId());
//                if (playerManager.getPlayer(uuid) instanceof FakePlayer fakePlayer) {
//
////                    playerManagerAccessor.getPlayers().remove(fakePlayer);
////                    playerManagerAccessor.getPlayerMap().remove(fakePlayer.getUuid());
////                    fakePlayer.getServerWorld().removePlayer(fakePlayer, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
////                this.sendToAll(new PlayerRemoveS2CPacket(List.of(player.getUuid())));
//                }
//            }
//        }
//    }

//    @Inject(method = "onReady", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
//    private void onReadyRemovePlayerMixin(ReadyC2SPacket packet, CallbackInfo info, PlayerManager playerManager, Text text, ServerPlayerEntity serverPlayerEntity) {
//        if (playerManager instanceof PlayerManagerAccessor playerManagerAccessor) {
//            if (SitlogMain.SITLOG_PLAYER_MAP.containsKey(this.profile.getId())) {
//                UUID uuid = SitlogMain.SITLOG_PLAYER_MAP.get(this.profile.getId());
//                if (playerManager.getPlayer(uuid) instanceof FakePlayer fakePlayer) {
////                    playerManager.sendToAll(new PlayerRemoveS2CPacket(List.of(serverPlayerEntity.getUuid())));
////                    playerManager.sendToAll(new PlayerRemoveS2CPacket(List.of(fakePlayer.getUuid())));
//                }
//            }
//        }
//    }


    @Inject(method = "onReady", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onReadyMixin(ReadyC2SPacket packet, CallbackInfo info, PlayerManager playerManager, Text text, ServerPlayerEntity serverPlayerEntity) {
        if (playerManager instanceof PlayerManagerAccessor playerManagerAccessor) {
            if (SitlogMain.SITLOG_PLAYER_MAP.containsKey(this.profile.getId())) {
                UUID uuid = SitlogMain.SITLOG_PLAYER_MAP.get(this.profile.getId());
                if (playerManager.getPlayer(uuid) instanceof FakePlayer fakePlayer) {

                    serverPlayerEntity.getInventory().clone(fakePlayer.getInventory());
                    serverPlayerEntity.setHealth(fakePlayer.getHealth());

                    serverPlayerEntity.setAir(fakePlayer.getAir());
                    serverPlayerEntity.setFireTicks(fakePlayer.getFireTicks());
                    serverPlayerEntity.setGlowing(fakePlayer.isGlowing());
                    serverPlayerEntity.setInvulnerable(fakePlayer.isInvulnerable());
                    ((ServerPlayerInteractionManagerAccessor)serverPlayerEntity.interactionManager).callSetGameMode(fakePlayer.interactionManager.getGameMode(), fakePlayer.interactionManager.getPreviousGameMode());

                    for (StatusEffectInstance statusEffectInstance : fakePlayer.getStatusEffects()) {
                        serverPlayerEntity.addStatusEffect(new StatusEffectInstance(statusEffectInstance));
                    }
                    serverPlayerEntity.experienceLevel = fakePlayer.experienceLevel;
                    serverPlayerEntity.totalExperience = fakePlayer.totalExperience;
                    serverPlayerEntity.experienceProgress = fakePlayer.experienceProgress;
                    serverPlayerEntity.setScore(fakePlayer.getScore());
                    serverPlayerEntity.portalManager = fakePlayer.portalManager;

                    serverPlayerEntity.teleport(fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(), false);

                    serverPlayerEntity.setPosition(fakePlayer.getPos());
                    serverPlayerEntity.refreshPositionAndAngles(fakePlayer.getPos(), fakePlayer.getYaw(), fakePlayer.getPitch());

                    playerManagerAccessor.getPlayers().remove(fakePlayer);
                    playerManagerAccessor.getPlayerMap().remove(fakePlayer.getUuid());

                    serverPlayerEntity.getServerWorld().removePlayer(fakePlayer, Entity.RemovalReason.UNLOADED_WITH_PLAYER);

                    // USED TO WORK BUT MIGHT LeAD TO ISSUeS
                    // serverPlayerEntity.networkHandler.sendPacket(new PlayerRemoveS2CPacket(List.of(serverPlayerEntity.getUuid())));

                }
            }
        }
    }
}
