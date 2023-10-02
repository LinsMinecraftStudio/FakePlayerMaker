package org.lins.mmmjjkx.fakeplayermaker.implementation;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getCraftClass;

public abstract class Implementations {
    private static final Map<String,Implementations> map = new HashMap<>();

    public static void register(Implementations impl) {
        for (String version : impl.minecraftVersion()) {
            map.put(version, impl);
        }
    }

    public static void runImpl(Consumer<Implementations> consumer) {
        consumer.accept(map.get(Bukkit.getMinecraftVersion()));
    }

    public static <T> T runImplAndReturn(Function<Implementations, T> function) {
        return function.apply(map.get(Bukkit.getMinecraftVersion()));
    }

    public final Player bukkitEntity(ServerPlayer player){
        try {
            return (Player) getCraftClass("entity.CraftEntity")
                    .getMethod("getEntity", getCraftClass("CraftServer"), net.minecraft.world.entity.Entity.class).invoke(null, Bukkit.getServer(), player);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract GameProfile profile(ServerPlayer player);
    public abstract void setConnection(ServerPlayer player, ServerGamePacketListenerImpl connection);
    public abstract @NotNull String[] minecraftVersion();
}
