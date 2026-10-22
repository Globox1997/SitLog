package net.sitlog.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
import net.sitlog.util.SitLogUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements PlayerEntityAccess {

    @Unique
    private int despawnTimer = 0;
    @Unique
    private int combatTimer = 0;

    @Unique
    private static final EntityDimensions SITTING_POSE = EntityDimensions.fixed(0.6F, 1.3F).withEyeHeight(1.12F);

    public PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickMixin(CallbackInfo info) {
        if (this.despawnTimer > 0 && ((LivingEntityAccess) this).sitLog$getSitting()) {
            this.despawnTimer--;
        }
        if (this.combatTimer > 0) {
            this.combatTimer--;
        }
    }

    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void getBaseDimensionsMixin(EntityPose pose, CallbackInfoReturnable<EntityDimensions> info) {
        if (((LivingEntityAccess) this).sitLog$getSitting()) {
            info.setReturnValue(SITTING_POSE);
        }
    }

    @WrapOperation(method = "updatePose", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setPose(Lnet/minecraft/entity/EntityPose;)V"))
    private void updatePoseMixin(PlayerEntity instance, EntityPose entityPose, Operation<Void> original) {
        original.call(instance, entityPose);
        if (!entityPose.equals(EntityPose.STANDING) && ((LivingEntityAccess) this).sitLog$getSitting() && (Object) this instanceof ServerPlayerEntity serverPlayerEntity) {
            SitLogUtil.setSitting(serverPlayerEntity, false);
        }
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void damageMixin(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        if (info.getReturnValue() && ((LivingEntityAccess) this).sitLog$getSitting() && (Object) this instanceof ServerPlayerEntity serverPlayerEntity && SitLogUtil.usesDespawnTimer(serverPlayerEntity)) {
            this.despawnTimer = ConfigInit.CONFIG.despawnTime;
            ServerPlayNetworking.send(serverPlayerEntity, new DespawnTimerPacket(ConfigInit.CONFIG.despawnTime));
        }
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void jumpMixin(CallbackInfo info) {
        if (((LivingEntityAccess) this).sitLog$getSitting()) {
            boolean canJump = this.getWorld().isClient() && ConfigInit.SYNCED_SITTING_PLAYER_CAN_JUMP != null ? ConfigInit.SYNCED_SITTING_PLAYER_CAN_JUMP : ConfigInit.CONFIG.sittingPlayerCanJump;
            if (!canJump) {
                info.cancel();
            }
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

    @Override
    public void sitLog$setCombatTimer(int combatTimer) {
        this.combatTimer = combatTimer;
    }

    @Override
    public int sitLog$getCombatTimer() {
        return this.combatTimer;
    }
}
