package net.sitlog.mixin;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sitlog.SitlogMain;
import net.sitlog.access.LivingEntityAccess;
import net.sitlog.init.ConfigInit;
import net.sitlog.network.packet.SitPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "fall", at = @At("HEAD"))
    private void fallMixin(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition, CallbackInfo info) {
        if (SitlogMain.SITLOG_PLAYER_MAP.inverse().containsKey(this.getUuid())) {
            super.fall(heightDifference, onGround, state, landedPosition);
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
        if (info.getReturnValue()) {
            ((LivingEntityAccess) this).sitLog$setSitting(false);
            for (ServerPlayerEntity serverPlayerEntity : ((ServerPlayerEntity) (Object) this).getServerWorld().getPlayers()) {
                ServerPlayNetworking.send(serverPlayerEntity, new SitPacket(this.getId(), false, ConfigInit.CONFIG.despawnTime > 0 ? -1 : 0));
            }
            this.calculateDimensions();
        }
    }
}
