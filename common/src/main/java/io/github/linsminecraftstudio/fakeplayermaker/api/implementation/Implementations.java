package io.github.linsminecraftstudio.fakeplayermaker.api.implementation;

import com.google.common.base.Preconditions;
import com.mojang.authlib.GameProfile;
import io.github.linsminecraftstudio.fakeplayermaker.api.objects.EmptyConnection;
import io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils;
import net.minecraft.network.Connection;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils.getCraftClass;
import static io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils.handlePlugins;

/**
 * Just an implementation in different versions of Minecraft.
 */
public abstract class Implementations {
    private static final Map<String,Implementations> map = new HashMap<>();
    private static final Class<? extends PlayerList> playerListClass = PlayerList.class;

    protected Implementations() {
        register();
    }

    public static void setup() {
        new v1_20_1();
        new v1_20_2();
    }

    public final void register() {
        for (String version : minecraftVersion()) {
            if (!map.containsKey(version)) {
                map.put(version, this);
            }
        }
    }

    public static Implementations get() {
        return map.get(Bukkit.getMinecraftVersion());
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
    public abstract @NotNull String[] minecraftVersion();
    public abstract void placePlayer(Connection connection, ServerPlayer player);

    public abstract PlayerList getPlayerList();
    public abstract ServerPlayer create(@NotNull ServerLevel level, @NotNull GameProfile profile);

    private static class v1_20_1 extends Implementations {
        private Method placePlayer;

        public v1_20_1() {
            try {
                placePlayer = playerListClass.getMethod("a", Connection.class, ServerPlayer.class);
            } catch (NoSuchMethodException ignored) {
            }
        }

        @Override
        public @NotNull GameProfile profile(ServerPlayer player) {
            try {
                return (GameProfile) player.getClass().getField("cp").get(player);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void setConnection(ServerPlayer player, @NotNull ServerGamePacketListenerImpl connection) {
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

        @Override
        public ServerPlayer create(@NotNull ServerLevel level, @NotNull GameProfile profile) {
            try {
                ServerPlayer player = ServerPlayer.class.getDeclaredConstructor(MinecraftServer.class, ServerLevel.class, GameProfile.class)
                        .newInstance(MinecraftUtils.getNMSServer(), level, profile);
                setConnection(player, MinecraftUtils.getGamePacketListener(new EmptyConnection(), player));
                return player;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void placePlayer(Connection connection, ServerPlayer player) {
            try {
                CompletableFuture.runAsync(() -> {
                    try {
                        placePlayer.invoke(this.getPlayerList(), connection, player);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                    handlePlugins(bukkitEntity(player));
                }).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public PlayerList getPlayerList() {
            try {
                return (PlayerList) MinecraftServer.class.getMethod("ac").invoke(MinecraftUtils.getNMSServer());
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
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
    }
}