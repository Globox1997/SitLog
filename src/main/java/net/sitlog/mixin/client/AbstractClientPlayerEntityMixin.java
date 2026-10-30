package net.sitlog.mixin.client;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.sitlog.SitLogClient;
import net.sitlog.access.AbstractClientPlayerEntityAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerEntityMixin extends Player implements AbstractClientPlayerEntityAccess {

    @Shadow
    @Nullable
    private PlayerInfo playerInfo;

    public AbstractClientPlayerEntityMixin(Level world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "getPlayerInfo", at = @At("RETURN"))
    private void getPlayerListEntryMixin(CallbackInfoReturnable<PlayerInfo> info) {
        if (this.playerInfo == null) {
            if (SitLogClient.CLIENT_PLAYER_MAP.inverse().containsKey(this.getUUID())) {
                this.playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(SitLogClient.CLIENT_PLAYER_MAP.inverse().get(this.getUUID()));
            }
        }
    }

    @Override
    public void setPlayerListEntry(@Nullable PlayerInfo playerInfo) {
        if (playerInfo != null) {
            this.playerInfo = playerInfo;
        }
    }
}
