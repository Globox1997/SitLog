package net.sitlog;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.UUID;

public class SitLogClient {

    public static final BiMap<UUID, UUID> CLIENT_PLAYER_MAP = HashBiMap.create();

}
