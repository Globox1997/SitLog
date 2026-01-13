package net.sitlog.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.sitlog.SitlogMain;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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
    @ModifyVariable(method = "createEntity", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;getPlayerListEntry(Ljava/util/UUID;)Lnet/minecraft/client/network/PlayerListEntry;"), index = 3)
    private PlayerListEntry createEntityMixin(PlayerListEntry playerListEntry, EntitySpawnS2CPacket packet) {
        if (playerListEntry == null) {
            if (SitlogMain.SITLOG_PLAYER_MAP.inverse().containsKey(packet.getUuid())) {
                UUID playerUuid = SitlogMain.SITLOG_PLAYER_MAP.inverse().get(packet.getUuid());
                if (playerListEntries.containsKey(playerUuid)) {
                    return playerListEntries.get(playerUuid);
                }
            }
        }
        return playerListEntry;
    }

    @ModifyVariable(method = "onPlayerList", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;playerListEntries:Ljava/util/Map;"), index = 3)
    private PlayerListS2CPacket.Entry createEntityMixin(PlayerListS2CPacket.Entry value) {
        if (SitlogMain.SITLOG_PLAYER_MAP.inverse().containsKey(value.profileId())) {
            UUID playerUuid = SitlogMain.SITLOG_PLAYER_MAP.inverse().get(value.profileId());
            if (playerListEntries.containsKey(playerUuid)) {
                return new PlayerListS2CPacket.Entry(playerUuid, value.profile(), value.listed(), value.latency(), value.gameMode(), value.displayName(), value.chatSession());
            }
        }

        return value;
    }

}
