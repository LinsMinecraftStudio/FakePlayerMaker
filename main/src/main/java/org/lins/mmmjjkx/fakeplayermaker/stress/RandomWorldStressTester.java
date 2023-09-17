package org.lins.mmmjjkx.fakeplayermaker.stress;

import com.mojang.authlib.GameProfile;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.FakePlayerCreateEvent;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.StressTesterStartEvent;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.StressTesterStopEvent;
import io.github.linsminecraftstudio.fakeplayermaker.api.interfaces.IStressTester;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
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

    public RandomWorldStressTester(boolean isAmountPerWorld,int amount,List<String> ignoreWorlds) {
        this.amount = amount;
        this.lastStartTimestamp = 0;
        this.isAmountPerWorld = isAmountPerWorld;
        this.ignores = ignoreWorlds;
    }

    @Override
    public void run() throws IllegalStateException{
        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp - lastStartTimestamp < (5 * 1000L)){
            throw new IllegalStateException();
        }

        new StressTesterStartEvent(this).callEvent();

        Random random = new Random();
        List<World> worlds = Bukkit.getWorlds();

        if (isAmountPerWorld) {
            String randomNamePrefix = NMSFakePlayerMaker.getRandomName(FakePlayerMaker.randomNameLength);
            for (World world: Bukkit.getWorlds()) {
                if (ignores.contains(world.getName())) {
                    continue;
                }
                ServerLevel level = (ServerLevel) getHandle(getCraftClass("CraftWorld"), world);
                Location location = generate(world);
                placePlayer(server, randomNamePrefix, level, location, amount);
            }
        } else {
            for (int i = 0; i < worlds.size(); i++) {
                String randomNamePrefix = NMSFakePlayerMaker.getRandomName(FakePlayerMaker.randomNameLength);
                World world = worlds.get(random.nextInt(worlds.size()));
                if (ignores.contains(world.getName())) {
                    continue;
                }
                ServerLevel level = (ServerLevel) getHandle(getCraftClass("CraftWorld"), world);
                Location location = generate(world);
                int placeAmount = random.nextInt(amount);
                if (amount == 0) return;
                placePlayer(server, randomNamePrefix, level, location, placeAmount);
                amount -= placeAmount;
            }
        }

        lastStartTimestamp = currentTimestamp;
    }

    private void placePlayer(MinecraftServer server, String randomNamePrefix, ServerLevel level, Location location, int amount) {
        for (int i = 0; i < amount; i++) {
            String finalName = randomNamePrefix + (i+1);
            UUID uuid = Bukkit.getOfflinePlayer(finalName).getUniqueId();

            ServerPlayer player = new ServerPlayer(server, level, new GameProfile(uuid, finalName));

            var connection = new EmptyConnection(PacketFlow.CLIENTBOUND);
            var listener = new ServerGamePacketListenerImpl(server, connection, player);

            listener.teleport(location);

            new FakePlayerCreateEvent(player.getBukkitEntity(), null).callEvent();

            connection.setListener(listener);

            server.getPlayerList().placeNewPlayer(connection, player);
            simulateLogin(player);

            player.connection = listener;

            tempPlayers.put(finalName, player);
        }
    }

    @Override
    public void stop() {
        new StressTesterStopEvent(this).callEvent();
        tempPlayers.values().forEach(server.getPlayerList()::remove);
        tempPlayers.clear();
    }

    @Override
    public Map<String, ServerPlayer> getTempPlayers(){
        return tempPlayers;
    }

    private Location generate(World world) {
        Random random = new Random();
        Material below = null;
        Location location = null;
        while (below == null || !(below.isSolid())) {
            int x = random.nextInt(-20000, 20000);
            int z = random.nextInt(-20000, 20000);
            location = new Location(world, x, world.getHighestBlockYAt(x,z), z);
            int y = (int) (location.getY() - 1);
            below = world.getBlockAt(x, y, z).getType();
        }
        return location;
    }
}
