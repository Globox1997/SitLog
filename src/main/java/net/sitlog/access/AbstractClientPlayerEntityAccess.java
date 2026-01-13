package net.sitlog.access;

import net.minecraft.client.network.PlayerListEntry;
import org.jetbrains.annotations.Nullable;

public interface AbstractClientPlayerEntityAccess {

    void setPlayerListEntry(@Nullable PlayerListEntry playerListEntry);
}
