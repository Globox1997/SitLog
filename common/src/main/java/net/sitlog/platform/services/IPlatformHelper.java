package net.sitlog.platform.services;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public interface IPlatformHelper {

    ServerGamePacketListenerImpl createBodyConnection(ServerPlayer body);

}
