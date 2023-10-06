package org.lins.mmmjjkx.fakeplayermaker.stress;

import com.mojang.authlib.GameProfile;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.StressTesterStartEvent;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.StressTesterStopEvent;
import io.github.linsminecraftstudio.fakeplayermaker.api.interfaces.IStressTester;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.implementation.Implementations;
import org.lins.mmmjjkx.fakeplayermaker.implementation.PacketListenerMaker;
import org.lins.mmmjjkx.fakeplayermaker.objects.EmptyConnection;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;

import java.util.*;

import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.*;

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

        new StressTesterStartEvent(this).callEvent();

        Random random = new Random();
        List<World> worlds = Bukkit.getWorlds();

        String randomNamePrefix = NMSFakePlayerMaker.getRandomName(FakePlayerMaker.randomNameLength);
        if (isAmountPerWorld) {
            for (World world: Bukkit.getWorlds()) {
                if (ignores.contains(world.getName())) {
                    continue;
                }
                ServerLevel level = (ServerLevel) getHandle(getCraftClass("CraftWorld"), world);
                placePlayer(randomNamePrefix, level, amount);
            }
        } else {
            for (int i = 0; i < worlds.size(); i++) {
                World world = worlds.get(random.nextInt(worlds.size()));
                if (ignores.contains(world.getName())) {
                    continue;
                }
                ServerLevel level = (ServerLevel) getHandle(getCraftClass("CraftWorld"), world);
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
            UUID uuid = UUIDUtil.createOfflinePlayerUUID(finalName);
            Location location = generate(level.getWorld());

            ServerPlayer player = Implementations.runImplAndReturn(t -> t.create(level, new GameProfile(uuid, finalName)));

            var connection = new EmptyConnection(PacketFlow.CLIENTBOUND);
            var listener = PacketListenerMaker.getGamePacketListener(connection, player);

            connection.setListener(listener);

            Implementations.runImpl(t -> t.placePlayer(connection, player));
            simulateLogin(player);

            player.connection = listener;
            player.teleportTo(level, location.getX(), location.getY(), location.getZ(),0,0);

            tempPlayers.put(finalName, player);
        }
    }

    @Override
    public void stop() {
        new StressTesterStopEvent(this).callEvent();
        tempPlayers.values().forEach(server.getPlayerList()::remove);
        tempPlayers.clear();
        HandlerList.unregisterAll(listener);
    }

    @Override
    public Map<String, ServerPlayer> getTempPlayers(){
        return tempPlayers;
    }

    private Location generate(World world) {
        Random random = new Random();
        int x = random.nextInt(-20000, 20000);
        int z = random.nextInt(-20000, 20000);
        return getHighestBlock(world, x, z);
    }

    private class AutoRespawn implements Listener {
        @EventHandler
        public void onDeath(PlayerDeathEvent e) {
            ServerPlayer player = (ServerPlayer) getHandle(getCraftClass("entity.CraftPlayer"), e.getPlayer());
            if (player != null && tempPlayers.containsKey(player.getName().getString())) {
                Location location = e.getPlayer().getLocation();
                e.getPlayer().spigot().respawn();
                e.getPlayer().teleport(location);
            }
        }
    }
}
