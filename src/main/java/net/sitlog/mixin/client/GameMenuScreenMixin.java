package net.sitlog.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.sitlog.access.PlayerEntityAccess;
import net.sitlog.init.ConfigInit;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {

    @Shadow
    @Nullable
    private ButtonWidget exitButton;

    public GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickMixin(CallbackInfo info) {
        if (this.client != null && this.exitButton != null && !this.exitButton.active && this.client.player != null && ((PlayerEntityAccess) this.client.player).sitLog$getDespawnTimer() == 0) {
            this.exitButton.active = true;
        }
    }

    @Inject(method = "initWidgets", at = @At("TAIL"))
    private void initWidgetsMixin(CallbackInfo info) {
        if (this.client != null && this.exitButton != null && this.client.player != null && !this.client.player.isCreativeLevelTwoOp() && (((PlayerEntityAccess) this.client.player).sitLog$getDespawnTimer() > 0 || ((PlayerEntityAccess) this.client.player).sitLog$getDespawnTimer() == -1)) {
            this.exitButton.active = false;
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderMixin(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
        if (this.client != null && this.exitButton != null && this.client.player != null && ((PlayerEntityAccess) this.client.player).sitLog$getDespawnTimer() > 0) {
            context.drawText(this.textRenderer, Text.translatable("screen.sitlog.despawn_time", ((PlayerEntityAccess) this.client.player).sitLog$getDespawnTimer() / 20), this.exitButton.getX() + 6 + ConfigInit.CONFIG.despawnTimeX, this.exitButton.getY() + 6 + ConfigInit.CONFIG.despawnTimeY, 16777215, true);
        }
    }

}
