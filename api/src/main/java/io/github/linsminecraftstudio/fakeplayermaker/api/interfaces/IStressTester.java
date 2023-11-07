package io.github.linsminecraftstudio.fakeplayermaker.api.interfaces;

import io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.lang.reflect.InvocationTargetException;
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
            if(location.getBlock().getType() != Material.AIR)
                return location.add(0, 1, 0);
            i--;
            location.setY(i);
        }
        return new Location(world, x, 1, z);
    }

    default PlayerList getPlayerList() {
        if (Bukkit.getMinecraftVersion().equals("1.20.1")) {
            try {
                return (PlayerList) MinecraftServer.class.getMethod("ac").invoke(MinecraftUtils.getNMSServer());
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        return MinecraftUtils.getNMSServer().getPlayerList();
    }
}
