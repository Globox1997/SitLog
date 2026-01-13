package net.sitlog.mixin.access;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PlayerSaveHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {

    @Accessor("saveHandler")
    PlayerSaveHandler getSaveHandler();
}
