package net.sitlog.neoforge.platform;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.sitlog.entity.SitLogBodyPacketListener;
import net.sitlog.platform.services.IPlatformHelper;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public ServerGamePacketListenerImpl createBodyConnection(ServerPlayer body) {
        return new NeoForgeBodyPacketListener(body);
    }

    private static final class NeoForgeBodyPacketListener extends SitLogBodyPacketListener {

        private NeoForgeBodyPacketListener(ServerPlayer body) {
            super(body);
        }

        @Override
        public boolean hasChannel(Identifier channel) {
            return false;
        }
    }

}
