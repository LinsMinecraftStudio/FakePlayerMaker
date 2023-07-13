package org.lins.mmmjjkx.fakeplayermaker.stress;

import com.mojang.authlib.GameProfile;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.regions.CuboidRegion;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.WorldNotFoundException;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;

import java.util.*;
import java.util.stream.StreamSupport;

import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getCraftClass;
import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getHandle;

public class AreaStressTester {
    private final Map<String, ServerPlayer> tempPlayers = new HashMap<>();
    private final CuboidRegion spawnRegion;
    private final int amount;
    private long lastStartTimestamp;

    public AreaStressTester(CuboidRegion spawnRegion, int amount) {
        this.spawnRegion = spawnRegion;
        this.amount = amount;
        this.lastStartTimestamp = 0;
    }

    public void start() throws WorldNotFoundException, IllegalStateException{
        if (spawnRegion.getWorld() == null) {
            throw new WorldNotFoundException();
        }

        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp - lastStartTimestamp < (5 * 1000L)){
            throw new IllegalStateException();
        }

        World world = BukkitAdapter.adapt(spawnRegion.getWorld());
        int y = spawnRegion.getMinimumY();
        List<BlockVector2> list = StreamSupport.stream(spawnRegion.getBoundingBox().asFlatRegion().spliterator(), false).toList();
        List<ServerPlayer> tempList = new ArrayList<>(amount);
        Random random = new Random();
        String randomNamePrefix = NMSFakePlayerMaker.getRandomName(FakePlayerMaker.randomNameLength);
        MinecraftServer server = MinecraftServer.getServer();
        ServerLevel level = (ServerLevel) getHandle(getCraftClass("CraftWorld"), world);

        for (int i = 0; i < amount; i++) {
            String finalName = randomNamePrefix + (i + 1);
            UUID uuid = Bukkit.getOfflinePlayer(finalName).getUniqueId();
            BlockVector2 flatLocation = list.get(random.nextInt(amount));

            ServerPlayer player = new ServerPlayer(server, level, new GameProfile(uuid, finalName));
            player.getBukkitEntity().teleport(new Location(world, flatLocation.getX(), y, flatLocation.getZ()));

            tempList.add(player);
        }

        for (ServerPlayer player : tempList) {
            tempPlayers.put(player.getName().getString(), player);
            server.getPlayerList().placeNewPlayer(player.connection.connection, player);
        }

        lastStartTimestamp = currentTimestamp;
    }

    public void stop() {
        for (ServerPlayer player : tempPlayers.values()) {
            MinecraftServer.getServer().getPlayerList().remove(player);
        }
        tempPlayers.clear();
    }

    public boolean isStarted() {
        return !tempPlayers.isEmpty();
    }
}
