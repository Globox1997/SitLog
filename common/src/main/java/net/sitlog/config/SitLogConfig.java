package net.sitlog.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "sitlog")
@Config.Gui.Background("minecraft:textures/block/stone.png")
public class SitLogConfig implements ConfigData {

    @Comment("Keep sitting players after restart of server")
    public boolean keepAfterRestart = true;
    public boolean sittingPlayerCanJump = false;
    @Comment("Player has to sit this long to leave without a body if >0. Time in ticks.")
    public int despawnTime = 0;
    @Comment("Body will get despawned after this time if >0. Time in ticks.")
    public int bodyLifetime = 0;
    @Comment("Only leave a body behind if the player was in combat")
    public boolean onlyInCombat = false;
    @Comment("Player stays in combat this long after hitting or getting hit. Time in ticks.")
    public int combatTagTime = 300;
    @Comment("Only fights between players count as combat")
    public boolean combatOnlyAgainstPlayers = true;
    @Comment("Despawn time screen position")
    public int despawnTimeX = 0;
    public int despawnTimeY = 0;
}
