package net.sitlog.mixin.access;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.ServerStatsCounter;

@Mixin(PlayerList.class)
public interface PlayerManagerAccessor {

    @Accessor("playersByUUID")
    Map<UUID, ServerPlayer> getPlayerMap();

    @Accessor("stats")
    Map<UUID, ServerStatsCounter> getStatisticsMap();

    @Accessor("advancements")
    Map<UUID, PlayerAdvancements> getAdvancementTrackers();

}
