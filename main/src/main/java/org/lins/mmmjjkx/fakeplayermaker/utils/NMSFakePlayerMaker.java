package org.lins.mmmjjkx.fakeplayermaker.utils;

import com.comphenix.protocol.injector.temporary.MinimalInjector;
import com.comphenix.protocol.injector.temporary.TemporaryPlayerFactory;
import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.google.common.base.Strings;
import com.mojang.authlib.GameProfile;
import fr.xephi.authme.api.v3.AuthMeApi;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.FakePlayerCreateEvent;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.FakePlayerRemoveEvent;
import io.github.linsminecraftstudio.polymer.objects.plugin.SimpleSettingsManager;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.PacketFlow;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.hook.protocol.FPMTempPlayerFactory;
import org.lins.mmmjjkx.fakeplayermaker.objects.EmptyConnection;
import su.nexmedia.engine.NexPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Level;

public class NMSFakePlayerMaker {
    public static Map<String, ServerPlayer> fakePlayerMap = new HashMap<>();
    private static final FakePlayerSaver saver = FakePlayerMaker.fakePlayerSaver;
    private static final MinecraftServer server = MinecraftServer.getServer();
    private static final SimpleSettingsManager settings = FakePlayerMaker.settings;
    private static final Method getEntity;

    static {
        try {
            getEntity = getCraftClass("entity.CraftEntity").getMethod("getEntity", getCraftClass("CraftServer"), net.minecraft.world.entity.Entity.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    static void reloadMap(List<ServerPlayer> players){
        new BukkitRunnable() {
            @Override
            public void run() {
                fakePlayerMap.clear();
                for (ServerPlayer player : players) {
                    if (server.getPlayerList().players.contains(player)) {
                        server.getPlayerList().remove(player);
                    }

                    fakePlayerMap.put(player.getName().getString(), player);
                    var connection = new EmptyConnection(PacketFlow.CLIENTBOUND);
                    var listener = new ServerGamePacketListenerImpl(server, connection, player);

                    server.getPlayerList().placeNewPlayer(connection, player);
                    simulateLogin(player);

                    player.setInvulnerable(settings.getBoolean("player.invulnerable"));
                    player.bukkitPickUpLoot = settings.getBoolean("player.canPickupItems");
                    player.collides = settings.getBoolean("player.collision");

                    runCMDAdd(player, connection, listener);

                    preventListen();
                }
            }
        }.runTaskLater(FakePlayerMaker.INSTANCE, 10);
    }

    private static void runCMDAdd(ServerPlayer player, EmptyConnection connection, ServerGamePacketListenerImpl listener) {
        connection.setListener(listener);

        Player p;
        try {
            p = (Player) getEntity.invoke(null, Bukkit.getServer(), player);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        for (String cmd : FakePlayerMaker.settings.getStrList("runCMDAfterJoin")) {
            Bukkit.dispatchCommand(p, cmd);
        }
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

        if (FakePlayerMaker.isProtocolLibLoaded()) {
            try {
                Player temp = FPMTempPlayerFactory.createPlayer(Bukkit.getServer(), name);
                MinimalInjector injector = TemporaryPlayerFactory.getInjectorFromPlayer(temp);
                ServerPlayer handle = (ServerPlayer) getHandle(getCraftClass("entity.CraftPlayer"), injector.getPlayer());
                fakePlayerMap.put(name, handle);

                if (handle != null) {
                    saver.syncPlayerInfo(handle);
                }

                new FakePlayerCreateEvent(temp, sender).callEvent();
                playerJoin(server, handle);

                temp.teleport(realLoc);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            ServerLevel level = (ServerLevel) Objects.requireNonNull(getHandle(getCraftClass("CraftWorld"), realLoc.getWorld()));
            ServerPlayer player = new ServerPlayer(server, level, new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name));
            var connection = new EmptyConnection(PacketFlow.CLIENTBOUND);
            var listener = new ServerGamePacketListenerImpl(server, connection, player);

            fakePlayerMap.put(name, player);
            saver.syncPlayerInfo(player);

            new FakePlayerCreateEvent(player.getBukkitEntity(), sender).callEvent();
            playerJoin(server, player, connection, listener);

            player.teleportTo(level, realLoc.getX(), realLoc.getY(), realLoc.getZ(), realLoc.getYaw(), realLoc.getPitch());
        }
    }

    private static void playerJoin(MinecraftServer server, ServerPlayer handle) {
        playerJoin(server, handle, new EmptyConnection(PacketFlow.CLIENTBOUND), new ServerGamePacketListenerImpl(server, new EmptyConnection(PacketFlow.CLIENTBOUND), handle));
    }

    private static void playerJoin(MinecraftServer server, ServerPlayer player, EmptyConnection connection, ServerGamePacketListenerImpl listener) {
        MinecraftServer.getServer().playerDataStorage.save(player);

        server.getPlayerList().placeNewPlayer(connection, player);
        simulateLogin(player);

        player.connection = listener;
        player.setShiftKeyDown(false);

        preventListen();

        runCMDAdd(player, connection, listener);

        player.setInvulnerable(settings.getBoolean("player.invulnerable"));
        player.bukkitPickUpLoot = settings.getBoolean("player.canPickupItems");
        player.collides = settings.getBoolean("player.collision");

        preventListen();
    }

    public static void joinFakePlayer(String name){
        ServerPlayer player = fakePlayerMap.get(name);
        if (player != null) {
            var connection = new EmptyConnection(PacketFlow.CLIENTBOUND);
            var listener = new ServerGamePacketListenerImpl(server, connection, player);

            playerJoin(server, player, connection, listener);
        }
    }

    public static void removeFakePlayer(String name,@Nullable CommandSender sender){
        ServerPlayer player = fakePlayerMap.get(name);
        if (player != null) {
            new FakePlayerRemoveEvent(player.getName().getString(), sender).callEvent();
            fakePlayerMap.remove(name);
            saver.removeFakePlayer(name);
            server.getPlayerList().remove(player);

            if (Bukkit.getPluginManager().isPluginEnabled("AuthMe")) {
                AuthMeApi.getInstance().forceUnregister(player.getName().getString());
            }
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
            FakePlayerMaker.INSTANCE.getLogger().log(Level.SEVERE, "Could not get handle of " + obj + " the class is " + craftClazz.getName() +",");
        }
        return null;
    }

    public static void simulateLogin(ServerPlayer p) {
        InetAddress fakeNetAddress = InetAddress.getLoopbackAddress();

        new BukkitRunnable() {
            @Override
            public void run() {
                new AsyncPlayerPreLoginEvent(
                        p.getName().getString(),
                        fakeNetAddress,
                        fakeNetAddress,
                        UUIDUtil.createOfflinePlayerUUID(p.getName().getString()),
                        new CraftPlayerProfile(UUIDUtil.createOfflinePlayerUUID(p.getName().getString()), p.getName().getString()),
                        fakeNetAddress.getHostName()
                ).callEvent();
            }
        }.runTaskAsynchronously(FakePlayerMaker.INSTANCE);

        try {
            new PlayerLoginEvent(
                    (Player) getEntity.invoke(null, Bukkit.getServer(), p),
                    fakeNetAddress.getHostName(),
                    fakeNetAddress
            ).callEvent();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static void preventListen() {
        if (Bukkit.getPluginManager().isPluginEnabled("NexEngine")) {
            FakePlayerMaker.unregisterHandlers(NexPlugin.class);
        }
    }
}
