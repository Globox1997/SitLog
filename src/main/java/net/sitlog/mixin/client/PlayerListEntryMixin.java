package net.sitlog.mixin.client;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.PlayerListEntry;
import net.sitlog.SitLogClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(PlayerListEntry.class)
public class PlayerListEntryMixin {

    @ModifyVariable(method = "texturesSupplier", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/DefaultSkinHelper;getSkinTextures(Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/client/util/SkinTextures;"))
    private static boolean texturesSupplierMixin(boolean original, GameProfile profile) {
        if (original&&SitLogClient.CLIENT_PLAYER_MAP.containsKey(profile.getId())) { // .inverse()
            return false;
        }
        return original;
    }

}
