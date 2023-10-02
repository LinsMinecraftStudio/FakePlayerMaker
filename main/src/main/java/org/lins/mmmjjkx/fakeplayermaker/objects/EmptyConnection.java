package org.lins.mmmjjkx.fakeplayermaker.objects;

import com.mojang.authlib.GameProfile;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

public final class EmptyConnection extends Connection {

    public EmptyConnection(PacketFlow side, GameProfile profile) {
        super(side);
        setup(profile);
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void send(Packet<?> packet) {
    }

    @Override
    public void send(Packet<?> packet, @Nullable PacketSendListener callbacks) {
    }

    private void setup(GameProfile profile) {
        ChannelFuture future = new Bootstrap()
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        channel.config().setOption(ChannelOption.TCP_NODELAY, true);

                        channel.pipeline().addLast("packet_handler", new Connection(PacketFlow.CLIENTBOUND));
                    }})
                .channel(NioSocketChannel.class)
                .group(Connection.NETWORK_WORKER_GROUP.get())
                .localAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 60000))
                .connect("127.0.0.1", Bukkit.getPort());

        future.addListener((ChannelFutureListener) future1 -> {
            if (!future1.isSuccess()) {
                future1.cause().printStackTrace();
            }
        });

        try {
            future.sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        configureSerialization(future.channel().pipeline(), PacketFlow.SERVERBOUND);

        Connection.NETWORK_WORKER_GROUP.get().register(future.channel());
        this.channel = future.channel();
        this.address = this.channel.remoteAddress();

        setProtocol(ConnectionProtocol.LOGIN);
    }
}
