package org.lins.mmmjjkx.fakeplayermaker.objects;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketDecoder;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public final class EmptyConnection extends Connection {

    public EmptyConnection() {
        super(PacketFlow.CLIENTBOUND);

        EmbeddedChannel theChannel = new EmbeddedChannel();
        theChannel.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 60000));
        theChannel.pipeline().addLast("encoder", new PacketEncoder(PacketFlow.SERVERBOUND));
        theChannel.pipeline().addLast("decoder", new PacketDecoder(PacketFlow.CLIENTBOUND));

        this.channel = theChannel;
        this.address = theChannel.localAddress();
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
}
