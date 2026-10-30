package net.sitlog.access;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import net.minecraft.network.chat.Component;

public interface FakePlayerAccess {

    void sitLog$setBodyDespawnTimer(int time);

    boolean sitLog$isDespawned();

    @Nullable
    Component sitLog$getDeathMessage();

    float sitLog$getDamageTaken();

    Collection<String> sitLog$getAttackers();
}
