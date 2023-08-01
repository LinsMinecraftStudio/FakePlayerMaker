package org.lins.mmmjjkx.fakeplayermaker.stress;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public interface IStressTester {
    Map<String, ServerPlayer> getTempPlayers();
    default boolean isStarted(){
        return !getTempPlayers().isEmpty();
    }
    void start();
    void stop();
}
