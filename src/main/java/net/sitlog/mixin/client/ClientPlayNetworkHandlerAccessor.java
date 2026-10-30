package net.sitlog.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
@Mixin(ClientPacketListener.class)
public interface ClientPlayNetworkHandlerAccessor {
    @Accessor("playerInfoMap")
    Map<UUID, PlayerInfo> getPlayerListEntries();
}
