package net.sitlog.init;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.sitlog.SitlogMain;
import net.sitlog.access.LivingEntityAccess;
import net.sitlog.network.packet.SitPacket;
import net.sitlog.platform.Services;

public class KeybindInit {

    public static final Identifier CATEGORY_ID = SitlogMain.identifierOf("keybind");

    public static KeyMapping sitBinding;

    public static KeyMapping createKeyMapping(KeyMapping.Category category) {
        sitBinding = new KeyMapping("key.sitlog.sit", InputConstants.Type.KEYBOARD, InputConstants.KEY_LALT, category);
        return sitBinding;
    }

    public static void onClientTick(Minecraft client) {
        if (sitBinding != null && sitBinding.consumeClick()) {
            if (client.player != null && !client.player.isHandsBusy()) {
                Services.network().sendToServer(new SitPacket(client.player.getId(), !((LivingEntityAccess) client.player).sitLog$getSitting(), 0));
            }
        }
    }

}
