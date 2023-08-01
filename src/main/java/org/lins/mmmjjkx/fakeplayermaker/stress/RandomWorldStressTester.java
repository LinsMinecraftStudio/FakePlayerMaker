package org.lins.mmmjjkx.fakeplayermaker.stress;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;

import java.util.*;

import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getCraftClass;
import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getHandle;

public class RandomWorldStressTester implements IStressTester{
    private final Map<String, ServerPlayer> tempPlayers = new HashMap<>();
    private int amount;
    private long lastStartTimestamp;
    private final boolean isAmountPerWorld;

    public RandomWorldStressTester(boolean isAmountPerWorld,int amount) {
        this.amount = amount;
        this.lastStartTimestamp = 0;
        this.isAmountPerWorld = isAmountPerWorld;
    }

    @Override
    public void start() throws IllegalStateException{
        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp - lastStartTimestamp < (5 * 1000L)){
            throw new IllegalStateException();
        }

        Random random = new Random();
        List<World> worlds = Bukkit.getWorlds();
        MinecraftServer server = MinecraftServer.getServer();
        PlayerList playerList = server.getPlayerList();

        if (isAmountPerWorld) {
            String randomNamePrefix = NMSFakePlayerMaker.getRandomName(FakePlayerMaker.randomNameLength);
            for (World world: Bukkit.getWorlds()) {
                ServerLevel level = (ServerLevel) getHandle(getCraftClass("CraftWorld"), world);
                Location location = generate(world);
                for (int i = 0; i < amount; i++) {
                    String finalName = randomNamePrefix + (i+1);
                    UUID uuid = Bukkit.getOfflinePlayer(finalName).getUniqueId();

                    ServerPlayer player = new ServerPlayer(server, level, new GameProfile(uuid, finalName));
                    playerList.placeNewPlayer(player.connection.connection, player);
                    player.getBukkitEntity().teleport(location);

                    tempPlayers.put(finalName, player);
                }
            }
        } else {
            for (int i = 0; i < worlds.size(); i++) {
                String randomNamePrefix = NMSFakePlayerMaker.getRandomName(FakePlayerMaker.randomNameLength);
                World world = worlds.get(random.nextInt(worlds.size()));
                ServerLevel level = (ServerLevel) getHandle(getCraftClass("CraftWorld"), world);
                Location location = generate(world);
                int placeAmount = random.nextInt(amount);
                if (amount == 0) return;
                for (int j = 0; j < placeAmount; j++){
                    String finalName = randomNamePrefix + (j+1);
                    UUID uuid = Bukkit.getOfflinePlayer(finalName).getUniqueId();

                    ServerPlayer player = new ServerPlayer(server, level, new GameProfile(uuid, finalName));
                    playerList.placeNewPlayer(player.connection.connection, player);
                    player.getBukkitEntity().teleport(location);

                    tempPlayers.put(finalName, player);
                }
                amount -= placeAmount;
            }
        }

        lastStartTimestamp = currentTimestamp;
    }

    @Override
    public void stop() {
        for (ServerPlayer player : tempPlayers.values()) {
            MinecraftServer.getServer().getPlayerList().remove(player);
        }
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
