package org.lins.mmmjjkx.fakeplayermaker.stress;

import com.mojang.authlib.GameProfile;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.StressTesterStartEvent;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.StressTesterStopEvent;
import io.github.linsminecraftstudio.fakeplayermaker.api.interfaces.IStressTester;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.WorldNotFoundException;
import org.lins.mmmjjkx.fakeplayermaker.objects.EmptyConnection;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;

import java.util.*;

import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.*;

public class AreaStressTester implements IStressTester {
    private final Map<String, ServerPlayer> tempPlayers = new HashMap<>();
    private final CuboidRegion spawnRegion;
    private final int amount;
    private final MinecraftServer server = MinecraftServer.getServer();
    private long lastStartTimestamp;
    private final AutoRespawn listener;
    public AreaStressTester(CuboidRegion spawnRegion, int amount) {
        this.spawnRegion = spawnRegion;
        this.amount = amount;
        this.lastStartTimestamp = 0;
        this.listener = new AutoRespawn();
    }

    @Override
    public void run() throws WorldNotFoundException, IllegalStateException{
        if (!FakePlayerMaker.settings.getBoolean("areaStressTesters")){
            return;
        }

        if (spawnRegion.getWorld() == null) {
            throw new WorldNotFoundException();
        }

        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp - lastStartTimestamp < (5 * 1000L)){
            throw new IllegalStateException();
        }

        Bukkit.getPluginManager().registerEvents(listener, FakePlayerMaker.INSTANCE);

        new StressTesterStartEvent(this).callEvent();

        World world = BukkitAdapter.adapt(spawnRegion.getWorld());
        List<BlockVector3> list = getAreaBlocks();
        Random random = new Random();
        String randomNamePrefix = NMSFakePlayerMaker.getRandomName(FakePlayerMaker.randomNameLength);
        ServerLevel level = (ServerLevel) getHandle(getCraftClass("CraftWorld"), world);

        for (int i = 0; i < amount; i++) {
            String finalName = randomNamePrefix + (i + 1);
            UUID uuid = UUIDUtil.createOfflinePlayerUUID(finalName);
            BlockVector3 flatLocation = list.get(random.nextInt(list.size()));

            if (level == null) {
                stop();
                return;
            }

            ServerPlayer player = new ServerPlayer(server, level, new GameProfile(uuid, finalName));

            var connection = new EmptyConnection(PacketFlow.CLIENTBOUND);
            var listener = new ServerGamePacketListenerImpl(server, connection, player);

            connection.setListener(listener);

            server.getPlayerList().placeNewPlayer(connection, player);
            Location loc = getHighestBlock(world, flatLocation.getX(), flatLocation.getZ());
            player.teleportTo(level, loc.getX(), loc.getY(), loc.getZ(), 0, 0);

            simulateLogin(player);

            tempPlayers.put(player.getName().getString(), player);
        }

        lastStartTimestamp = currentTimestamp;
    }

    @Override
    public void stop() {
        new StressTesterStopEvent(this).callEvent();
        tempPlayers.values().forEach(server.getPlayerList()::remove);
        tempPlayers.clear();
        HandlerList.unregisterAll(listener);
    }

    @Override
    public Map<String, ServerPlayer> getTempPlayers() {
        return tempPlayers;
    }

    private class AutoRespawn implements Listener {
        @EventHandler
        public void onDeath(PlayerDeathEvent e) {
            ServerPlayer player = (ServerPlayer) getHandle(getCraftClass("entity.CraftPlayer"), e.getPlayer());
            if (player != null && tempPlayers.containsKey(player.getName().getString())) {
                Location location = e.getPlayer().getLocation();
                e.getPlayer().spigot().respawn();
                ServerLevel level = (ServerLevel) getHandle(getCraftClass("CraftWorld"), e.getPlayer().getWorld());
                if (level != null) {
                    player.teleportTo(level, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
                }
            }
        }
    }

    private List<BlockVector3> getAreaBlocks() {
        BlockVector3 vecMax = spawnRegion.getMaximumPoint();
        BlockVector3 vecMin = spawnRegion.getMinimumPoint();
        List<BlockVector3> list = new ArrayList<>();

        final int xMax = vecMax.getBlockX();
        final int zMax = vecMax.getBlockZ();

        final int xMin = vecMin.getBlockX();
        final int zMin = vecMin.getBlockZ();
        final int y = spawnRegion.getMinimumY();

        for (int i = xMin; i <= xMax; i++) {
            for (int k = zMin; k <= zMax; k++) {
                BlockVector3 blockVector3 = BlockVector3.at(i, y, k);
                list.add(blockVector3);
            }
        }
        return list;
    }

    public Location getHighestBlock(World world, int x, int z){
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
}
