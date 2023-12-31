package org.lins.mmmjjkx.fakeplayermaker.stress;

import io.github.linsminecraftstudio.fakeplayermaker.api.implementation.Implementations;
import io.github.linsminecraftstudio.fakeplayermaker.api.interfaces.IStressTester;
import io.github.linsminecraftstudio.fakeplayermaker.api.objects.EmptyConnection;
import io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils.getHandle;

public class RandomWorldStressTester implements IStressTester {
    private final Map<String, ServerPlayer> tempPlayers = new HashMap<>();
    private final MinecraftServer server = MinecraftServer.getServer();
    private int amount;
    private long lastStartTimestamp;
    private final boolean isAmountPerWorld;
    private final List<String> ignores;
    private final AutoRespawn listener;

    public RandomWorldStressTester(boolean isAmountPerWorld,int amount,List<String> ignoreWorlds) {
        this.amount = amount;
        this.lastStartTimestamp = 0;
        this.isAmountPerWorld = isAmountPerWorld;
        this.ignores = ignoreWorlds;
        this.listener = new AutoRespawn();
    }

    @Override
    public void run() throws IllegalStateException{
        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp - lastStartTimestamp < (5 * 1000L)){
            throw new IllegalStateException();
        }

        Bukkit.getPluginManager().registerEvents(listener, FakePlayerMaker.INSTANCE);

        Random random = new Random();
        List<World> worlds = Bukkit.getWorlds();

        String randomNamePrefix = NMSFakePlayerMaker.getRandomName(FakePlayerMaker.randomNameLength);
        if (isAmountPerWorld) {
            for (World world: Bukkit.getWorlds()) {
                if (ignores.contains(world.getName())) {
                    continue;
                }
                ServerLevel level = (ServerLevel) getHandle(MinecraftUtils.getCraftClass("CraftWorld"), world);
                placePlayer(randomNamePrefix, level, amount);
            }
        } else {
            for (int i = 0; i < worlds.size(); i++) {
                World world = worlds.get(random.nextInt(worlds.size()));
                if (ignores.contains(world.getName())) {
                    continue;
                }
                ServerLevel level = (ServerLevel) getHandle(MinecraftUtils.getCraftClass("CraftWorld"), world);
                int placeAmount = random.nextInt(amount);
                if (amount == 0) return;
                placePlayer(randomNamePrefix, level, placeAmount);
                amount -= placeAmount;
            }
        }

        lastStartTimestamp = currentTimestamp;
    }

    private void placePlayer(String randomNamePrefix, ServerLevel level, int amount) {
        for (int i = 0; i < amount; i++) {
            String finalName = randomNamePrefix + (i +1);
            Location location = generate(level.getWorld());

            Pair<Location, ServerPlayer> pair = NMSFakePlayerMaker.createSimple(location, finalName);
            ServerPlayer player = pair.getRight();

            Implementations.get().placePlayer(new EmptyConnection(), player);

            player.teleportTo(level, location.getX(), location.getY(), location.getZ(),0,0);

            tempPlayers.put(finalName, player);
        }
    }

    @Override
    public void stop() {
        tempPlayers.values().forEach(server.getPlayerList()::remove);
        tempPlayers.clear();
        HandlerList.unregisterAll(listener);
    }

    @Override
    public Map<String, ServerPlayer> getTempPlayers(){
        return tempPlayers;
    }

    private Location generate(World world) {
        int x = new Random().nextInt(-20000, 20001);
        int z = new Random().nextInt(-20000, 20001);
        return getHighestBlock(world, x, z).add(0, 1, 0);
    }

    private class AutoRespawn implements Listener {
        @EventHandler
        public void onDeath(PlayerDeathEvent e) {
            ServerPlayer player = (ServerPlayer) getHandle(MinecraftUtils.getCraftClass("entity.CraftPlayer"), e.getPlayer());
            if (player != null && tempPlayers.containsKey(player.getName().getString())) {
                Location location = e.getPlayer().getLocation();
                e.getPlayer().spigot().respawn();
                e.getPlayer().teleport(location);
            }
        }
    }
}
