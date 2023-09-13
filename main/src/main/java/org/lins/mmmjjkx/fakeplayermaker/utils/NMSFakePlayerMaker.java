package org.lins.mmmjjkx.fakeplayermaker.utils;

import com.comphenix.protocol.ProtocolLib;
import com.google.common.base.Strings;
import com.mojang.authlib.GameProfile;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.api.v3.AuthMeApi;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.FakePlayerCreateEvent;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.FakePlayerRemoveEvent;
import io.github.linsminecraftstudio.polymer.objects.plugin.SimpleSettingsManager;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.objects.EmptyConnection;
import org.lins.mmmjjkx.fakeplayermaker.objects.EmptyGamePackListener;
import org.lins.mmmjjkx.fakeplayermaker.objects.EmptyLoginPacketListener;
import su.nexmedia.engine.NexPlugin;

import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Level;

public class NMSFakePlayerMaker {
    public static Map<String, ServerPlayer> fakePlayerMap = new HashMap<>();
    private static final FakePlayerSaver saver = FakePlayerMaker.fakePlayerSaver;
    private static final MinecraftServer server = MinecraftServer.getServer();
    private static final InetAddress fakeAddress = InetAddress.getLoopbackAddress();
    private static final SimpleSettingsManager settings = FakePlayerMaker.settings;

