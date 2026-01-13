package net.sitlog.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "sitlog")
@Config.Gui.Background("minecraft:textures/block/stone.png")
public class SitLogConfig implements ConfigData {

    @Comment("Keep sitting players after restart of server")
    public boolean keepAfterRestart = true;
    @Comment("Player will get despawned after this time if >0. Time in ticks.")
    public int despawnTime = 0;
    @Comment("Despawn time screen position")
    public int despawnTimeX = 0;
    public int despawnTimeY = 0;
}
