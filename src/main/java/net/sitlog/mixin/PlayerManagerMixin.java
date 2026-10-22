package net.sitlog.mixin;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sitlog.SitlogMain;
import net.sitlog.access.PlayerEntityAccess;
import net.sitlog.init.ConfigInit;
import net.sitlog.util.SitLogUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    @Final
    private Map<UUID, ServerPlayerEntity> playerMap;

    @Inject(method = "remove", at = @At("TAIL"))
    private void removeMixin(ServerPlayerEntity player, CallbackInfo info) {
        if (this.server.isHost(player.getGameProfile()) || player instanceof FakePlayer || !this.server.isRunning()) {
            return;
        }
        if (player.isSpectator() || player.isDead() || player.isCreativeLevelTwoOp()) {
            return;
        }
        if (ConfigInit.CONFIG.onlyInCombat && !SitLogUtil.isInCombat(player)) {
            return;
        }
        if (ConfigInit.CONFIG.despawnTime > 0 && ((PlayerEntityAccess) player).sitLog$getDespawnTimer() == 0) {
            return;
        }
        SitLogUtil.createBody(this.server, player);
    }

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void onPlayerConnectMixin(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo info) {
        SitLogUtil.restoreFromBody(this.server, player);
        SitLogUtil.sendBodies(this.server, player);
    }

    @Inject(method = "saveAllPlayerData", at = @At("TAIL"))
    private void saveAllPlayerDataMixin(CallbackInfo info) {
        for (UUID bodyUuid : SitlogMain.SITLOG_PLAYER_MAP.values()) {
            if (this.playerMap.get(bodyUuid) instanceof FakePlayer body) {
                SitLogUtil.saveBody(this.server, body);
            }
        }
    }

}
