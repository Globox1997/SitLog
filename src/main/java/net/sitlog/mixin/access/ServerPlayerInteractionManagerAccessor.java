package net.sitlog.mixin.access;

import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerPlayerGameMode.class)
public interface ServerPlayerInteractionManagerAccessor {

    @Invoker("setGameModeForPlayer")
    void callSetGameMode(GameType gameMode, @Nullable GameType previousGameMode);

}
