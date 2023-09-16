package org.lins.mmmjjkx.fakeplayermaker.objects;

import io.netty.channel.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;

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

    private static class EmptyChannel extends AbstractChannel {

        public EmptyChannel() {
            super(null);
            this.pipeline().addFirst("encoder", new EmptyEncoder());
            this.pipeline().addFirst("decoder", new EmptyDecoder());
        }

        private final ChannelConfig config = new DefaultChannelConfig(this);

        @Override
        public ChannelConfig config() {
            return config;
        }

        @Override
        protected void doBind(SocketAddress arg0) {
        }

        @Override
        protected void doClose() {
        }

        @Override
        protected void doBeginRead() {
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
            return false;
        }

        @Override
        protected SocketAddress localAddress0() {
            return new InetSocketAddress(InetAddress.getLoopbackAddress().getHostName(), 60000);
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
            return new InetSocketAddress(InetAddress.getLoopbackAddress().getHostName(), 60000);
        }
    }
}
