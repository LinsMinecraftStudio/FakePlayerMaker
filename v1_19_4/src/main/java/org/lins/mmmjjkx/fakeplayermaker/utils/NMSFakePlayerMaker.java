package org.lins.mmmjjkx.fakeplayermaker.utils;

import com.comphenix.protocol.injector.temporary.MinimalInjector;
import com.comphenix.protocol.injector.temporary.TemporaryPlayerFactory;
import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.google.common.base.Strings;
import com.mojang.authlib.GameProfile;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.FakePlayerCreateEvent;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.FakePlayerRemoveEvent;
import io.github.linsminecraftstudio.fakeplayermaker.api.implementation.Implementations;
import io.github.linsminecraftstudio.fakeplayermaker.api.interfaces.FakePlayerController;
import io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.jetbrains.annotations.Nullable;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.hook.protocol.FPMTempPlayerFactory;
import org.lins.mmmjjkx.fakeplayermaker.objects.EmptyConnection;
import org.lins.mmmjjkx.fakeplayermaker.objects.FPMPacketListener;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Level;

public class NMSFakePlayerMaker {
    public static Map<String, ServerPlayer> fakePlayerMap = new HashMap<>();
    private static final FakePlayerSaver saver = FakePlayerMaker.fakePlayerSaver;
    private static final MinecraftServer server = MinecraftServer.getServer();

    static void reloadMap(Map<ServerPlayer, Location> players) {
        MinecraftUtils.schedule(FakePlayerMaker.INSTANCE, () -> {
            fakePlayerMap.clear();
            for (ServerPlayer player : players.keySet()) {
                if (server.getPlayerList().getPlayers().contains(player)) {
                    server.getPlayerList().remove(player);
                }

                fakePlayerMap.put(player.getName().getString(), player);
                var connection = new EmptyConnection();
                var listener = new FPMPacketListener(connection, player);

                MinecraftServer.getServer().getPlayerList().placeNewPlayer(connection, player);
                simulateLogin(player);

                ActionUtils.setupValues(player);

                runCMDs(player, connection, listener);

                preventListen();

                Location location = players.get(player);
                ServerLevel level = (ServerLevel) MinecraftUtils.getHandle(getCraftClass("CraftWorld"), location.getWorld());
                if (level != null) {
                    player.teleportTo(level, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
                }
            }
        }, 10, false);
    }

    private static void runCMDs(ServerPlayer player, EmptyConnection connection, ServerGamePacketListenerImpl listener) {
        connection.setListener(listener);

        Player p = player.getBukkitEntity();
        for (String cmd : FakePlayerMaker.settings.getStrList("runCMDAfterJoin")) {
            cmd = cmd.replaceAll("%player%", p.getName());
            if (cmd.startsWith("chat:")) {
                ActionUtils.chat(player, cmd.replace("chat:", ""));
                continue;
            }
            Bukkit.dispatchCommand(p, cmd);
        }

        for (String cmd : FakePlayerMaker.settings.getStrList("runCMDConsoleAfterJoin")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replaceAll("%player%", p.getName()));
        }
    }

    @Nullable
    public static ServerPlayer spawnFakePlayer(Location loc, String name, @Nullable CommandSender sender) {
        return spawnFakePlayer(loc, name, sender, true);
    }

    @Nullable
    public static ServerPlayer spawnFakePlayer(Location loc, String name, @Nullable CommandSender sender, boolean runCMD) {
        if (name == null || name.isBlank()) {
            name = getRandomName(FakePlayerMaker.randomNameLength);
        }

        Location realLoc = loc != null ? loc : FakePlayerMaker.settings.getLocation("defaultSpawnLocation");

        if (realLoc == null) {
            FakePlayerMaker.INSTANCE.getLogger().warning("Failed to create a fake player, the default spawn location is null");
            return null;
        }

        if (!Strings.isNullOrEmpty(FakePlayerMaker.settings.getString("namePrefix"))) {
            name = FakePlayerMaker.settings.getString("namePrefix") + name;
        }

        GameProfile profile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name);

