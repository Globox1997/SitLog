package net.sitlog;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.sitlog.init.KeybindInit;
import net.sitlog.network.SitLogClientPacket;

@Environment(EnvType.CLIENT)
public class SitLogClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SitLogClientPacket.init();
        KeybindInit.init();
    }
}
