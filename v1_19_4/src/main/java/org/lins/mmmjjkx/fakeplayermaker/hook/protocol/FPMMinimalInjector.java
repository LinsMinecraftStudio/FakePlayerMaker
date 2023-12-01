package org.lins.mmmjjkx.fakeplayermaker.hook.protocol;

import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.injector.temporary.MinimalInjector;
import com.mojang.authlib.GameProfile;
import io.github.linsminecraftstudio.fakeplayermaker.api.objects.EmptyConnection;
import io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.entity.Player;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.UUID;

import static io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils.getCraftClass;
import static io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils.getHandle;

public class FPMMinimalInjector implements MinimalInjector {
    private final String name;

    public FPMMinimalInjector(String name) {
        this.name = name;
    }

    @Override
    public SocketAddress getAddress() {
        return InetSocketAddress.createUnresolved("127.0.0.1", 60000);
    }

    @Override
    public void disconnect(String s) {
        nmsPlayer().connection.disconnect(s);
    }

    @Override
    public void sendServerPacket(Object o, NetworkMarker networkMarker, boolean b) {
        nmsPlayer().connection.send((Packet<?>) o);
    }

    @Override
    public Player getPlayer() {
        ServerLevel level = (ServerLevel) Objects.requireNonNull(getHandle(getCraftClass("CraftWorld"), Objects.requireNonNull(FakePlayerMaker.settings.getLocation("defaultSpawnLocation")).getWorld()));
        ServerPlayer player = new ServerPlayer(MinecraftServer.getServer(), level, new GameProfile(UUID.randomUUID(), name));
        var connection = new EmptyConnection();
        player.connection = MinecraftUtils.getGamePacketListener(connection, player);
        return player.getBukkitEntity();
    }

    @Override
    public boolean isConnected() {
        return !nmsPlayer().hasDisconnected();
    }

    private ServerPlayer nmsPlayer() {
        return (ServerPlayer) getHandle(getCraftClass("entity.CraftPlayer"), getPlayer());
    }
}
