package org.lins.mmmjjkx.fakeplayermaker.objects;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketDecoder;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public final class EmptyConnection extends Connection {

    public EmptyConnection(PacketFlow side) {
        super(side);
        this.channel = new EmbeddedChannel();
        this.channel.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 60000));
        this.channel.pipeline().addLast("encoder", new PacketEncoder(PacketFlow.SERVERBOUND));
        this.channel.pipeline().addLast("decoder", new PacketDecoder(PacketFlow.CLIENTBOUND));
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
}
