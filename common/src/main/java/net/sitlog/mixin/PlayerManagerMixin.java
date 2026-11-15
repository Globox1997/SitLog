package net.sitlog.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.sitlog.SitlogMain;
import net.sitlog.access.PlayerEntityAccess;
import net.sitlog.entity.SitLogBody;
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

@Mixin(PlayerList.class)
public class PlayerManagerMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    @Final
    private Map<UUID, ServerPlayer> playersByUUID;

    @Inject(method = "remove", at = @At("TAIL"))
    private void removeMixin(ServerPlayer player, CallbackInfo info) {
        if (this.server.isSingleplayerOwner(new NameAndId(player.getGameProfile())) || player instanceof SitLogBody || !this.server.isRunning()) {
            return;
        }
        if (player.isSpectator() || player.isDeadOrDying() || player.canUseGameMasterBlocks()) {
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

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void onPlayerConnectMixin(Connection connection, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo info) {
        SitLogUtil.restoreFromBody(this.server, player);
        SitLogUtil.sendBodies(this.server, player);
    }

    @Inject(method = "saveAll", at = @At("TAIL"))
    private void saveAllPlayerDataMixin(CallbackInfo info) {
        for (UUID bodyUuid : SitlogMain.SITLOG_PLAYER_MAP.values()) {
            if (this.playersByUUID.get(bodyUuid) instanceof SitLogBody body) {
                SitLogUtil.saveBody(this.server, body);
            }
        }
    }

}
