package net.sitlog.neoforge.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sitlog.platform.services.INetworkHelper;

public class NeoForgeNetworkHelper implements INetworkHelper {

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    @Override
    public void sendToTracking(Entity entity, CustomPacketPayload payload) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().sendToTrackingPlayersFiltered(entity, new ClientboundCustomPayloadPacket(payload), player -> player.connection.hasChannel(payload));
        }
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null && connection.hasChannel(payload)) {
            ClientPacketDistributor.sendToServer(payload);
        }
    }

}
