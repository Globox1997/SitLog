package net.sitlog.mixin;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.sitlog.access.FakePlayerAccess;
import net.sitlog.mixin.access.PlayerEntityAccessor;
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
import java.util.List;
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
    private void isInvulnerableToMixin(ServerLevel level, DamageSource damageSource, CallbackInfoReturnable<Boolean> info) {
        if (SitLogUtil.isBody(this)) {
            info.setReturnValue(super.isInvulnerableTo(level, damageSource));
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

            if (!this.connection.hasClientLoaded()) {
                this.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            }

            if (this.getInvulnerableTime() > 0) {
                this.setInvulnerableTime(this.getInvulnerableTime() - 1);
            }

            if (!this.isRemoved()) {
                super.aiStep();
            }

            this.level().getChunkSource().move(this);

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
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float health = this.getHealth() + this.getAbsorptionAmount();
        boolean damaged = super.hurtServer(level, source, amount);
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
            SitLogUtil.saveBody(this.level().getServer(), this);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("SittingName", this.getName().getString());
        output.putByte("SittingModelParts", this.getEntityData().get(PlayerEntityAccessor.getPLAYER_MODEL_PARTS()));
        if (SitLogUtil.isBody(this)) {
            output.putBoolean("SittingBody", true);
        }
        output.putInt("SittingDespawnTimer", this.despawnTimer);
        output.putBoolean("SittingDespawned", this.despawned);
        if (this.deathMessage != null) {
            ComponentSerialization.CODEC.encodeStart(this.registryAccess().createSerializationContext(JsonOps.INSTANCE), this.deathMessage).result().ifPresent(json -> output.putString("SittingDeathMessage", json.toString()));
        }
        output.putFloat("SittingDamageTaken", this.damageTaken);
        output.store("SittingAttackers", Codec.STRING.listOf(), List.copyOf(this.attackers));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("SittingModelParts", Codec.BYTE).ifPresent(modelParts -> this.getEntityData().set(PlayerEntityAccessor.getPLAYER_MODEL_PARTS(), modelParts));
        this.despawnTimer = input.getIntOr("SittingDespawnTimer", 0);
        this.despawned = input.getBooleanOr("SittingDespawned", false);
        input.getString("SittingDeathMessage").ifPresent(json -> this.deathMessage = ComponentSerialization.CODEC.parse(this.registryAccess().createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(json)).result().orElse(null));
        this.damageTaken = input.getFloatOr("SittingDamageTaken", 0.0F);
        this.attackers.clear();
        input.read("SittingAttackers", Codec.STRING.listOf()).ifPresent(this.attackers::addAll);
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
