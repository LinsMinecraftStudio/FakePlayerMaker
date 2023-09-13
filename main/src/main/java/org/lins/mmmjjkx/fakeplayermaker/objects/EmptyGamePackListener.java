package org.lins.mmmjjkx.fakeplayermaker.objects;

import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class EmptyGamePackListener extends ServerGamePacketListenerImpl {
    public EmptyGamePackListener(MinecraftServer server, Connection connection, ServerPlayer player) {
        super(server, connection, player);
    }
}
