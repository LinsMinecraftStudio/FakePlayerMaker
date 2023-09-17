package org.lins.mmmjjkx.fakeplayermaker.objects;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class EmptyConnection extends Connection {
    public EmptyConnection(PacketFlow side) {
        super(side);
        this.channel = new EmptyChannel();
        this.address = this.channel.remoteAddress();
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

    private static class EmptyChannel extends AbstractChannel {

        private final ChannelConfig config = new DefaultChannelConfig(this);
        private final ChannelPipeline pipeline;

        public EmptyChannel() {
            super(null);
            this.pipeline = newChannelPipeline();
            this.pipeline.addFirst("encoder", new EmptyEncoder());
            this.pipeline.addFirst("decoder", new EmptyDecoder());

            EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
            ChannelFuture channelFuture = eventLoopGroup.register(this);
            channelFuture.addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    future.cause().printStackTrace();
                }
            });

            try {
                channelFuture.sync();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public ChannelConfig config() {
            return config;
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        protected void doBeginRead() {
        }

        @Override
        protected void doBind(SocketAddress localAddress) {
        }

        @Override
        protected void doClose() {
        }

        @Override
        protected void doDisconnect() {
        }

        @Override
        protected void doWrite(ChannelOutboundBuffer outboundBuffer) {
        }

        @Override
        protected boolean isCompatible(EventLoop eventLoop) {
            return true;
        }

        @Override
        protected SocketAddress localAddress0() {
            return new InetSocketAddress(InetAddress.getLoopbackAddress(), 60000);
        }

        @Override
        protected SocketAddress remoteAddress0() {
            return new InetSocketAddress(InetAddress.getLoopbackAddress(), 60000);
        }

        @Override
        public ChannelMetadata metadata() {
            return new ChannelMetadata(false);
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
        public ChannelPipeline pipeline() {
            return pipeline;
        }

        @Override
        public ByteBufAllocator alloc() {
            return config().getAllocator();
        }
    }
}
