package io.github.linsminecraftstudio.fakeplayermaker.api.objects;

import io.github.linsminecraftstudio.fakeplayermaker.api.implementation.Implementations;
import io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * A packet listener on version 1.20.2
 */
public class FPMPacketListener extends ServerGamePacketListenerImpl {
    public FPMPacketListener(Connection connection, ServerPlayer player) {
        super(MinecraftUtils.getNMSServer(), connection, player, CommonListenerCookie.createInitial(Implementations.get().profile(player)));
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

        if (player.serverLevel().getPlayerByUUID(player.getUUID()) != null) {
            resetPosition();
            player.serverLevel().getChunkSource().move(player);
        }
    }
}
