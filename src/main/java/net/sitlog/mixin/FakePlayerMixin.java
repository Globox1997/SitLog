package net.sitlog.mixin;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
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

@Mixin(FakePlayer.class)
public abstract class FakePlayerMixin extends ServerPlayerEntity implements FakePlayerAccess {

    @Unique
    private int despawnTimer = 0;
    @Unique
    @Nullable
    private Text deathMessage = null;

    public FakePlayerMixin(MinecraftServer server, ServerWorld world, GameProfile profile, SyncedClientOptions clientOptions) {
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

            if (this.isDead()) {
                this.updatePostDeath();
                if (this.isRemoved()) {
                    SitLogUtil.unloadBody(this, Entity.RemovalReason.KILLED);
                }
                return;
            }

            if (((ServerPlayerEntityAccessor) this).getJoinInvulnerabilityTicks() > 0) {
                ((ServerPlayerEntityAccessor) this).setJoinInvulnerabilityTicks(0);
            }

            if (this.timeUntilRegen > 0) {
                this.timeUntilRegen--;
            }

            if (!this.isRemoved()) {
                super.tickMovement();
            }

            this.getServerWorld().getChunkManager().updatePosition(this);

            if (!this.getWorld().isClient() && this.despawnTimer > 0) {
                this.despawnTimer--;
                if (this.despawnTimer == 0) {
                    SitLogUtil.unloadBody(this, Entity.RemovalReason.DISCARDED);
                }
            }

//            this.tickFallStartPos();
        }
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        boolean body = SitLogUtil.isBody(this);
        if (body) {
            this.deathMessage = this.getDamageTracker().getDeathMessage();
        }
        super.onDeath(damageSource);
        if (body) {
            SitLogUtil.saveBody(this.server, this);
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("SittingName", this.getName().getString());
        nbt.putByte("SittingModelParts", this.getDataTracker().get(PlayerEntityAccessor.getPLAYER_MODEL_PARTS()));
        if (SitLogUtil.isBody(this)) {
            nbt.putBoolean("SittingBody", true);
        }
        if (this.deathMessage != null) {
            nbt.putString("SittingDeathMessage", Text.Serialization.toJsonString(this.deathMessage, this.getRegistryManager()));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("SittingModelParts")) {
            this.getDataTracker().set(PlayerEntityAccessor.getPLAYER_MODEL_PARTS(), nbt.getByte("SittingModelParts"));
        }
        if (nbt.contains("SittingDeathMessage")) {
            this.deathMessage = Text.Serialization.fromJson(nbt.getString("SittingDeathMessage"), this.getRegistryManager());
        }
    }

    @Override
    public void sitLog$setBodyDespawnTimer(int despawnTimer) {
        this.despawnTimer = despawnTimer;
    }

    @Nullable
    @Override
    public Text sitLog$getDeathMessage() {
        return this.deathMessage;
    }
}
