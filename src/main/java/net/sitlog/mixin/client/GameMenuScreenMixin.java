package net.sitlog.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.sitlog.access.PlayerEntityAccess;
import net.sitlog.init.ConfigInit;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(PauseScreen.class)
public abstract class GameMenuScreenMixin extends Screen {

    @Shadow
    @Nullable
    private Button disconnectButton;

    public GameMenuScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickMixin(CallbackInfo info) {
        if (this.minecraft != null && this.disconnectButton != null && !this.disconnectButton.active && this.minecraft.player != null && (((PlayerEntityAccess) this.minecraft.player).sitLog$getDespawnTimer() == 0 || !this.isInCombat())) {
            this.disconnectButton.active = true;
        }
    }

    @Inject(method = "createPauseMenu", at = @At("TAIL"))
    private void initWidgetsMixin(CallbackInfo info) {
        if (this.minecraft != null && this.disconnectButton != null && this.minecraft.player != null && !this.minecraft.hasSingleplayerServer() && !this.minecraft.player.canUseGameMasterBlocks() && this.isInCombat() && (((PlayerEntityAccess) this.minecraft.player).sitLog$getDespawnTimer() > 0 || ((PlayerEntityAccess) this.minecraft.player).sitLog$getDespawnTimer() == -1)) {
            this.disconnectButton.active = false;
        }
    }

    @Unique
    private boolean isInCombat() {
        return !Boolean.TRUE.equals(ConfigInit.SYNCED_ONLY_IN_COMBAT) || ((PlayerEntityAccess) this.minecraft.player).sitLog$getCombatTimer() > 0;
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void renderMixin(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo info) {
        if (this.minecraft != null && this.disconnectButton != null && this.minecraft.player != null && ((PlayerEntityAccess) this.minecraft.player).sitLog$getDespawnTimer() > 0) {
            context.text(this.font, Component.translatable("screen.sitlog.despawn_time", ((PlayerEntityAccess) this.minecraft.player).sitLog$getDespawnTimer() / 20), this.disconnectButton.getX() + 6 + ConfigInit.CONFIG.despawnTimeX, this.disconnectButton.getY() + 6 + ConfigInit.CONFIG.despawnTimeY, 0xFFFFFFFF, true);
        }
    }

}
