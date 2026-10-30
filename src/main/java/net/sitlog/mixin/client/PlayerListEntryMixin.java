package net.sitlog.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.sitlog.SitLogClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin(PlayerInfo.class)
public class PlayerListEntryMixin {

    @ModifyArg(method = "createSkinLookup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/SkinManager;createLookup(Lcom/mojang/authlib/GameProfile;Z)Ljava/util/function/Supplier;"), index = 1)
    private static boolean texturesSupplierMixin(boolean original, @Local(argsOnly = true) GameProfile profile) {
        if (original&&SitLogClient.CLIENT_PLAYER_MAP.containsKey(profile.id())) { // .inverse()
            return false;
        }
        return original;
    }

}
