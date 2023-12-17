package io.github.linsminecraftstudio.fakeplayermaker.api.interfaces;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.Map;

public interface IStressTester extends Runnable{
    Map<String, ServerPlayer> getTempPlayers();
    default boolean isStarted(){
        return !getTempPlayers().isEmpty();
    }

    /**
     * Start the stress tester
     * @throws IllegalStateException when you start it too fast
     */
    void run() throws IllegalStateException;

    /**
     * Stop the stress tester
     * Note: Before executing this method, use the {@link #isStarted()} method to check if the stress tester is executing.
     */
    void stop();

    default Location getHighestBlock(World world, int x, int z){
        int i = 319;
        Location location = new Location(world, x, i, z);
        while(i > 0){
            if (location.getBlock().getType() != Material.AIR) {
                return location.set(x, i, z);
            }
            i--;
        }
        return new Location(world, x, 100, z);
    }
}