        if (FakePlayerMaker.isProtocolLibLoaded()) {
            try {
                Player temp = FPMTempPlayerFactory.createPlayer(Bukkit.getServer(), name);
                MinimalInjector injector = TemporaryPlayerFactory.getInjectorFromPlayer(temp);
                ServerPlayer handle = (ServerPlayer) MinecraftUtils.getHandle(getCraftClass("entity.CraftPlayer"), injector.getPlayer());
                fakePlayerMap.put(name, handle);

                if (handle != null) {
                    saver.syncPlayerInfo(handle);
                }

                new FakePlayerCreateEvent(temp, sender).callEvent();
                var connection = new EmptyConnection();
                playerJoin(handle, connection, new FPMPacketListener(connection, handle), true);

                temp.teleport(realLoc);
                return handle;
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            ServerLevel level = (ServerLevel) Objects.requireNonNull(MinecraftUtils.getHandle(getCraftClass("CraftWorld"), realLoc.getWorld()));
            ServerPlayer player = new ServerPlayer(MinecraftServer.getServer(), level, profile);
            var connection = new EmptyConnection();

            fakePlayerMap.put(name, player);
            saver.syncPlayerInfo(player);

            new FakePlayerCreateEvent(player.getBukkitEntity(), sender).callEvent();
            playerJoin(player, connection, new FPMPacketListener(connection, player), runCMD);

            player.teleportTo(level, realLoc.getX(), realLoc.getY(), realLoc.getZ(), realLoc.getYaw(), realLoc.getPitch());
            return player;
        }
    }

    private static void playerJoin(ServerPlayer player, EmptyConnection connection, ServerGamePacketListenerImpl listener, boolean runCMD) {
        MinecraftServer.getServer().playerDataStorage.save(player);

        MinecraftServer.getServer().getPlayerList().placeNewPlayer(connection, player);
        simulateLogin(player);

        player.connection = listener;

        preventListen();

        ActionUtils.setupValues(player);

        if (runCMD) {
            runCMDs(player, connection, listener);
        }
    }

    public static void joinFakePlayer(String name) {
        ServerPlayer player = fakePlayerMap.get(name);
        if (player != null) {
            var connection = new EmptyConnection();
            var listener = new FPMPacketListener(connection, player);

            playerJoin(player, connection, listener, true);
        }
    }

    public static void removeFakePlayer(String name, @Nullable CommandSender sender) {
        ServerPlayer player = fakePlayerMap.get(name);
        if (player != null) {
            new FakePlayerRemoveEvent(player.getName().getString(), sender).callEvent();

            for (String cmd : FakePlayerMaker.settings.getStrList("runCMDAfterRemove")) {
                cmd = cmd.replaceAll("%player%", name);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }

            fakePlayerMap.remove(name);
            saver.removeFakePlayer(name);
            server.getPlayerList().remove(player);
        }
    }

    public static void removeAllFakePlayers(@Nullable CommandSender sender) {
        Set<String> set = new HashSet<>(fakePlayerMap.keySet());
        for (String name : set) {
            removeFakePlayer(name, sender);
        }
    }

    public static String getRandomName(int length) {
        char[] characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
        StringBuilder builder = new StringBuilder(length);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < length; i++) {
            builder.append(characters[random.nextInt(length)]);
        }
        return builder.toString();
    }

    public static Class<?> getCraftClass(String name) {
        String version = Bukkit.getServer().getClass().getName().split("\\.")[3];
        String className = "org.bukkit.craftbukkit." + version + "." + name;
        Class<?> c = null;
        try {
            c = Class.forName(className);
        } catch (Exception e) {
            FakePlayerMaker.INSTANCE.getLogger().log(Level.WARNING, "Could not find CraftBukkit class for " + name + " .\n" +
                    "Maybe your server minecraft version is not compatible with the plugin or an error occurred while executing the remap task, " +
                    "you should open a issue in our github repo!(Please confirm that you are using an official plugin)", e);
        }
        return c;
    }

    public static void simulateLogin(ServerPlayer p) {
        InetAddress fakeNetAddress = InetAddress.getLoopbackAddress();

        MinecraftUtils.schedule(FakePlayerMaker.INSTANCE, () ->
                new AsyncPlayerPreLoginEvent(
                        p.getName().getString(),
                        fakeNetAddress,
                        fakeNetAddress,
                        p.getUUID(),
                        new CraftPlayerProfile(p.getGameProfile()),
                        fakeNetAddress.getHostName()
                ).callEvent(), 0, true);

        new PlayerLoginEvent(
                p.getBukkitEntity(),
                fakeNetAddress.getHostName(),
                fakeNetAddress
        ).callEvent();
    }

    private static void preventListen() {
        if (Bukkit.getPluginManager().isPluginEnabled("NexEngine")) {
            MinecraftUtils.preventListen("su.nexmedia.engine.NexPlugin");
        }
    }

    public static FakePlayerController asController() {
        return new FakePlayerController() {
            @Override
            public boolean isFakePlayer(String name) {
                return fakePlayerMap.get(name) != null;
            }

            @Override
            public List<ServerPlayer> getAllFakePlayers() {
                return fakePlayerMap.values().stream().toList();
            }

            @Override
            public ServerPlayer spawnFakePlayer(@Nullable String name, Location location) {
                return NMSFakePlayerMaker.spawnFakePlayer(location, name, null);
            }

            @Override
            public @Nullable Player getFakePlayer(String name) {
                return Implementations.bukkitEntity(fakePlayerMap.get(name));
            }
        };
    }
}
