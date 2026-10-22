package net.sitlog.init;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.sitlog.config.SitLogConfig;
import org.jetbrains.annotations.Nullable;

public class ConfigInit {

    public static SitLogConfig CONFIG = new SitLogConfig();

    @Nullable
    public static Boolean SYNCED_SITTING_PLAYER_CAN_JUMP = null;
    @Nullable
    public static Boolean SYNCED_ONLY_IN_COMBAT = null;

    public static void init(){
        AutoConfig.register(SitLogConfig.class, JanksonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(SitLogConfig.class).getConfig();
    }
}
