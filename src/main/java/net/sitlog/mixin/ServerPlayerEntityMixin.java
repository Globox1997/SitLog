package net.sitlog.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.sitlog.access.LivingEntityAccess;
import net.sitlog.util.SitLogUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin extends Player {

    public ServerPlayerEntityMixin(Level world, GameProfile gameProfile) {
        super(world, gameProfile);
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
    private void startRidingMixin(Entity entity, boolean force, boolean sendEventAndTriggers, CallbackInfoReturnable<Boolean> info) {
        if (info.getReturnValue() && ((LivingEntityAccess) this).sitLog$getSitting()) {
            SitLogUtil.setSitting((ServerPlayer) (Object) this, false);
        }
    }
}
