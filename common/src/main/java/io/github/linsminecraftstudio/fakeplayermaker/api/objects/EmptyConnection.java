package io.github.linsminecraftstudio.fakeplayermaker.api.objects;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.util.SampleLogger;
import org.bukkit.Bukkit;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public final class EmptyConnection extends Connection {

    public EmptyConnection() {
        super(PacketFlow.SERVERBOUND);

        FPMChannel theChannel = new FPMChannel();
        EventLoopGroup loop = new DefaultEventLoopGroup();
        loop.register(theChannel);

        theChannel.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 60000));

        configSerialization(theChannel.pipeline());

        if (Bukkit.getMinecraftVersion().equals("1.20.2")) {
            setAttributes(theChannel);

            this.channel = theChannel;
            this.address = theChannel.remoteAddress();
        } else {
            try {
                getClass().getField("m").set(this, theChannel);
                getClass().getField("n").set(this, theChannel.remoteAddress());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    private void setAttributes(Channel channel) {
        channel.attr(ATTRIBUTE_SERVERBOUND_PROTOCOL).set(ConnectionProtocol.PLAY.codec(PacketFlow.SERVERBOUND));
        channel.attr(ATTRIBUTE_CLIENTBOUND_PROTOCOL).set(ConnectionProtocol.PLAY.codec(PacketFlow.CLIENTBOUND));
    }

    private void configSerialization(ChannelPipeline pipeline) {
        if (Bukkit.getMinecraftVersion().equals("1.20.2")) {
            configureSerialization(pipeline, PacketFlow.SERVERBOUND, new BandwidthDebugMonitor(new SampleLogger()));
        } else {
            try {
                Connection.class.getMethod("a", ChannelPipeline.class, PacketFlow.class)
                        .invoke(null, pipeline, PacketFlow.SERVERBOUND);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
