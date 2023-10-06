package org.lins.mmmjjkx.fakeplayermaker.implementation;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;

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
        new v1_20_2().register();
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
    public abstract void placePlayer(Connection connection, ServerPlayer player);
    public abstract ServerPlayer create(@NotNull ServerLevel level, @NotNull GameProfile profile);
    public abstract String getName(ServerPlayer player);

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

        @Override
        public void placePlayer(Connection connection, ServerPlayer player) {
            try {
                PlayerList list = (PlayerList) MinecraftServer.class.getMethod("ac").invoke(FakePlayerMaker.getNMSServer());
                list.getClass().getMethod("a", Connection.class, ServerPlayer.class).invoke(list, connection, player);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public ServerPlayer create(@NotNull ServerLevel level, @NotNull GameProfile profile) {
            try {
                return ServerPlayer.class.getDeclaredConstructor(MinecraftServer.class, ServerLevel.class, GameProfile.class)
                        .newInstance(FakePlayerMaker.getNMSServer(), level, profile);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String getName(ServerPlayer player) {
            try {
                Component component = (Component) player.getClass().getMethod("Z").invoke(player);
                return component.getString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class v1_20_1 extends v1_19_3 {
        @Override
        public @NotNull GameProfile profile(ServerPlayer player) {
            try {
                return (GameProfile) player.getClass().getField("cp").get(player);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void setConnection(ServerPlayer player, ServerGamePacketListenerImpl connection) {
            player.connection = connection;
            /*
            Save code for future versions
            try {
                player.getClass().getField("c").set(player, connection);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }

             */
        }

        @Override
        public String[] minecraftVersion() {
            return new String[]{"1.20.1"};
        }
    }

    private static class v1_20_2 extends v1_20_1 {
        @Override
        public @NotNull GameProfile profile(ServerPlayer player) {
            return player.getGameProfile();
        }

        @Override
        public String[] minecraftVersion() {
            return new String[]{"1.20.2"};
        }

        @Override
        public void placePlayer(Connection connection, ServerPlayer player) {
            FakePlayerMaker.getNMSServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(this.profile(player)));
        }

        @Override
        public ServerPlayer create(@NotNull ServerLevel level, @NotNull GameProfile profile) {
            return new ServerPlayer(FakePlayerMaker.getNMSServer(), level, profile, ClientInformation.createDefault());
        }

        @Override
        public String getName(ServerPlayer player) {
            return player.getName().getString();
        }
    }
}