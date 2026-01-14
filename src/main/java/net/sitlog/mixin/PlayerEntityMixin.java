package net.sitlog.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.sitlog.access.LivingEntityAccess;
import net.sitlog.access.PlayerEntityAccess;
import net.sitlog.init.ConfigInit;
import net.sitlog.network.packet.DespawnTimerPacket;
import net.sitlog.network.packet.SitPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements PlayerEntityAccess {

    @Unique
    private int despawnTimer = 0;

    @Unique
    private static final EntityDimensions SITTING_POSE = EntityDimensions.fixed(0.6F, 1.3F).withEyeHeight(1.12F);

    public PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickMixin(CallbackInfo info) {
        if (this.despawnTimer > 0) {
            this.despawnTimer--;
        }
    }

    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void getBaseDimensionsMixin(EntityPose pose, CallbackInfoReturnable<EntityDimensions> info) {
        if (((LivingEntityAccess) this).sitLog$getSitting()) {
            info.setReturnValue(SITTING_POSE);
        }
    }

    @Inject(method = "updatePose", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setPose(Lnet/minecraft/entity/EntityPose;)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void updatePoseMixin(CallbackInfo info, EntityPose entityPose2) {
        if (!this.getWorld().isClient() && !entityPose2.equals(EntityPose.STANDING)) {
            ((LivingEntityAccess) this).sitLog$setSitting(false);
            ServerPlayNetworking.send((ServerPlayerEntity) (Object) this, new SitPacket(this.getId(), false, ConfigInit.CONFIG.despawnTime > 0 ? -1 : 0));
            this.calculateDimensions();
        }
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void damageMixin(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        if (!this.getWorld().isClient() && info.getReturnValue() && ((LivingEntityAccess) this).sitLog$getSitting() && ConfigInit.CONFIG.despawnTime > 0) {
            this.despawnTimer = ConfigInit.CONFIG.despawnTime;
            ServerPlayNetworking.send((ServerPlayerEntity) (Object) this, new DespawnTimerPacket(ConfigInit.CONFIG.despawnTime));
//            for (ServerPlayerEntity serverPlayerEntity : ((ServerPlayerEntity) (Object) this).getServerWorld().getPlayers()) {
//                ServerPlayNetworking.send(serverPlayerEntity, new SitPacket(this.getId(), true, ConfigInit.CONFIG.despawnTime));
//            }
        }
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void jumpMixin(CallbackInfo info) {
        if (((LivingEntityAccess) this).sitLog$getSitting() && !ConfigInit.CONFIG.sittingPlayerCanJump) {
            info.cancel();
        }
    }

    @Override
    public void sitLog$setDespawnTimer(int despawnTimer) {
        this.despawnTimer = despawnTimer;
    }

    @Override
    public int sitLog$getDespawnTimer() {
        return this.despawnTimer;
    }
}
