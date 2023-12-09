package org.lins.mmmjjkx.fakeplayermaker.utils;

import com.comphenix.protocol.injector.temporary.MinimalInjector;
import com.comphenix.protocol.injector.temporary.TemporaryPlayerFactory;
import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.google.common.base.Strings;
import com.mojang.authlib.GameProfile;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.FakePlayerCreateEvent;
import io.github.linsminecraftstudio.fakeplayermaker.api.events.FakePlayerRemoveEvent;
import io.github.linsminecraftstudio.fakeplayermaker.api.interfaces.FakePlayerController;
import io.github.linsminecraftstudio.fakeplayermaker.api.objects.EmptyConnection;
import io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
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

import static io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils.getCraftClass;
import static io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils.getHandle;

public class NMSFakePlayerMaker {
    public static Map<String, ServerPlayer> fakePlayerMap = new HashMap<>();
    private static final FakePlayerSaver saver = FakePlayerMaker.fakePlayerSaver;
    private static final MinecraftServer server = MinecraftServer.getServer();

    static void reloadMap(boolean removeAll, Map<ServerPlayer, Location> players) {
        MinecraftUtils.schedule(FakePlayerMaker.INSTANCE, () -> {
            if (removeAll) {
                for (ServerPlayer player : players.keySet()) {
                    if (MinecraftServer.getServer().getPlayerList().getPlayer(player.getUUID()) != null) {
                        MinecraftServer.getServer().getPlayerList().remove(player);
                    }
                }
                fakePlayerMap.clear();
                return;
            }

            fakePlayerMap.clear();
            for (ServerPlayer player : players.keySet()) {
                if (server.getPlayerList().getPlayer(player.getUUID()) == null) {
                    EmptyConnection connection = new EmptyConnection();
                    playerJoin(player, connection, MinecraftUtils.getGamePacketListener(connection, player), true, null);
                }

                fakePlayerMap.put(player.getName().getString(), player);

                ActionUtils.setupValues(player);

                MinecraftUtils.preventListen("su.nexmedia.engine.NexPlugin");

                Location location = players.get(player);
                ServerLevel level = (ServerLevel) getHandle(getCraftClass("CraftWorld"), location.getWorld());
                player.teleportTo(Objects.requireNonNull(level), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            }
        }, 5, false);
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

    public static Pair<Location, ServerPlayer> createSimple(Location loc, String name) {
        if (name == null || name.isBlank()) {
            name = getRandomName(FakePlayerMaker.randomNameLength);
        }

        if (!Strings.isNullOrEmpty(FakePlayerMaker.settings.getString("namePrefix"))) {
            name = FakePlayerMaker.settings.getString("namePrefix") + name;
        }

        Location realLoc = loc != null ? loc : FakePlayerMaker.settings.getLocation("defaultSpawnLocation");
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);

        if (realLoc == null) {
            return ImmutablePair.right(new ServerPlayer(MinecraftServer.getServer(), MinecraftServer.getServer().overworld(), profile));
        }

        if (FakePlayerMaker.isProtocolLibLoaded()) {
            try {
                Player temp = FPMTempPlayerFactory.createPlayer(Bukkit.getServer(), name);
                MinimalInjector injector = TemporaryPlayerFactory.getInjectorFromPlayer(temp);

                return new ImmutablePair<>(realLoc, (ServerPlayer) getHandle(getCraftClass("entity.CraftPlayer"), injector.getPlayer()));
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            ServerLevel level = (ServerLevel) Objects.requireNonNull(getHandle(getCraftClass("CraftWorld"), realLoc.getWorld()));

            return new ImmutablePair<>(realLoc, new ServerPlayer(MinecraftServer.getServer(), level, profile));
        }
    }

    @Nullable
    public static ServerPlayer spawnFakePlayer(Location loc, String name, @Nullable CommandSender sender) {
        return spawnFakePlayer(loc, name, sender, true);
    }

    @Nullable
    public static ServerPlayer spawnFakePlayer(Location loc, String name, @Nullable CommandSender sender, boolean runCMD) {
        Pair<Location, ServerPlayer> player = createSimple(loc, name);

        var connection = new EmptyConnection();

        fakePlayerMap.put(name, player.getValue());
        saver.syncPlayerInfo(player.getValue());

        new FakePlayerCreateEvent(player.getValue().getBukkitEntity(), sender).callEvent();

        FakePlayerMaker.guiHandler.setData(fakePlayerMap.values().stream().toList());

        playerJoin(player.getValue(), connection, MinecraftUtils.getGamePacketListener(connection, player.getValue()), runCMD, player.getKey());
        return player.getValue();
    }

    private static void playerJoin(ServerPlayer player, EmptyConnection connection, ServerGamePacketListenerImpl listener, boolean runCMD, @Nullable Location realLoc) {
        if (realLoc == null) {
            realLoc = FakePlayerMaker.settings.getLocation("defaultSpawnLocation");
        }

        ServerLevel level = (ServerLevel) Objects.requireNonNull(getHandle(getCraftClass("CraftWorld"), realLoc.getWorld()));
        player.setLevel(level);

        server.getPlayerList().placeNewPlayer(connection, player);
        simulateLogin(player);
        MinecraftUtils.handlePlugins(player.getBukkitEntity());

        player.teleportTo(level, realLoc.getX(), realLoc.getY(), realLoc.getZ(), realLoc.getYaw(), realLoc.getPitch());

        MinecraftUtils.preventListen("su.nexmedia.engine.NexPlugin");

        ActionUtils.setupValues(player);

        if (runCMD) {
            runCMDs(player, connection, listener);
        }
    }

    public static void joinFakePlayer(String name) {
        ServerPlayer player = fakePlayerMap.get(name);
        if (player != null) {
            var connection = new EmptyConnection();
            var listener = MinecraftUtils.getGamePacketListener(connection, player);

            playerJoin(player, connection, listener, true, player.getBukkitEntity().getLocation());
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

            Player bukkit = player.getBukkitEntity();
            bukkit.kick(Component.translatable("multiplayer.player.left"));

            FakePlayerMaker.guiHandler.setData(fakePlayerMap.values().stream().toList());
        }
    }

    public static void removeAllFakePlayers(@Nullable CommandSender sender) {
        Set<String> set = new HashSet<>(fakePlayerMap.keySet());
        for (String name : set) {
            ServerPlayer player = fakePlayerMap.get(name);
            if (player != null) {
                new FakePlayerRemoveEvent(player.getName().getString(), sender).callEvent();

                for (String cmd : FakePlayerMaker.settings.getStrList("runCMDAfterRemove")) {
                    cmd = cmd.replaceAll("%player%", name);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }

                Player bukkit = player.getBukkitEntity();
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
                        p.getName().getString(),
                        fakeNetAddress,
                        fakeNetAddress,
                        p.getUUID(),
                        new CraftPlayerProfile(p.getGameProfile()),
                        fakeNetAddress.getHostName()
                ).callEvent(), true);

        new PlayerLoginEvent(
                p.getBukkitEntity(),
                fakeNetAddress.getHostName(),
                fakeNetAddress
        ).callEvent();
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
                return fakePlayerMap.get(name) != null ? fakePlayerMap.get(name).getBukkitEntity() : null;
            }

            @Override
            public void removeFakePlayer(String name) {
                NMSFakePlayerMaker.removeFakePlayer(name, null);
            }
        };
    }


}
