package net.sitlog.mixin;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.sitlog.SitlogMain;
import net.sitlog.access.FakePlayerAccess;
import net.sitlog.mixin.access.ServerPlayerEntityAccessor;
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

    public FakePlayerMixin(MinecraftServer server, ServerWorld world, GameProfile profile, SyncedClientOptions clientOptions) {
        super(server, world, profile, clientOptions);
    }

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void isInvulnerableToMixin(DamageSource damageSource, CallbackInfoReturnable<Boolean> info) {
        if (SitlogMain.SITLOG_PLAYER_MAP.inverse().containsKey(this.getUuid())) {
            info.setReturnValue(super.isInvulnerableTo(damageSource));
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickMixin(CallbackInfo info) {
        if (SitlogMain.SITLOG_PLAYER_MAP.inverse().containsKey(this.getUuid())) {

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
                    this.discard();
                }
            }

//            this.tickFallStartPos();
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("SittingName", this.getName().getString());
    }

    @Override
    public void sitLog$setDespawnTimer(int despawnTimer) {
        this.despawnTimer = despawnTimer;
    }
}
