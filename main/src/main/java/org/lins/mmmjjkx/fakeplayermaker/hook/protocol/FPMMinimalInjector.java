package org.lins.mmmjjkx.fakeplayermaker.hook.protocol;

import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.injector.temporary.MinimalInjector;
import com.comphenix.protocol.injector.temporary.TemporaryPlayer;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.entity.Player;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.objects.EmptyConnection;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;

import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getCraftClass;
import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getHandle;

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
        ServerPlayer player = new ServerPlayer(MinecraftServer.getServer(), (ServerLevel) Objects.requireNonNull(getHandle(getCraftClass("CraftWorld"), FakePlayerMaker.settings.getLocation("defaultSpawnLocation").getWorld())),
                new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name));
        var connection = new EmptyConnection(PacketFlow.CLIENTBOUND);
        player.connection = new ServerGamePacketListenerImpl(MinecraftServer.getServer(), connection, player);
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
