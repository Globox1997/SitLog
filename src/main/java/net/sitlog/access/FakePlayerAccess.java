package net.sitlog.access;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public interface FakePlayerAccess {

    void sitLog$setBodyDespawnTimer(int time);

    @Nullable
    Text sitLog$getDeathMessage();
}
