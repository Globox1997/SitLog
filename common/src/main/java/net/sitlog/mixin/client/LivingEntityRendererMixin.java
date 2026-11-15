package net.sitlog.mixin.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.sitlog.access.LivingEntityAccess;
import net.sitlog.access.LivingEntityRenderStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void extractRenderStateMixin(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo info) {
        ((LivingEntityRenderStateAccess) state).sitLog$setSitting(((LivingEntityAccess) entity).sitLog$getSitting());
    }
}
