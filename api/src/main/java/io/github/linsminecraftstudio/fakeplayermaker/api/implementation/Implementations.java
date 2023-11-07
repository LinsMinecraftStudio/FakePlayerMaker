package io.github.linsminecraftstudio.fakeplayermaker.api.implementation;

import com.google.common.base.Preconditions;
import com.mojang.authlib.GameProfile;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils.getCraftClass;

/**
 * Just an implementation in different versions of Minecraft.
 */
public abstract class Implementations {
    private static final Map<String,Implementations> map = new HashMap<>();
    private static final Class<? extends PlayerList> playerListClass = PlayerList.class;

    public static void setup() {
        new v1_20_1().register();
        new v1_20_2().register();
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
    public abstract ServerPlayer create(@NotNull ServerLevel level, @NotNull GameProfile profile);

    private static class v1_20_1 extends Implementations {
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

        @Override
        public ServerPlayer create(@NotNull ServerLevel level, @NotNull GameProfile profile) {
            try {
                return ServerPlayer.class.getDeclaredConstructor(MinecraftServer.class, ServerLevel.class, GameProfile.class)
                        .newInstance(MinecraftUtils.getNMSServer(), level, profile);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void placePlayer(Connection connection, ServerPlayer player) {
            try {
                PlayerList list = (PlayerList) MinecraftServer.class.getMethod("ac").invoke(MinecraftUtils.getNMSServer());
                playerListClass.getMethod("a", Connection.class, ServerPlayer.class).invoke(list, connection, player);
            } catch (Exception e) {
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
            PlayerList list = MinecraftUtils.getNMSServer().getPlayerList();
            list.placeNewPlayer(connection, player, CommonListenerCookie.createInitial(this.profile(player)));
        }

        @Override
        public ServerPlayer create(@NotNull ServerLevel level, @NotNull GameProfile profile) {
            return new ServerPlayer(MinecraftUtils.getNMSServer(), level, profile, ClientInformation.createDefault());
        }
    }
}