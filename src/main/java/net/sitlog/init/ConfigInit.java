package net.sitlog.init;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.sitlog.config.SitLogConfig;

public class ConfigInit {

    public static SitLogConfig CONFIG = new SitLogConfig();

    public static void init(){
        AutoConfig.register(SitLogConfig.class, JanksonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(SitLogConfig.class).getConfig();
    }
}
