package org.lins.mmmjjkx.fakeplayermaker.objects;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.NotNull;

public class EmptyGamePackListener extends ServerGamePacketListenerImpl {
    public EmptyGamePackListener(MinecraftServer server, Connection connection, ServerPlayer player) {
        super(server, connection, player);
    }

    @Override
    public void send(@NotNull Packet<?> packet) {
        super.send(packet);
    }
}
