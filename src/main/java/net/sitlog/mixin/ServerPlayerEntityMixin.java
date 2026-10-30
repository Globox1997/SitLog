package net.sitlog.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.sitlog.SitlogMain;
import net.sitlog.access.LivingEntityAccess;
import net.sitlog.util.SitLogUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin extends Player {

    public ServerPlayerEntityMixin(Level world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "checkFallDamage", at = @At("HEAD"))
    private void fallMixin(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition, CallbackInfo info) {
        if (SitlogMain.SITLOG_PLAYER_MAP.inverse().containsKey(this.getUUID())) {
            super.checkFallDamage(heightDifference, onGround, state, landedPosition);
        }
    }

    // TODO: MIGHT NEED TO GET REMOVED CAUSE OF USELESS
//    @Inject(method = "onDeath",at = @At("HEAD"))
//    private void onDeathMixin(DamageSource damageSource, CallbackInfo info){
//        if((Object) this instanceof FakePlayer && SitlogMain.SITLOG_PLAYER_MAP.inverse().containsKey(this.getUuid())){
//            ((LivingEntityAccess) this).sitLog$setSitting(false);
//            ServerPlayNetworking.send((ServerPlayerEntity) (Object) this, new SitPacket(this.getId(), false, -1));
//            this.calculateDimensions();
//        }
//    }

    @Inject(method = "startRiding", at = @At("RETURN"))
    private void startRidingMixin(Entity entity, boolean force, CallbackInfoReturnable<Boolean> info) {
        if (info.getReturnValue() && ((LivingEntityAccess) this).sitLog$getSitting()) {
            SitLogUtil.setSitting((ServerPlayer) (Object) this, false);
        }
    }
}
