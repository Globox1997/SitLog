package net.sitlog.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.sitlog.init.KeybindInit;
import net.sitlog.network.SitLogClientPacket;
import net.sitlog.network.packet.*;

@Environment(EnvType.CLIENT)
public class SitLogFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PlayerListPacket.PACKET_ID, (payload, context) -> SitLogClientPacket.handlePlayerList(payload));
        ClientPlayNetworking.registerGlobalReceiver(PlayerListSinglePacket.PACKET_ID, (payload, context) -> SitLogClientPacket.handlePlayerListSingle(payload));
        ClientPlayNetworking.registerGlobalReceiver(SitPacket.PACKET_ID, (payload, context) -> SitLogClientPacket.handleSit(payload));
        ClientPlayNetworking.registerGlobalReceiver(FakePlayerPacket.PACKET_ID, (payload, context) -> SitLogClientPacket.handleFakePlayer(payload));
        ClientPlayNetworking.registerGlobalReceiver(DespawnTimerPacket.PACKET_ID, (payload, context) -> SitLogClientPacket.handleDespawnTimer(payload));
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPacket.PACKET_ID, (payload, context) -> SitLogClientPacket.handleConfigSync(payload));
        ClientPlayNetworking.registerGlobalReceiver(CombatTagPacket.PACKET_ID, (payload, context) -> SitLogClientPacket.handleCombatTag(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SitLogClientPacket.onDisconnect());

        KeyMappingHelper.registerKeyMapping(KeybindInit.createKeyMapping(KeyMapping.Category.register(KeybindInit.CATEGORY_ID)));
        ClientTickEvents.END_CLIENT_TICK.register(KeybindInit::onClientTick);
    }

}
