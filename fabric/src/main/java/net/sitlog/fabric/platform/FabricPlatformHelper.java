package net.sitlog.fabric.platform;

import net.fabricmc.fabric.impl.networking.UntrackedPacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.sitlog.entity.SitLogBodyPacketListener;
import net.sitlog.platform.services.IPlatformHelper;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public ServerGamePacketListenerImpl createBodyConnection(ServerPlayer body) {
        return new FabricBodyPacketListener(body);
    }

    private static final class FabricBodyPacketListener extends SitLogBodyPacketListener implements UntrackedPacketListener {

        private FabricBodyPacketListener(ServerPlayer body) {
            super(body);
        }
    }

}
