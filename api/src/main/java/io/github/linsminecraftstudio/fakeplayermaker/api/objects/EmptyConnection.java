package io.github.linsminecraftstudio.fakeplayermaker.api.objects;

import io.netty.channel.*;
import io.netty.util.internal.SocketUtils;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.util.SampleLogger;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public final class EmptyConnection extends Connection {

    public EmptyConnection() {
        super(PacketFlow.SERVERBOUND);

        EmptyChannel theChannel = new EmptyChannel();
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

    private static class EmptyChannel extends AbstractChannel {
        private final static EventLoop EVENT_LOOP = new DefaultEventLoop();
        private final DefaultChannelConfig config = new DefaultChannelConfig(this);

        public EmptyChannel() {
            super(null);
        }

        @Override
        public ChannelConfig config() {
            config.setAutoRead(true);
            return config;
        }

        @Override
        protected void doBeginRead() {
        }

        @Override
        protected void doBind(@NotNull SocketAddress arg0) throws Exception {
            SocketUtils.bind(java.nio.channels.SocketChannel.open(), arg0);
        }

        @Override
        protected void doClose() {
        }

        @Override
        protected void doDisconnect() {
        }

        @Override
        protected void doWrite(ChannelOutboundBuffer arg0) {
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        protected boolean isCompatible(EventLoop arg0) {
            return true;
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        protected SocketAddress localAddress0() {
            return new InetSocketAddress(60000);
        }

        @Override
        public ChannelMetadata metadata() {
            return new ChannelMetadata(true);
        }

        @Override
        protected AbstractUnsafe newUnsafe() {
            return new AbstractUnsafe() {
                @Override
                public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
                    safeSetSuccess(promise);
                }
            };
        }

        @Override
        protected SocketAddress remoteAddress0() {
            return new InetSocketAddress(60000);
        }

        @Override
        public EventLoop eventLoop() {
            return EVENT_LOOP;
        }
    }
}
