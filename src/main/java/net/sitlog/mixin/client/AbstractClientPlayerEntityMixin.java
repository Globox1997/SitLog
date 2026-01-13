package net.sitlog.mixin.client;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sitlog.SitlogMain;
import net.sitlog.access.AbstractClientPlayerEntityAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin extends PlayerEntity implements AbstractClientPlayerEntityAccess {

    @Shadow
    @Nullable
    private PlayerListEntry playerListEntry;

    public AbstractClientPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "getPlayerListEntry", at = @At("RETURN"))
    private void getPlayerListEntryMixin(CallbackInfoReturnable<PlayerListEntry> info) {
        if (this.playerListEntry == null) {
            if (SitlogMain.SITLOG_PLAYER_MAP.inverse().containsKey(this.getUuid())) {
                this.playerListEntry = MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(SitlogMain.SITLOG_PLAYER_MAP.inverse().get(this.getUuid()));
            }
        }
    }

    @Override
    public void setPlayerListEntry(@Nullable PlayerListEntry playerListEntry) {
        if (playerListEntry != null) {
            this.playerListEntry = playerListEntry;
        }
    }
}
