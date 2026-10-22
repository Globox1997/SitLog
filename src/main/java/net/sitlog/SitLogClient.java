package net.sitlog;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.sitlog.init.KeybindInit;
import net.sitlog.network.SitLogClientPacket;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public class SitLogClient implements ClientModInitializer {

    public static final BiMap<UUID, UUID> CLIENT_PLAYER_MAP = HashBiMap.create();

    @Override
    public void onInitializeClient() {
        SitLogClientPacket.init();
        KeybindInit.init();
    }
}
