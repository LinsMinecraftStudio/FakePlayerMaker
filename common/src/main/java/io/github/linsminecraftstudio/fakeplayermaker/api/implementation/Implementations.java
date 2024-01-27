package io.github.linsminecraftstudio.fakeplayermaker.api.implementation;

import com.google.common.base.Preconditions;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import static io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils.getCraftClass;

/**
 * Just an implementation in different versions of Minecraft.
 */
public abstract class Implementations {
    private static Implementations current;

    public Implementations() {
    }

    public static void register(Implementations implementations) {
        current = implementations;
    }

    public static Implementations get() {
        return current;
    }

    public static Player bukkitEntity(ServerPlayer player){
        Preconditions.checkNotNull(player);

        try {
            return (Player) getCraftClass("entity.CraftEntity")
                    .getMethod("getEntity", getCraftClass("CraftServer"), net.minecraft.world.entity.Entity.class).invoke(null, Bukkit.getServer(), player);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getName(ServerPlayer player) {
        return get().profile(player).getName();
    }

    public static UUID getUUID(ServerPlayer player) {
        return get().profile(player).getId();
    }

    public abstract GameProfile profile(ServerPlayer player);
    public abstract void setConnection(ServerPlayer player, ServerGamePacketListenerImpl connection);
    public abstract void placePlayer(Connection connection, ServerPlayer player);

    public abstract PlayerList getPlayerList();

    public abstract ServerPlayer create(@NotNull ServerLevel level, GameProfile profile);
}