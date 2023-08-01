package org.lins.mmmjjkx.fakeplayermaker.stress;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public interface IStressTester {
    Map<String, ServerPlayer> getTempPlayers();
    default boolean isStarted(){
        return !getTempPlayers().isEmpty();
    }

    /**
     * Start the stress tester
     * @throws IllegalStateException
     */
    void start() throws IllegalStateException;

    /**
     * Stop the stress tester
     * Note: Before executing this method, use the {@link #isStarted()} method to check if the stress tester is executing.
     */
    void stop();
}
