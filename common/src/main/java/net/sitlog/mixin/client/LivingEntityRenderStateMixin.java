package net.sitlog.mixin.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.sitlog.access.LivingEntityRenderStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements LivingEntityRenderStateAccess {

    @Unique
    private boolean sitting = false;

    @Override
    public void sitLog$setSitting(boolean sitting) {
        this.sitting = sitting;
    }

    @Override
    public boolean sitLog$isSitting() {
        return this.sitting;
    }
}
