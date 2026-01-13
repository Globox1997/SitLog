package net.sitlog.state;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.sitlog.SitlogMain;

import java.util.Map;
import java.util.UUID;

public class SittingPlayerState extends PersistentState {

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        int count = 0;
        nbt.putInt("SittingPlayerCount", SitlogMain.SITLOG_PLAYER_MAP.size());
        for (Map.Entry<UUID, UUID> entry : SitlogMain.SITLOG_PLAYER_MAP.entrySet()) {
            nbt.putUuid("Player_" + count, entry.getKey());
            nbt.putUuid("SittingPlayer_" + count, entry.getValue());
            count++;
        }
        return nbt;
    }

    public static PersistentState.Type<SittingPlayerState> getPersistentStateType() {
        return new PersistentState.Type<>(SittingPlayerState::new, (nbt, registryLookup) -> fromNbt(nbt), null);
    }

    public static SittingPlayerState fromNbt(NbtCompound nbt) {
        SittingPlayerState sittingPlayerState = new SittingPlayerState();
        for (int i = 0; i < nbt.getInt("SittingPlayerCount"); i++) {
            if(nbt.contains("Player_" + i) && nbt.contains("SittingPlayer_" + i)) {
                UUID playerUuid = nbt.getUuid("Player_" + i);
                UUID sittingPlayerUuid = nbt.getUuid("SittingPlayer_" + i);
                SitlogMain.SITLOG_PLAYER_MAP.put(playerUuid, sittingPlayerUuid);
            }
        }
        return sittingPlayerState;
    }

    public static SittingPlayerState getSittingPlayerState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        SittingPlayerState state = persistentStateManager.getOrCreate(getPersistentStateType(), "sitlog");
        state.markDirty();
        return state;
    }

}