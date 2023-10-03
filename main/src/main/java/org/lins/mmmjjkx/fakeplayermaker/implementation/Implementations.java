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

    public static void setup() {
        new v1_19_3().register();
        new v1_20_1().register();
    }

    public void register() {
        for (String version : minecraftVersion()) {
            map.put(version, this);
        }
    }

    public static void runImpl(Consumer<Implementations> consumer) {
        consumer.accept(map.get(Bukkit.getMinecraftVersion()));
    }

    public static <T> T runImplAndReturn(Function<Implementations, T> function) {
        return function.apply(map.get(Bukkit.getMinecraftVersion()));
    }

    public static Player bukkitEntity(ServerPlayer player){
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

    private static class v1_19_3 extends Implementations {
        @Override
        public @NotNull GameProfile profile(ServerPlayer player) {
            try {
                return (GameProfile) player.getClass().getField("co").get(player);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void setConnection(ServerPlayer player, ServerGamePacketListenerImpl connection) {
            try {
                player.getClass().getField("b").set(player, connection);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String[] minecraftVersion() {
            return new String[]{"1.19.3","1.19.4"};
        }
    }

    private static class v1_20_1 extends Implementations {
        @Override
        public @NotNull GameProfile profile(ServerPlayer player) {
            return player.getGameProfile();
        }

        @Override
        public void setConnection(ServerPlayer player, ServerGamePacketListenerImpl connection) {
            player.connection = connection;
        }

        @Override
        public String[] minecraftVersion() {
            return new String[]{"1.20.1"};
        }
    }
}
