package net.sitlog.mixin.access;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayerEntity.class)
public interface ServerPlayerEntityAccessor {

    @Accessor("joinInvulnerabilityTicks")
    int getJoinInvulnerabilityTicks();

    @Accessor("joinInvulnerabilityTicks")
    void setJoinInvulnerabilityTicks(int joinInvulnerabilityTicks);
}
