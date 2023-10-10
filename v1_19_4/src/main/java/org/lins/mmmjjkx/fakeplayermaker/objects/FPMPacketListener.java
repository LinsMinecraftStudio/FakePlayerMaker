package org.lins.mmmjjkx.fakeplayermaker.objects;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;

import java.util.Set;

public class FPMPacketListener extends ServerGamePacketListenerImpl {
    public FPMPacketListener(Connection connection, ServerPlayer player) {
        super(FakePlayerMaker.getNMSServer(), connection, player);
    }

    @Override
    public void send(Packet<?> packet) {
    }

    @Override
    public void send(Packet<?> packet, @Nullable PacketSendListener callbacks) {
    }

    @Override
    public void internalTeleport(double d0, double d1, double d2, float f, float f1, @NotNull Set<RelativeMovement> set) {
        super.internalTeleport(d0, d1, d2, f, f1, set);

        if (player.getLevel().getPlayerByUUID(player.getUUID()) != null) {
            resetPosition();
            player.getLevel().getChunkSource().move(player);
        }
    }
}
