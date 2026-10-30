package net.sitlog.state;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.sitlog.SitlogMain;

import java.util.Map;
import java.util.UUID;

public class SittingPlayerState extends SavedData {

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        int count = 0;
        nbt.putInt("SittingPlayerCount", SitlogMain.SITLOG_PLAYER_MAP.size());
        for (Map.Entry<UUID, UUID> entry : SitlogMain.SITLOG_PLAYER_MAP.entrySet()) {
            nbt.putUUID("Player_" + count, entry.getKey());
            nbt.putUUID("SittingPlayer_" + count, entry.getValue());
            count++;
        }
        return nbt;
    }

    public static SavedData.Factory<SittingPlayerState> getPersistentStateType() {
        return new SavedData.Factory<>(SittingPlayerState::new, (nbt, registryLookup) -> fromNbt(nbt), null);
    }

    public static SittingPlayerState fromNbt(CompoundTag nbt) {
        SittingPlayerState sittingPlayerState = new SittingPlayerState();
        for (int i = 0; i < nbt.getInt("SittingPlayerCount"); i++) {
            if(nbt.contains("Player_" + i) && nbt.contains("SittingPlayer_" + i)) {
                UUID playerUuid = nbt.getUUID("Player_" + i);
                UUID sittingPlayerUuid = nbt.getUUID("SittingPlayer_" + i);
                SitlogMain.SITLOG_PLAYER_MAP.put(playerUuid, sittingPlayerUuid);
            }
        }
        return sittingPlayerState;
    }

    public static SittingPlayerState getSittingPlayerState(MinecraftServer server) {
        DimensionDataStorage persistentStateManager = server.getLevel(Level.OVERWORLD).getDataStorage();
        SittingPlayerState state = persistentStateManager.computeIfAbsent(getPersistentStateType(), "sitlog");
        state.setDirty();
        return state;
    }

}