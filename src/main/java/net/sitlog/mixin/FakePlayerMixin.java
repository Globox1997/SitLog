package net.sitlog.mixin;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.sitlog.access.FakePlayerAccess;
import net.sitlog.mixin.access.PlayerEntityAccessor;
import net.sitlog.mixin.access.ServerPlayerEntityAccessor;
import net.sitlog.util.SitLogUtil;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(FakePlayer.class)
public abstract class FakePlayerMixin extends ServerPlayer implements FakePlayerAccess {

    @Unique
    private int despawnTimer = 0;
    @Unique
    private boolean despawned = false;
    @Unique
    @Nullable
    private Component deathMessage = null;
    @Unique
    private float damageTaken = 0.0F;
    @Unique
    private final Set<String> attackers = new LinkedHashSet<>();

    public FakePlayerMixin(MinecraftServer server, ServerLevel world, GameProfile profile, ClientInformation clientOptions) {
        super(server, world, profile, clientOptions);
    }

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void isInvulnerableToMixin(DamageSource damageSource, CallbackInfoReturnable<Boolean> info) {
        if (SitLogUtil.isBody(this)) {
            info.setReturnValue(super.isInvulnerableTo(damageSource));
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickMixin(CallbackInfo info) {
        if (SitLogUtil.isBody(this)) {

            if (this.isDeadOrDying()) {
                this.tickDeath();
                if (this.isRemoved()) {
                    SitLogUtil.unloadBody(this, Entity.RemovalReason.KILLED);
                }
                return;
            }

            if (((ServerPlayerEntityAccessor) this).getJoinInvulnerabilityTicks() > 0) {
                ((ServerPlayerEntityAccessor) this).setJoinInvulnerabilityTicks(0);
            }

            if (this.invulnerableTime > 0) {
                this.invulnerableTime--;
            }

            if (!this.isRemoved()) {
                super.aiStep();
            }

            this.serverLevel().getChunkSource().move(this);

            if (!this.level().isClientSide() && this.despawnTimer > 0) {
                this.despawnTimer--;
                if (this.despawnTimer == 0) {
                    this.despawned = true;
                    SitLogUtil.unloadBody(this, Entity.RemovalReason.DISCARDED);
                }
            }

//            this.tickFallStartPos();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        float health = this.getHealth() + this.getAbsorptionAmount();
        boolean damaged = super.hurt(source, amount);
        if (damaged && SitLogUtil.isBody(this)) {
            this.damageTaken += Math.max(0.0F, health - this.getHealth() - this.getAbsorptionAmount());
            if (source.getEntity() instanceof Player attacker) {
                this.attackers.add(attacker.getName().getString());
            }
        }
        return damaged;
    }

    @Override
    public void die(DamageSource damageSource) {
        boolean body = SitLogUtil.isBody(this);
        if (body) {
            this.deathMessage = this.getCombatTracker().getDeathMessage();
        }
        super.die(damageSource);
        if (body) {
            SitLogUtil.saveBody(this.server, this);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("SittingName", this.getName().getString());
        nbt.putByte("SittingModelParts", this.getEntityData().get(PlayerEntityAccessor.getPLAYER_MODEL_PARTS()));
        if (SitLogUtil.isBody(this)) {
            nbt.putBoolean("SittingBody", true);
        }
        nbt.putInt("SittingDespawnTimer", this.despawnTimer);
        nbt.putBoolean("SittingDespawned", this.despawned);
        if (this.deathMessage != null) {
            nbt.putString("SittingDeathMessage", Component.Serializer.toJson(this.deathMessage, this.registryAccess()));
        }
        nbt.putFloat("SittingDamageTaken", this.damageTaken);
        ListTag attackerList = new ListTag();
        for (String attacker : this.attackers) {
            attackerList.add(StringTag.valueOf(attacker));
        }
        nbt.put("SittingAttackers", attackerList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("SittingModelParts")) {
            this.getEntityData().set(PlayerEntityAccessor.getPLAYER_MODEL_PARTS(), nbt.getByte("SittingModelParts"));
        }
        this.despawnTimer = nbt.getInt("SittingDespawnTimer");
        this.despawned = nbt.getBoolean("SittingDespawned");
        if (nbt.contains("SittingDeathMessage")) {
            this.deathMessage = Component.Serializer.fromJson(nbt.getString("SittingDeathMessage"), this.registryAccess());
        }
        this.damageTaken = nbt.getFloat("SittingDamageTaken");
        this.attackers.clear();
        ListTag attackerList = nbt.getList("SittingAttackers", Tag.TAG_STRING);
        for (int i = 0; i < attackerList.size(); i++) {
            this.attackers.add(attackerList.getString(i));
        }
    }

    @Override
    public void sitLog$setBodyDespawnTimer(int despawnTimer) {
        this.despawnTimer = despawnTimer;
    }

    @Override
    public boolean sitLog$isDespawned() {
        return this.despawned;
    }

    @Nullable
    @Override
    public Component sitLog$getDeathMessage() {
        return this.deathMessage;
    }

    @Override
    public float sitLog$getDamageTaken() {
        return this.damageTaken;
    }

    @Override
    public Collection<String> sitLog$getAttackers() {
        return this.attackers;
    }
}
