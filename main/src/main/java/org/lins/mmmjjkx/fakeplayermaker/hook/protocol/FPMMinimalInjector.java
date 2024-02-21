package org.lins.mmmjjkx.fakeplayermaker.hook.protocol;

import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.injector.temporary.MinimalInjector;
import com.mojang.authlib.GameProfile;
import io.github.linsminecraftstudio.fakeplayermaker.api.implementation.Implementations;
import io.github.linsminecraftstudio.fakeplayermaker.api.objects.EmptyConnection;
import io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils.getCraftClass;

public class FPMMinimalInjector implements MinimalInjector {
    private final String name;
    private final ServerLevel level;

    public FPMMinimalInjector(String name, ServerLevel level) {
        this.name = name;
        this.level = level;
    }

    @Override
    public SocketAddress getAddress() {
        return InetSocketAddress.createUnresolved("127.0.0.1", 60000);
    }

    @Override
    public void disconnect(String s) {
        nmsPlayer().connection.disconnect(s, PlayerKickEvent.Cause.PLUGIN);
    }

    @Override
    public void sendServerPacket(Object o, NetworkMarker networkMarker, boolean b) {
        nmsPlayer().connection.send((Packet<?>) o);
    }

    @Override
    public Player getPlayer() {
        ServerPlayer player = Implementations.get().create(level, new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name));
        Implementations.get().setConnection(player, MinecraftUtils.getGamePacketListener(new EmptyConnection(), player));
        return Implementations.bukkitEntity(player);
    }

    @Override
    public boolean isConnected() {
        return !nmsPlayer().hasDisconnected();
    }

    private ServerPlayer nmsPlayer() {
        return (ServerPlayer) MinecraftUtils.getHandle(getCraftClass("entity.CraftPlayer"), getPlayer());
    }
}
