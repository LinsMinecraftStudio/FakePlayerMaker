package org.lins.mmmjjkx.fakeplayermaker.stress;

import com.mojang.authlib.GameProfile;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.FakePlayerCreateEvent;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.StressTesterStartEvent;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.StressTesterStopEvent;
import io.github.linsminecraftstudio.fakeplayermaker.api.interfaces.IStressTester;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.bukkit.Location;
import org.bukkit.World;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.WorldNotFoundException;
import org.lins.mmmjjkx.fakeplayermaker.objects.EmptyConnection;
import org.lins.mmmjjkx.fakeplayermaker.objects.EmptyGamePackListener;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;

import java.util.*;

import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.*;

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
    public void run() throws WorldNotFoundException, IllegalStateException{
        if (!FakePlayerMaker.settings.getBoolean("areaStressTester")){
            return;
        }

        if (spawnRegion.getWorld() == null) {
            throw new WorldNotFoundException();
        }

        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp - lastStartTimestamp < (5 * 1000L)){
            throw new IllegalStateException();
        }

        new StressTesterStartEvent(this).callEvent();

        World world = BukkitAdapter.adapt(spawnRegion.getWorld());
        int y = spawnRegion.getMinimumY();
        List<BlockVector3> list = spawnRegion.getChunkCubes().stream().toList();
        Random random = new Random();
        String randomNamePrefix = NMSFakePlayerMaker.getRandomName(FakePlayerMaker.randomNameLength);
        ServerLevel level = (ServerLevel) getHandle(getCraftClass("CraftWorld"), world);

        for (int i = 0; i < amount; i++) {
            String finalName = randomNamePrefix + (i + 1);
            UUID uuid = UUID.randomUUID();
            BlockVector3 flatLocation = list.get(random.nextInt(amount));

            ServerPlayer player = new ServerPlayer(server, level, new GameProfile(uuid, finalName));

            tempPlayers.put(player.getName().getString(), player);

            var connection = new EmptyConnection(PacketFlow.CLIENTBOUND);
            var listener = new EmptyGamePackListener(server, player);
            var listener2 = new ServerLoginPacketListenerImpl(server, connection);

            listener.teleport(new Location(world, flatLocation.getX(), y, flatLocation.getZ()));

            new FakePlayerCreateEvent(player.getBukkitEntity(), null).callEvent();
            simulateLogin(player);

            connection.setListener(listener);

            server.getPlayerList().placeNewPlayer(connection, player);
            player.connection = listener;

            connection.setListener(listener2);
        }
        lastStartTimestamp = currentTimestamp;
    }

    @Override
    public void stop() {
        new StressTesterStopEvent(this).callEvent();
        tempPlayers.values().forEach(server.getPlayerList()::remove);
        tempPlayers.clear();
    }

    @Override
    public Map<String, ServerPlayer> getTempPlayers() {
        return tempPlayers;
    }
}
