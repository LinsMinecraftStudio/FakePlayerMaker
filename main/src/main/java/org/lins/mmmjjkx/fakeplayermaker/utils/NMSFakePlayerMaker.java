package org.lins.mmmjjkx.fakeplayermaker.utils;

import com.mojang.authlib.GameProfile;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.FakePlayerCreateEvent;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.FakePlayerRemoveEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class NMSFakePlayerMaker {
    public static Map<String, ServerPlayer> fakePlayerMap = new HashMap<>();
    private static final FakePlayerSaver saver = FakePlayerMaker.fakePlayerSaver;

    static void reloadMap(List<ServerPlayer> players){
        fakePlayerMap.clear();
        for (ServerPlayer player : players) {
            fakePlayerMap.put(player.getName().getString(), player);
            MinecraftServer.getServer().getPlayerList().remove(player);
            MinecraftServer.getServer().getPlayerList().placeNewPlayer(player.connection.connection, player);
        }
    }

    public static void spawnFakePlayer(Location loc, String name,@Nullable CommandSender sender){
        if (name == null || name.isBlank()) {
            name = getRandomName(FakePlayerMaker.randomNameLength);
        }

        MinecraftServer server = MinecraftServer.getServer();
        Location realLoc = loc != null ? loc : FakePlayerMaker.settings.getLocation("defaultSpawnLocation");

        if (realLoc == null) {
            FakePlayerMaker.INSTANCE.getLogger().warning("Failed to create a fake player, the default spawn location is null");
            return;
        }

        ServerPlayer player = new ServerPlayer(server, (ServerLevel) getHandle(getCraftClass("CraftWorld"), realLoc.getWorld()), new GameProfile(Bukkit.getOfflinePlayer(name).getUniqueId(), name));
        Player craftPlayer = (Player) player.getBukkitEntity();
        new FakePlayerCreateEvent(craftPlayer, sender);

        player.getBukkitEntity().teleport(realLoc);

        fakePlayerMap.put(name, player);
        saver.syncPlayerInfo(player);
        MinecraftServer.getServer().getPlayerList().placeNewPlayer(player.connection.connection, player);
    }

    public static void removeFakePlayer(String name,@Nullable CommandSender sender){
        ServerPlayer player = fakePlayerMap.get(name);
        if (player != null) {
            new FakePlayerRemoveEvent(player.getBukkitEntity(), sender).callEvent();
            fakePlayerMap.remove(name);
            saver.removeFakePlayer(name);
            MinecraftServer.getServer().getPlayerList().remove(player);
        }
    }

    public static void removeAllFakePlayers(@Nullable CommandSender sender){
        for (String name : fakePlayerMap.keySet()) {
            removeFakePlayer(name, sender);
        }
        fakePlayerMap.clear();
    }
    public static String getRandomName(int length) {
        char[] characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
        StringBuilder builder = new StringBuilder(length);
        Random random = new Random();
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
            e.printStackTrace();
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
}
