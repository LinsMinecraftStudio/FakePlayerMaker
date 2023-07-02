package org.lins.mmmjjkx.fakeplayermaker.utils;

import com.mojang.authlib.GameProfile;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;

import java.util.*;

public class NMSFakePlayerMaker {
    public static Map<String, ServerPlayer> fakePlayerMap = new HashMap<>();
    private static final FakePlayerSaver saver = FakePlayerMaker.fakePlayerSaver;

    static void reloadMap(List<ServerPlayer> players){
        fakePlayerMap.clear();
        for (ServerPlayer player : players) {
            fakePlayerMap.put(player.getName().getString(), player);
        }
    }

    public static void spawnFakePlayer(Location loc, String name){
        if (name == null || name.isBlank()) name = getRandomName(FakePlayerMaker.randomNameLength);
        MinecraftServer server = MinecraftServer.getServer();

        World world;
        double x, y, z;
        float pitch, yaw;
        if (loc == null) {
            world = Bukkit.getWorlds().get(0);
            x = 0;
            y = 70;
            z = 0;
            pitch = 0f;
            yaw = 0f;
        } else {
            world = loc.getWorld();
            x = loc.getX();
            y = loc.getY();
            z = loc.getZ();
            pitch = loc.getPitch();
            yaw = loc.getYaw();
        }

        ServerPlayer player = new ServerPlayer(server, (ServerLevel) getHandle(getCraftClass("CraftWorld"), world), new GameProfile(Bukkit.getOfflinePlayer(name).getUniqueId(), name));

        player.forceSetPositionRotation(x, y, z, pitch, yaw);

        for(Player all : Bukkit.getOnlinePlayers()){
            sendPacket(all, new ClientboundAddPlayerPacket(player));
            sendPacket(all, new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, player));
        }

        fakePlayerMap.put(name, player);
        saver.addFakePlayer(player);
        if (FakePlayerMaker.specialFeatures.firePlayerJoinEvent()) {
            try {
                Bukkit.getPluginManager().callEvent(new PlayerJoinEvent(player.getBukkitEntity(),
                        Component.translatable("multiplayer.player.joined")));
            } catch (Exception e) {
                e.printStackTrace();
                FakePlayerMaker.INSTANCE.getLogger().warning("Can't call a player join event");
            }
        }
    }
    public static void removeFakePlayer(String name){
        ServerPlayer player = fakePlayerMap.get(name);
        if (player != null) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                sendPacket(onlinePlayer, new ClientboundPlayerInfoRemovePacket(Collections.singletonList(player.getUUID())));
            }
            fakePlayerMap.remove(name);
            saver.removeFakePlayer(name);
            if (FakePlayerMaker.specialFeatures.firePlayerQuitEvent()) firePlayerQuitEvent(player);
        }
    }
    public static void removeAllFakePlayers(){
        for (String name : fakePlayerMap.keySet()) {
            ServerPlayer fakePlayer = fakePlayerMap.get(name);
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                sendPacket(onlinePlayer, new ClientboundPlayerInfoRemovePacket(Collections.singletonList(fakePlayer.getUUID())));
                saver.removeFakePlayer(name);
                if (FakePlayerMaker.specialFeatures.firePlayerQuitEvent()) firePlayerQuitEvent(fakePlayer);
            }
        }
        fakePlayerMap.clear();
    }
    private static String getRandomName(int length) {
        char[] characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
        StringBuilder builder = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            builder.append(characters[random.nextInt(length)]);
        }
        return builder.toString();
    }

    private static void firePlayerQuitEvent(ServerPlayer player) {
        try {
            Bukkit.getPluginManager().callEvent(new PlayerQuitEvent(player.getBukkitEntity(),
                    Component.translatable("multiplayer.player.left"), PlayerQuitEvent.QuitReason.DISCONNECTED));
        } catch (Exception e) {
            e.printStackTrace();
            FakePlayerMaker.INSTANCE.getLogger().warning("Could not call a player quit event");
        }
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

    private static Object getHandle(Class<?> craftClazz, Object obj){
        try {
            return craftClazz.getDeclaredMethod("getHandle").invoke(craftClazz.cast(obj));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendPacket(Player bukkitPlayer, Packet<?> packet){
        Class<?> craftPlayerClass = getCraftClass("entity.CraftPlayer");
        try {
            ServerPlayer serverPlayer = (ServerPlayer) getHandle(craftPlayerClass, bukkitPlayer);
            serverPlayer.connection.send(packet);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
