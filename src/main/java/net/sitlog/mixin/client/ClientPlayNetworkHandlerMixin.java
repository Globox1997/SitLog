package net.sitlog.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
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

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Shadow
    @Mutable
    @Final
    private Map<UUID, PlayerListEntry> playerListEntries;

    // Both mixins might not be required anymore
    @ModifyExpressionValue(method = "createEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;getPlayerListEntry(Ljava/util/UUID;)Lnet/minecraft/client/network/PlayerListEntry;"))
    private PlayerListEntry createEntityMixin(PlayerListEntry playerListEntry, @Local(argsOnly = true) EntitySpawnS2CPacket packet) {
        if (playerListEntry == null) {
            if (SitLogClient.CLIENT_PLAYER_MAP.inverse().containsKey(packet.getUuid())) {
                UUID playerUuid = SitLogClient.CLIENT_PLAYER_MAP.inverse().get(packet.getUuid());
                if (playerListEntries.containsKey(playerUuid)) {
                    return playerListEntries.get(playerUuid);
                }
            }
        }
        return playerListEntry;
    }

    @ModifyExpressionValue(method = "onPlayerList", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/PlayerListS2CPacket$Entry;profileId()Ljava/util/UUID;"))
    private UUID onPlayerListMixin(UUID profileId) {
        if (SitLogClient.CLIENT_PLAYER_MAP.inverse().containsKey(profileId)) {
            UUID playerUuid = SitLogClient.CLIENT_PLAYER_MAP.inverse().get(profileId);
            if (playerListEntries.containsKey(playerUuid)) {
                return playerUuid;
            }
        }
        return profileId;
    }

    @Inject(method = "onPlayerRemove", at = @At("TAIL"))
    private void onPlayerRemoveMixin(PlayerRemoveS2CPacket packet, CallbackInfo info) {
        for (UUID uuid : packet.profileIds()) {
            SitLogClient.CLIENT_PLAYER_MAP.inverse().remove(uuid);
        }
    }

}
