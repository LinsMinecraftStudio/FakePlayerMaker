package org.lins.mmmjjkx.fakeplayermaker.utils;

import com.comphenix.protocol.injector.temporary.MinimalInjector;
import com.comphenix.protocol.injector.temporary.TemporaryPlayerFactory;
import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.google.common.base.Strings;
import com.mojang.authlib.GameProfile;
import io.github.linsminecraftstudio.fakeplayermaker.api.implementation.ActionImpl;
import io.github.linsminecraftstudio.fakeplayermaker.api.implementation.Implementations;
import io.github.linsminecraftstudio.fakeplayermaker.api.objects.EmptyConnection;
import io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils;
import net.kyori.adventure.text.Component;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.jetbrains.annotations.Nullable;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.hook.protocol.FPMTempPlayerFactory;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.*;

import static io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils.getHandle;

public class NMSFakePlayerMaker{
    public static Map<String, ServerPlayer> fakePlayerMap = new HashMap<>();
    private static final FakePlayerSaver saver = FakePlayerMaker.fakePlayerSaver;

    static void reloadMap(boolean removeAll, Map<ServerPlayer, Location> players) {
        MinecraftUtils.schedule(FakePlayerMaker.INSTANCE, () -> {
            if (removeAll) {
                for (ServerPlayer player : players.keySet()) {
                    if (Implementations.get().getPlayerList().getPlayer(Implementations.getUUID(player)) != null) {
                        Implementations.get().getPlayerList().remove(player);
                    }
                }
                fakePlayerMap.clear();
                return;
            }

            fakePlayerMap.clear();
            for (ServerPlayer player : players.keySet()) {
                if (Implementations.get().getPlayerList().getPlayer(Implementations.getUUID(player)) == null) {
                    EmptyConnection connection = new EmptyConnection();
                    playerJoin(player, connection, MinecraftUtils.getGamePacketListener(connection, player), true, null);
                }

                fakePlayerMap.put(Implementations.getName(player), player);

                ActionImpl.setupValues(FakePlayerMaker.settings, player);

                MinecraftUtils.preventListen("su.nexmedia.engine.NexPlugin");

                Location location = players.get(player);
                ServerLevel level = (ServerLevel) Objects.requireNonNull(getHandle(MinecraftUtils.getCraftClass("CraftWorld"), location.getWorld()));
                player.teleportTo(level, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            }
        }, 5, false);
    }

    private static void runCMDs(ServerPlayer player, EmptyConnection connection, ServerGamePacketListenerImpl listener) {
        connection.setListener(listener);

        Player p = Implementations.bukkitEntity(player);
        for (String cmd : FakePlayerMaker.settings.getStrList("runCMDAfterJoin")) {
            if (p != null) {
                cmd = cmd.replaceAll("%player%", p.getName());
            }
            if (cmd.startsWith("chat:")) {
                ActionImpl.get().chat(FakePlayerMaker.INSTANCE, player,  cmd.replace("chat:", ""));
                continue;
            }
            Bukkit.dispatchCommand(p, cmd);
        }

        for (String cmd : FakePlayerMaker.settings.getStrList("runCMDConsoleAfterJoin")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replaceAll("%player%", p.getName()));
        }
    }

    public static Pair<Location, ServerPlayer> createSimple(Location loc, String name) {
        if (name == null || name.isBlank()) {
            name = getRandomName(FakePlayerMaker.randomNameLength);
        }

        if (!Strings.isNullOrEmpty(FakePlayerMaker.settings.getString("namePrefix"))) {
            name = FakePlayerMaker.settings.getString("namePrefix") + name;
        }

        GameProfile profile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name);
        Location realLoc = loc != null ? loc : FakePlayerMaker.settings.getLocation("defaultSpawnLocation");

        if (realLoc == null) {
            return ImmutablePair.right(Implementations.get().create(MinecraftServer.getServer().overworld(), profile));
        }

        if (FakePlayerMaker.isProtocolLibLoaded()) {
            try {
                Player temp = FPMTempPlayerFactory.createPlayer(Bukkit.getServer(), name);
                MinimalInjector injector = TemporaryPlayerFactory.getInjectorFromPlayer(temp);

                return new ImmutablePair<>(realLoc, (ServerPlayer) getHandle(MinecraftUtils.getCraftClass("entity.CraftPlayer"), injector.getPlayer()));
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            ServerLevel level = (ServerLevel) Objects.requireNonNull(getHandle(MinecraftUtils.getCraftClass("CraftWorld"), realLoc.getWorld()));

            return new ImmutablePair<>(realLoc, Implementations.get().create(level, profile));
        }
    }

