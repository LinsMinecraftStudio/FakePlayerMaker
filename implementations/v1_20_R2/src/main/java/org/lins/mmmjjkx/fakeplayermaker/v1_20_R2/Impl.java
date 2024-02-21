package org.lins.mmmjjkx.fakeplayermaker.v1_20_R2;

import com.mojang.authlib.GameProfile;
import io.github.linsminecraftstudio.fakeplayermaker.api.implementation.Implementations;
import io.github.linsminecraftstudio.fakeplayermaker.api.objects.EmptyConnection;
import io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils.handlePlugins;

public class Impl extends Implementations {
    @Override
    public @NotNull GameProfile profile(ServerPlayer player) {
        return player.getGameProfile();
    }

    @Override
    public void setConnection(ServerPlayer player, ServerGamePacketListenerImpl connection) {
        player.connection = connection;
    }

    @Override
    public void placePlayer(Connection connection, ServerPlayer player) {
        try {
            CompletableFuture.runAsync(() -> {
                PlayerList list = getPlayerList();
                list.placeNewPlayer(connection, player, CommonListenerCookie.createInitial(this.profile(player)));
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        handlePlugins(bukkitEntity(player));
    }

    @Override
    public ServerPlayer create(@NotNull ServerLevel level, @NotNull GameProfile profile) {
        ServerPlayer player = new ServerPlayer(MinecraftUtils.getNMSServer(), level, profile, ClientInformation.createDefault());
        setConnection(player, MinecraftUtils.getGamePacketListener(new EmptyConnection(), player));
        return player;
    }

    @Override
    public PlayerList getPlayerList() {
        return MinecraftUtils.getNMSServer().getPlayerList();
    }

    @Override
    public void removePlayer(ServerPlayer serverPlayer) {
        getPlayerList().remove(serverPlayer);
    }
}
