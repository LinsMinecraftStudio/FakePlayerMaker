package org.lins.mmmjjkx.fakeplayermaker.v1_19_R3;

import com.mojang.authlib.GameProfile;
import io.github.linsminecraftstudio.fakeplayermaker.api.implementation.Implementations;
import io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Impl extends Implementations {
    @Override
    public GameProfile profile(ServerPlayer player) {
        return player.getGameProfile();
    }

    @Override
    public void setConnection(ServerPlayer player, ServerGamePacketListenerImpl connection) {
        player.connection = connection;
    }

    @Override
    public void placePlayer(Connection connection, ServerPlayer player) {
        boolean result = MinecraftUtils.removeTheSameUUIDEntity(getUUID(player));
        if (result) {
            try {
                getPlayerList().placeNewPlayer(connection, player);
                CompletableFuture.runAsync(() -> MinecraftUtils.handlePlugins(bukkitEntity(player))).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public PlayerList getPlayerList() {
        return MinecraftUtils.getNMSServer().getPlayerList();
    }

    @Override
    public void removePlayer(ServerPlayer serverPlayer) {
        getPlayerList().remove(serverPlayer);
    }

    @Override
    public ServerPlayer create(@NotNull ServerLevel level, @NotNull GameProfile profile) {
        return new ServerPlayer(MinecraftUtils.getNMSServer(), level, profile);
    }
}
