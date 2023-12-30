package org.lins.mmmjjkx.fakeplayermaker.impl_1201;

import com.mojang.authlib.GameProfile;
import io.github.linsminecraftstudio.fakeplayermaker.api.implementation.Implementations;
import io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.NotNull;

public class V1201Impl extends Implementations {
    @Override
    public GameProfile profile(ServerPlayer player) {
        return player.getGameProfile();
    }

    @Override
    public void setConnection(ServerPlayer player, ServerGamePacketListenerImpl connection) {
        player.connection = connection;
    }

    @Override
    public @NotNull String[] minecraftVersion() {
        return new String[]{"1.20.1"};
    }

    @Override
    public void placePlayer(Connection connection, ServerPlayer player) {
        getPlayerList().placeNewPlayer(connection, player);
    }

    @Override
    public PlayerList getPlayerList() {
        return MinecraftUtils.getNMSServer().getPlayerList();
    }

    @Override
    public ServerPlayer create(@NotNull ServerLevel level, @NotNull GameProfile profile) {
        return new ServerPlayer(MinecraftUtils.getNMSServer(), level, profile);
    }
}
