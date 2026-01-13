package net.sitlog.init;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.sitlog.access.LivingEntityAccess;
import net.sitlog.network.packet.SitPacket;
import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

@Environment(EnvType.CLIENT)
public class KeybindInit {

    public static KeyBinding sitBinding;

    public static void init() {
        // Keybinds
        sitBinding = new KeyBinding("key.sitlog.sit", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "category.sitlog.keybind");
        // Registering
        KeyBindingHelper.registerKeyBinding(sitBinding);
        // Callback
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (sitBinding.wasPressed()) {
                if (client.player != null && !client.player.isRiding()) {
                    ClientPlayNetworking.send(new SitPacket(client.player.getId(), !((LivingEntityAccess) client.player).sitLog$getSitting(), 0));
                    return;
                }
            }
        });
    }

}
