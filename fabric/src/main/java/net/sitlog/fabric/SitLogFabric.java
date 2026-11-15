package net.sitlog.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.sitlog.SitlogMain;
import net.sitlog.init.EventInit;
import net.sitlog.network.SitLogServerPacket;
import net.sitlog.network.packet.*;

public class SitLogFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SitlogMain.init();

        PayloadTypeRegistry.clientboundPlay().register(PlayerListPacket.PACKET_ID, PlayerListPacket.PACKET_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PlayerListSinglePacket.PACKET_ID, PlayerListSinglePacket.PACKET_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SitPacket.PACKET_ID, SitPacket.PACKET_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FakePlayerPacket.PACKET_ID, FakePlayerPacket.PACKET_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SitPacket.PACKET_ID, SitPacket.PACKET_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DespawnTimerPacket.PACKET_ID, DespawnTimerPacket.PACKET_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ConfigSyncPacket.PACKET_ID, ConfigSyncPacket.PACKET_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CombatTagPacket.PACKET_ID, CombatTagPacket.PACKET_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SitPacket.PACKET_ID,
                (payload, context) -> context.server().execute(() -> SitLogServerPacket.handleSit(context.player(), payload)));

        EntityTrackingEvents.START_TRACKING.register(EventInit::onStartTracking);
        ServerPlayerEvents.JOIN.register(EventInit::onPlayerJoin);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> EventInit.onPlayerRespawn(newPlayer));
        ServerLifecycleEvents.SERVER_STARTING.register(EventInit::onServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(EventInit::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(EventInit::onServerStopping);
        ServerLifecycleEvents.SERVER_STOPPED.register(EventInit::onServerStopped);
    }

}
