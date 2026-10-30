package net.sitlog.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity implements PlayerEntityAccess {

    @Unique
    private int despawnTimer = 0;
    @Unique
    private int combatTimer = 0;

    public PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
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

    @WrapOperation(method = "updatePlayerPose", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setPose(Lnet/minecraft/world/entity/Pose;)V"))
    private void updatePoseMixin(Player instance, Pose entityPose, Operation<Void> original) {
        original.call(instance, entityPose);
        if (!entityPose.equals(Pose.STANDING) && ((LivingEntityAccess) this).sitLog$getSitting() && (Object) this instanceof ServerPlayer serverPlayerEntity) {
            SitLogUtil.setSitting(serverPlayerEntity, false);
        }
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void damageMixin(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        if (info.getReturnValue() && ((LivingEntityAccess) this).sitLog$getSitting() && (Object) this instanceof ServerPlayer serverPlayerEntity && SitLogUtil.usesDespawnTimer(serverPlayerEntity)) {
            this.despawnTimer = ConfigInit.CONFIG.despawnTime;
            ServerPlayNetworking.send(serverPlayerEntity, new DespawnTimerPacket(ConfigInit.CONFIG.despawnTime));
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
