package org.lins.mmmjjkx.fakeplayermaker.hook.protocol;

import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.injector.temporary.MinimalInjector;
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
import org.lins.mmmjjkx.fakeplayermaker.implementation.Implementations;
import org.lins.mmmjjkx.fakeplayermaker.implementation.PacketListenerMaker;
import org.lins.mmmjjkx.fakeplayermaker.objects.EmptyConnection;
import org.lins.mmmjjkx.fakeplayermaker.objects.FPMPacketListener;

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
        ServerLevel level = (ServerLevel) Objects.requireNonNull(getHandle(getCraftClass("CraftWorld"), Objects.requireNonNull(FakePlayerMaker.settings.getLocation("defaultSpawnLocation")).getWorld()));
        ServerPlayer player = Implementations.runImplAndReturn(t -> t.create(level, new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name)));
        var connection = new EmptyConnection(PacketFlow.CLIENTBOUND);
        Implementations.runImpl(t -> t.setConnection(player, PacketListenerMaker.getGamePacketListener(connection, player)));
        return Implementations.bukkitEntity(player);
    }

    @Override
    public boolean isConnected() {
        return !nmsPlayer().hasDisconnected();
    }

    private ServerPlayer nmsPlayer() {
        return (ServerPlayer) getHandle(getCraftClass("entity.CraftPlayer"), getPlayer());
    }
}
