package net.sitlog;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import net.sitlog.init.ConfigInit;
import net.sitlog.init.EventInit;
import net.sitlog.network.SitLogServerPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class SitlogMain implements ModInitializer {

    /**
     * Real Player UUID, Fake Player UUID
     */
    public static BiMap<UUID, UUID> SITLOG_PLAYER_MAP = HashBiMap.create();

    public static final String MOD_ID = "sitlog";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ConfigInit.init();
        SitLogServerPacket.init();
        EventInit.init();
    }

    public static Identifier identifierOf(String name) {
        return Identifier.of(MOD_ID, name);
    }
}