package io.github.linsminecraftstudio.fakeplayermaker.api.interfaces;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public interface IStressTester extends Runnable{
    Map<String, ServerPlayer> getTempPlayers();
    default boolean isStarted(){
        return !getTempPlayers().isEmpty();
    }

    /**
     * Start the stress tester
     * @throws IllegalStateException
     */
    void run() throws IllegalStateException;

    /**
     * Stop the stress tester
     * Note: Before executing this method, use the {@link #isStarted()} method to check if the stress tester is executing.
     */
    void stop();
}
