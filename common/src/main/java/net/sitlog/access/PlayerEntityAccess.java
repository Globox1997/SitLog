package net.sitlog.access;

public interface PlayerEntityAccess {

    void sitLog$setDespawnTimer(int time);

    int sitLog$getDespawnTimer();

    void sitLog$setCombatTimer(int time);

    int sitLog$getCombatTimer();
}