    @Nullable
    public static ServerPlayer spawnFakePlayer(Location loc, String name) {
        return spawnFakePlayer(loc, name, true);
    }

    @Nullable
    public static ServerPlayer spawnFakePlayer(Location loc, String name, boolean runCMD) {
        Pair<Location, ServerPlayer> player = createSimple(loc, name);

        var connection = new EmptyConnection();

        fakePlayerMap.put(name, player.getValue());
        saver.syncPlayerInfo(player.getValue());

        FakePlayerMaker.guiHandler.setData(fakePlayerMap.values().stream().toList());

        playerJoin(player.getValue(), connection, MinecraftUtils.getGamePacketListener(connection, player.getValue()), runCMD, player.getKey());
        return player.getValue();
    }

    private static void playerJoin(ServerPlayer player, EmptyConnection connection, ServerGamePacketListenerImpl listener, boolean runCMD, @Nullable Location realLoc) {
        if (realLoc == null) {
            realLoc = FakePlayerMaker.settings.getLocation("defaultSpawnLocation");
        }

        ServerLevel level = (ServerLevel) Objects.requireNonNull(getHandle(MinecraftUtils.getCraftClass("CraftWorld"), realLoc.getWorld()));
        player.setLevel(level);

        Implementations.get().placePlayer(connection, player);

        MinecraftUtils.preventListen("su.nexmedia.engine.NexPlugin");

        simulateLogin(player);

        player.teleportTo(level, realLoc.getX(), realLoc.getY(), realLoc.getZ(), realLoc.getYaw(), realLoc.getPitch());

        ActionImpl.setupValues(FakePlayerMaker.settings, player);

        if (runCMD) {
            runCMDs(player, connection, listener);
        }
    }

    public static void joinFakePlayer(String name){
        ServerPlayer player = fakePlayerMap.get(name);
        if (player != null) {
            var connection = new EmptyConnection();
            var listener = MinecraftUtils.getGamePacketListener(connection, player);

            playerJoin(player, connection, listener, true, Implementations.bukkitEntity(player).getLocation());
        }
    }

    public static void removeFakePlayer(String name) {
        ServerPlayer player = fakePlayerMap.get(name);
        if (player != null) {

            for (String cmd : FakePlayerMaker.settings.getStrList("runCMDAfterRemove")) {
                cmd = cmd.replaceAll("%player%", name);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }

            fakePlayerMap.remove(name);
            saver.removeFakePlayer(name);

            Player bukkit = Implementations.bukkitEntity(player);
            bukkit.kick(Component.translatable("multiplayer.player.left"));

            FakePlayerMaker.guiHandler.setData(fakePlayerMap.values().stream().toList());
        }
    }

    public static void removeAllFakePlayers() {
        Set<String> set = new HashSet<>(fakePlayerMap.keySet());
        for (String name : set) {
            ServerPlayer player = fakePlayerMap.get(name);
            if (player != null) {

                for (String cmd : FakePlayerMaker.settings.getStrList("runCMDAfterRemove")) {
                    cmd = cmd.replaceAll("%player%", name);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }

                Player bukkit = Implementations.bukkitEntity(player);
                bukkit.kick(Component.translatable("multiplayer.player.left"));

                FakePlayerMaker.guiHandler.setData(fakePlayerMap.values().stream().toList());
            }
        }

        fakePlayerMap.clear();
        saver.removeAllFakePlayers();
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

    public static void simulateLogin(ServerPlayer p) {
        InetAddress fakeNetAddress = InetAddress.getLoopbackAddress();

        MinecraftUtils.scheduleNoDelay(FakePlayerMaker.INSTANCE, () ->
                new AsyncPlayerPreLoginEvent(
                        Implementations.getName(p),
                        fakeNetAddress,
                        fakeNetAddress,
                        Implementations.getUUID(p),
                        new CraftPlayerProfile(Implementations.get().profile(p)),
                        fakeNetAddress.getHostName()
                ).callEvent(), true);

        new PlayerLoginEvent(
                Implementations.bukkitEntity(p),
                fakeNetAddress.getHostName(),
                fakeNetAddress
        ).callEvent();
    }
}
