package org.lins.mmmjjkx.fakeplayermaker.objects;

import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public final class EmptyConnection extends Connection {

    public EmptyConnection() {
        super(PacketFlow.SERVERBOUND);

        EmbeddedChannel theChannel = new EmbeddedChannel();
        theChannel.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 60000));
        theChannel.pipeline().addLast("encoder", getEncoder());
        theChannel.pipeline().addLast("decoder", getDecoder());

        if (Bukkit.getMinecraftVersion().equals("1.20.2")) {
            setupChannel(theChannel);

            this.channel = theChannel;
        } else {
            try {
                getClass().getField("m").set(this, theChannel);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void send(@NotNull Packet<?> packet) {
    }

    @Override
    public void send(@NotNull Packet<?> packet, @Nullable PacketSendListener callbacks) {
    }

    public void send(@NotNull Packet<?> packet, @Nullable PacketSendListener callbacks, boolean flush) {
    }

    private PacketEncoder getEncoder() {
        if (Bukkit.getMinecraftVersion().equals("1.20.2")) {
            return new PacketEncoder(ATTRIBUTE_SERVERBOUND_PROTOCOL);
        } else {
            try {
                return PacketEncoder.class.getDeclaredConstructor(PacketFlow.class).newInstance(PacketFlow.SERVERBOUND);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private PacketDecoder getDecoder() {
        if (Bukkit.getMinecraftVersion().equals("1.20.2")) {
            return new PacketDecoder(ATTRIBUTE_CLIENTBOUND_PROTOCOL);
        } else {
            try {
                return PacketDecoder.class.getDeclaredConstructor(PacketFlow.class).newInstance(PacketFlow.CLIENTBOUND);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void setupChannel(Channel channel) {
        channel.attr(ATTRIBUTE_SERVERBOUND_PROTOCOL).set(ConnectionProtocol.PLAY.codec(PacketFlow.SERVERBOUND));
        channel.attr(ATTRIBUTE_CLIENTBOUND_PROTOCOL).set(ConnectionProtocol.PLAY.codec(PacketFlow.CLIENTBOUND));
    }
}
