package net.sitlog.state;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.sitlog.SitlogMain;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SittingPlayerState extends SavedData {

    public static final Codec<SittingPlayerState> CODEC = CompoundTag.CODEC.xmap(SittingPlayerState::fromNbt, SittingPlayerState::toNbt);

    public static final SavedDataType<SittingPlayerState> TYPE = new SavedDataType<>(SitlogMain.identifierOf("sitlog"), SittingPlayerState::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        int count = 0;
        nbt.putInt("SittingPlayerCount", SitlogMain.SITLOG_PLAYER_MAP.size());
        for (Map.Entry<UUID, UUID> entry : SitlogMain.SITLOG_PLAYER_MAP.entrySet()) {
            nbt.store("Player_" + count, UUIDUtil.CODEC, entry.getKey());
            nbt.store("SittingPlayer_" + count, UUIDUtil.CODEC, entry.getValue());
            count++;
        }
        return nbt;
    }

    public static SittingPlayerState fromNbt(CompoundTag nbt) {
        SittingPlayerState sittingPlayerState = new SittingPlayerState();
        int count = nbt.getIntOr("SittingPlayerCount", 0);
        for (int i = 0; i < count; i++) {
            Optional<UUID> playerUuid = nbt.read("Player_" + i, UUIDUtil.CODEC);
            Optional<UUID> sittingPlayerUuid = nbt.read("SittingPlayer_" + i, UUIDUtil.CODEC);
            if (playerUuid.isPresent() && sittingPlayerUuid.isPresent()) {
                SitlogMain.SITLOG_PLAYER_MAP.put(playerUuid.get(), sittingPlayerUuid.get());
            }
        }
        return sittingPlayerState;
    }

    public static SittingPlayerState getSittingPlayerState(MinecraftServer server) {
        SavedDataStorage dataStorage = server.getDataStorage();
        SittingPlayerState state = dataStorage.get(TYPE);
        if (state == null) {
            state = new SittingPlayerState();
            readLegacyData(server);
            dataStorage.set(TYPE, state);
        }
        state.setDirty();
        return state;
    }

    private static void readLegacyData(MinecraftServer server) {
        Path legacyPath = server.getWorldPath(LevelResource.DATA).resolve("sitlog.dat");
        if (Files.isRegularFile(legacyPath)) {
            try {
                NbtIo.readCompressed(legacyPath, NbtAccounter.unlimitedHeap()).getCompound("data").ifPresent(SittingPlayerState::fromNbt);
            } catch (Exception exception) {
                SitlogMain.LOGGER.warn("Failed to read legacy sitting player data {}", legacyPath, exception);
            }
        }
    }

}
