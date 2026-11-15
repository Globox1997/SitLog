package net.sitlog.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.sitlog.SitlogMain;
import net.sitlog.init.EventInit;
import net.sitlog.network.SitLogServerPacket;
import net.sitlog.network.packet.*;

@Mod(SitlogMain.MOD_ID)
public class SitLogNeoForge {

    public SitLogNeoForge(IEventBus modBus) {
        SitlogMain.init();
        modBus.addListener(RegisterPayloadHandlersEvent.class, SitLogNeoForge::registerPayloads);

        NeoForge.EVENT_BUS.addListener(PlayerEvent.StartTracking.class, event -> {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                EventInit.onStartTracking(event.getTarget(), serverPlayer);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                EventInit.onPlayerJoin(serverPlayer);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                EventInit.onPlayerRespawn(serverPlayer);
            }
        });
        NeoForge.EVENT_BUS.addListener(ServerAboutToStartEvent.class, event -> EventInit.onServerStarting(event.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> EventInit.onServerStarted(event.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, event -> EventInit.onServerStopping(event.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> EventInit.onServerStopped(event.getServer()));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(PlayerListPacket.PACKET_ID, PlayerListPacket.PACKET_CODEC);
        registrar.playToClient(PlayerListSinglePacket.PACKET_ID, PlayerListSinglePacket.PACKET_CODEC);
        registrar.playBidirectional(SitPacket.PACKET_ID, SitPacket.PACKET_CODEC,
                (payload, context) -> SitLogServerPacket.handleSit((ServerPlayer) context.player(), payload));
        registrar.playToClient(FakePlayerPacket.PACKET_ID, FakePlayerPacket.PACKET_CODEC);
        registrar.playToClient(DespawnTimerPacket.PACKET_ID, DespawnTimerPacket.PACKET_CODEC);
        registrar.playToClient(ConfigSyncPacket.PACKET_ID, ConfigSyncPacket.PACKET_CODEC);
        registrar.playToClient(CombatTagPacket.PACKET_ID, CombatTagPacket.PACKET_CODEC);
    }

}
