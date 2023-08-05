package org.lins.mmmjjkx.fakeplayermaker.stress;

import com.mojang.authlib.GameProfile;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.lins.mmmjjkx.fakeplayermaker.WorldNotFoundException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;

import java.util.*;

import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getCraftClass;
import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getHandle;

public class AreaStressTester implements IStressTester {
    private final Map<String, ServerPlayer> tempPlayers = new HashMap<>();
    private final CuboidRegion spawnRegion;
    private final int amount;
    private final MinecraftServer server = MinecraftServer.getServer();
    private long lastStartTimestamp;

    public AreaStressTester(CuboidRegion spawnRegion, int amount) {
        this.spawnRegion = spawnRegion;
        this.amount = amount;
        this.lastStartTimestamp = 0;
    }

    @Override
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
        List<BlockVector3> list = spawnRegion.getChunkCubes().stream().toList();
        Random random = new Random();
        String randomNamePrefix = NMSFakePlayerMaker.getRandomName(FakePlayerMaker.randomNameLength);
        ServerLevel level = (ServerLevel) getHandle(getCraftClass("CraftWorld"), world);

        for (int i = 0; i < amount; i++) {
            String finalName = randomNamePrefix + (i + 1);
            UUID uuid = Bukkit.getOfflinePlayer(finalName).getUniqueId();
            BlockVector3 flatLocation = list.get(random.nextInt(amount));

            ServerPlayer player = new ServerPlayer(server, level, new GameProfile(uuid, finalName));
            player.getBukkitEntity().teleport(new Location(world, flatLocation.getX(), y, flatLocation.getZ()));

            tempPlayers.put(player.getName().getString(), player);
            server.getPlayerList().placeNewPlayer(player.connection.connection, player);
        }
        lastStartTimestamp = currentTimestamp;
    }

    @Override
    public void stop() {
        tempPlayers.values().forEach(server.getPlayerList()::remove);
        tempPlayers.clear();
    }

    @Override
    public Map<String, ServerPlayer> getTempPlayers() {
        return tempPlayers;
    }
}
