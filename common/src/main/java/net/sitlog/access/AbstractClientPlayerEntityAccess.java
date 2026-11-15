package net.sitlog.access;

import net.minecraft.client.multiplayer.PlayerInfo;
import org.jspecify.annotations.Nullable;

public interface AbstractClientPlayerEntityAccess {

    void setPlayerListEntry(@Nullable PlayerInfo playerListEntry);
}
