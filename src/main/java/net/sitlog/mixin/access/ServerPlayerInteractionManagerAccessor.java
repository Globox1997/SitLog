package net.sitlog.mixin.access;

import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerPlayerInteractionManager.class)
public interface ServerPlayerInteractionManagerAccessor {

    @Invoker("setGameMode")
    void callSetGameMode(GameMode gameMode, @Nullable GameMode previousGameMode);

}
