package net.sitlog.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.sitlog.SitLogClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {

    @Shadow
    @Mutable
    @Final
    private Map<UUID, PlayerInfo> playerInfoMap;

    // Both mixins might not be required anymore
    @ModifyExpressionValue(method = "createEntityFromPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;getPlayerInfo(Ljava/util/UUID;)Lnet/minecraft/client/multiplayer/PlayerInfo;"))
    private PlayerInfo createEntityMixin(PlayerInfo playerListEntry, @Local(argsOnly = true) ClientboundAddEntityPacket packet) {
        if (playerListEntry == null) {
            if (SitLogClient.CLIENT_PLAYER_MAP.inverse().containsKey(packet.getUUID())) {
                UUID playerUuid = SitLogClient.CLIENT_PLAYER_MAP.inverse().get(packet.getUUID());
                if (playerInfoMap.containsKey(playerUuid)) {
                    return playerInfoMap.get(playerUuid);
                }
            }
        }
        return playerListEntry;
    }

    @ModifyExpressionValue(method = "handlePlayerInfoUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket$Entry;profileId()Ljava/util/UUID;"))
    private UUID onPlayerListMixin(UUID profileId) {
        if (SitLogClient.CLIENT_PLAYER_MAP.inverse().containsKey(profileId)) {
            UUID playerUuid = SitLogClient.CLIENT_PLAYER_MAP.inverse().get(profileId);
            if (playerInfoMap.containsKey(playerUuid)) {
                return playerUuid;
            }
        }
        return profileId;
    }

    @Inject(method = "handlePlayerInfoRemove", at = @At("TAIL"))
    private void onPlayerRemoveMixin(ClientboundPlayerInfoRemovePacket packet, CallbackInfo info) {
        for (UUID uuid : packet.profileIds()) {
            SitLogClient.CLIENT_PLAYER_MAP.inverse().remove(uuid);
        }
    }

}
