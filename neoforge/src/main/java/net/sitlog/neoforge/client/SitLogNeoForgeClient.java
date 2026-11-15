package net.sitlog.neoforge.client;

import me.shedaniel.autoconfig.AutoConfigClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.sitlog.SitlogMain;
import net.sitlog.config.SitLogConfig;
import net.sitlog.init.KeybindInit;
import net.sitlog.network.SitLogClientPacket;
import net.sitlog.network.packet.*;

@Mod(value = SitlogMain.MOD_ID, dist = Dist.CLIENT)
public class SitLogNeoForgeClient {

    public SitLogNeoForgeClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(RegisterClientPayloadHandlersEvent.class, SitLogNeoForgeClient::registerPayloadHandlers);
        modBus.addListener(RegisterKeyMappingsEvent.class, SitLogNeoForgeClient::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> KeybindInit.onClientTick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> SitLogClientPacket.onDisconnect());

        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> AutoConfigClient.getConfigScreen(SitLogConfig.class, parent).get());
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(PlayerListPacket.PACKET_ID, (payload, context) -> SitLogClientPacket.handlePlayerList(payload));
        event.register(PlayerListSinglePacket.PACKET_ID, (payload, context) -> SitLogClientPacket.handlePlayerListSingle(payload));
        event.register(SitPacket.PACKET_ID, (payload, context) -> SitLogClientPacket.handleSit(payload));
        event.register(FakePlayerPacket.PACKET_ID, (payload, context) -> SitLogClientPacket.handleFakePlayer(payload));
        event.register(DespawnTimerPacket.PACKET_ID, (payload, context) -> SitLogClientPacket.handleDespawnTimer(payload));
        event.register(ConfigSyncPacket.PACKET_ID, (payload, context) -> SitLogClientPacket.handleConfigSync(payload));
        event.register(CombatTagPacket.PACKET_ID, (payload, context) -> SitLogClientPacket.handleCombatTag(payload));
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        KeyMapping.Category category = new KeyMapping.Category(KeybindInit.CATEGORY_ID);
        event.registerCategory(category);
        event.register(KeybindInit.createKeyMapping(category));
    }

}