    static void reloadMap(List<ServerPlayer> players){
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ServerPlayer player : players) {
                    if (server.getPlayerList().players.contains(player)) {
                        server.getPlayerList().remove(player);
                    }

                    fakePlayerMap.put(player.getName().getString(), player);
                    var connection = new EmptyConnection(PacketFlow.CLIENTBOUND);
                    var listener = new EmptyGamePackListener(server, connection, player);
                    var listener2 = new EmptyLoginPacketListener(server, connection);
                    connection.setListener(listener);
                    server.getPlayerList().placeNewPlayer(connection, player);

                    player.setInvulnerable(settings.getBoolean("player.invulnerable"));

                    Player bukkitPlayer = player.getBukkitEntity();
                    bukkitPlayer.setCanPickupItems(settings.getBoolean("player.canPickupItems"));
                    bukkitPlayer.setCollidable(settings.getBoolean("player.collision"));

                    simulateLogin(player);

                    connection.setListener(listener2);

                    if (FakePlayerMaker.isAuthmeOn()) {
                        AuthMeApi.getInstance().forceLogin(player.getBukkitEntity());
                    }

                    preventListen();
                }
            }
        }.runTaskLater(FakePlayerMaker.INSTANCE, 10);
    }

    public static void spawnFakePlayer(Location loc, String name, @Nullable CommandSender sender){
        if (name == null || name.isBlank()) {
            name = getRandomName(FakePlayerMaker.randomNameLength);
        }

        MinecraftServer server = MinecraftServer.getServer();
        Location realLoc = loc != null ? loc : FakePlayerMaker.settings.getLocation("defaultSpawnLocation");

        if (realLoc == null) {
            FakePlayerMaker.INSTANCE.getLogger().warning("Failed to create a fake player, the default spawn location is null");
            return;
        }

        if (!Strings.isNullOrEmpty(FakePlayerMaker.settings.getString("namePrefix"))) {
            name = FakePlayerMaker.settings.getString("namePrefix") + name;
        }

        ServerPlayer player = new ServerPlayer(server, (ServerLevel) getHandle(getCraftClass("CraftWorld"), realLoc.getWorld()), new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name));
        var connection = new EmptyConnection(PacketFlow.CLIENTBOUND);
        var listener = new EmptyGamePackListener(server, connection, player);
        var listener2 = new EmptyLoginPacketListener(server, connection);

        fakePlayerMap.put(name, player);
        saver.syncPlayerInfo(player);

        new FakePlayerCreateEvent(player.getBukkitEntity(), sender).callEvent();
        playerJoin(server, player, connection, listener, listener2);

        listener.teleport(realLoc);
    }

    private static void playerJoin(MinecraftServer server, ServerPlayer player, EmptyConnection connection, EmptyGamePackListener listener, EmptyLoginPacketListener listener2) {
        connection.setListener(listener);

        server.getPlayerList().placeNewPlayer(connection, player);

        simulateLogin(player);
        player.connection = listener;
        player.setShiftKeyDown(false);

        preventListen();

        connection.setListener(listener2);

        if (FakePlayerMaker.isAuthmeOn()) {
            if (!AuthMeApi.getInstance().isRegistered(player.getName().getString())) {
                AuthMeApi.getInstance().forceRegister(player.getBukkitEntity(), getRandomName(10).replace(FakePlayerMaker.settings.getString("namePrefix"), ""));
            }
            AuthMeApi.getInstance().forceLogin(player.getBukkitEntity());
        }

        player.setInvulnerable(settings.getBoolean("player.invulnerable"));

        Player bukkitPlayer = player.getBukkitEntity();
        bukkitPlayer.setCanPickupItems(settings.getBoolean("player.canPickupItems"));
        bukkitPlayer.setCollidable(settings.getBoolean("player.collision"));

        preventListen();
    }

    public static void joinFakePlayer(String name){
        ServerPlayer player = fakePlayerMap.get(name);
        if (player != null) {
            var connection = new EmptyConnection(PacketFlow.CLIENTBOUND);
            var listener = new EmptyGamePackListener(server, connection, player);
            var listener2 = new EmptyLoginPacketListener(server, connection);

            playerJoin(server, player, connection, listener, listener2);
        }
    }

    public static void removeFakePlayer(String name,@Nullable CommandSender sender){
        ServerPlayer player = fakePlayerMap.get(name);
        if (player != null) {
            new FakePlayerRemoveEvent(player.getName().getString(), sender).callEvent();
            fakePlayerMap.remove(name);
            saver.removeFakePlayer(name);
            server.getPlayerList().remove(player);
        }
    }

    public static void removeAllFakePlayers(@Nullable CommandSender sender) {
        Iterator<String> iterator = fakePlayerMap.keySet().iterator();
        while (iterator.hasNext()) {
            String name = iterator.next();
            removeFakePlayer(name, sender);
            iterator.remove();
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
        } catch(Exception e) {
            FakePlayerMaker.INSTANCE.getLogger().log(Level.WARNING, "Could not find CraftBukkit class for " + name + " .\n" +
                    "Maybe your server minecraft version is not compatible with the plugin or an error occurred while executing the remap task, " +
                    "you should open a issue in our github repo!(Please confirm that you are using an official plugin)", e);
        }
        return c;
    }

    public static Object getHandle(Class<?> craftClazz, Object obj){
        try {
            return craftClazz.getDeclaredMethod("getHandle").invoke(craftClazz.cast(obj));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void simulateLogin(ServerPlayer p) {
        new BukkitRunnable() {
            @Override
            public void run() {
                new AsyncPlayerPreLoginEvent(p.getName().getString(),
                        fakeAddress,
                        fakeAddress,
                        p.getUUID(),
                        p.getBukkitEntity().getPlayerProfile(),
                        fakeAddress.getHostName()
                ).callEvent();
            }
        }.runTaskAsynchronously(FakePlayerMaker.INSTANCE);

        new PlayerLoginEvent(
                p.getBukkitEntity(),
                fakeAddress.getHostName(),
                fakeAddress
        ).callEvent();
    }

    private static void preventListen() {
        if (Bukkit.getPluginManager().isPluginEnabled("NexEngine")) {
            FakePlayerMaker.unregisterHandlers(NexPlugin.class);
        }
        if (FakePlayerMaker.isAuthmeOn()) {
            FakePlayerMaker.unregisterHandlers(AuthMe.class);
        }
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            FakePlayerMaker.unregisterHandlers(ProtocolLib.class);
        }
    }
}
