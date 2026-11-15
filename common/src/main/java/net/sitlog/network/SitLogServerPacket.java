package net.sitlog.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.sitlog.network.packet.SitPacket;
import net.sitlog.util.SitLogUtil;

public class SitLogServerPacket {

    public static void handleSit(ServerPlayer player, SitPacket payload) {
        boolean sit = payload.sit();
        if (sit && player.isPassenger()) {
            return;
        }
        if (!player.isSpectator() && player.isAlive() && player.getPose().equals(Pose.STANDING)) {
            SitLogUtil.setSitting(player, sit);
        }
    }

}
