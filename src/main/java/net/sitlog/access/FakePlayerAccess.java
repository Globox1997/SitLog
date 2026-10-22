package net.sitlog.access;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface FakePlayerAccess {

    void sitLog$setBodyDespawnTimer(int time);

    boolean sitLog$isDespawned();

    @Nullable
    Text sitLog$getDeathMessage();

    float sitLog$getDamageTaken();

    Collection<String> sitLog$getAttackers();
}
