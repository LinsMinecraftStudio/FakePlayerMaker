package org.lins.mmmjjkx.fakeplayermaker;

import io.github.linsminecraftstudio.fakeplayermaker.api.events.FPMPluginLoadEvent;
import io.github.linsminecraftstudio.polymer.command.PolymerCommand;
import io.github.linsminecraftstudio.polymer.objects.plugin.PolymerPlugin;
import io.github.linsminecraftstudio.polymer.objects.plugin.SimpleSettingsManager;
import io.github.linsminecraftstudio.polymer.objects.plugin.message.PolymerMessageHandler;
import io.github.linsminecraftstudio.polymer.utils.Metrics;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.lins.mmmjjkx.fakeplayermaker.command.FPMCommand;
import org.lins.mmmjjkx.fakeplayermaker.stress.StressTestSaver;
import org.lins.mmmjjkx.fakeplayermaker.utils.ActionUtils;
import org.lins.mmmjjkx.fakeplayermaker.utils.FakePlayerSaver;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;

import java.util.List;

import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getCraftClass;
import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getHandle;

public class FakePlayerMaker extends PolymerPlugin implements Listener {
    public static PolymerMessageHandler messageHandler;
    public static FakePlayerSaver fakePlayerSaver;
    public static FakePlayerMaker INSTANCE;
    public static SimpleSettingsManager settings;
    public static volatile int randomNameLength;
    public static volatile Location defaultSpawnLocation;
    public static StressTestSaver stressTestSaver;

    @Override
    public void onPlEnable() {
        // Plugin startup logic
        getLogger().info("""
                 \n
                  _____     _        ____  _                       __  __       _            \s
                 |  ___|_ _| | _____|  _ \\| | __ _ _   _  ___ _ __|  \\/  | __ _| | _____ _ __\s
                 | |_ / _` | |/ / _ \\ |_) | |/ _` | | | |/ _ \\ '__| |\\/| |/ _` | |/ / _ \\ '__|
                 |  _| (_| |   <  __/  __/| | (_| | |_| |  __/ |  | |  | | (_| |   <  __/ |  \s
                 |_|  \\__,_|_|\\_\\___|_|   |_|\\__,_|\\__, |\\___|_|  |_|  |_|\\__,_|_|\\_\\___|_|  \s
                                                   |___/                                     \s
                                                                 
                 version %s by mmmjjkx
                """.formatted(getPluginMeta().getVersion()));
        suggestSpark();
        INSTANCE = this;
        settings = new SimpleSettingsManager(this);
        completeLangFile("en-us", "zh-cn");
        messageHandler = new PolymerMessageHandler(this);
        fakePlayerSaver = new FakePlayerSaver();
        stressTestSaver = new StressTestSaver();

        new Metrics(this, 19435);
        randomNameLength = settings.getInt("randomNameLength");
        defaultSpawnLocation = settings.getLocation("defaultSpawnLocation");

        fakePlayerSaver.reload();
        stressTestSaver.reload();

        new BukkitRunnable() {
            @Override
            public void run() {
                new FPMPluginLoadEvent(NMSFakePlayerMaker.asController()).callEvent();
            }
        }.runTaskAsynchronously(this);

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onPlDisable() {
        stressTestSaver.stopAll();
    }

    @Override
    public List<PolymerCommand> registerCommands() {
        return List.of(new FPMCommand("fakeplayermaker"));
    }

    @Override
    public String requireVersion() {
        return "1.3.5";
    }

    public static void unregisterHandlers(Class<? extends JavaPlugin> clazz) {
        JavaPlugin plugin = JavaPlugin.getPlugin(clazz);
        HandlerList.unregisterAll(plugin);
    }

    public static boolean isProtocolLibLoaded() {
        return Bukkit.getPluginManager().isPluginEnabled("ProtocolLib");
    }

    public static void reload() {
        INSTANCE.reloadConfig();
        settings = new SimpleSettingsManager(FakePlayerMaker.INSTANCE);
        fakePlayerSaver.reload();
        stressTestSaver.reload();

        randomNameLength = settings.getInt("randomNameLength");
        defaultSpawnLocation = settings.getLocation("defaultSpawnLocation");
    }

    public static MinecraftServer getNMSServer() {
        return MinecraftServer.getServer();
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        SimpleSettingsManager settings = FakePlayerMaker.settings;
        ServerPlayer player = (ServerPlayer) getHandle(getCraftClass("entity.CraftPlayer"), e.getEntity().getPlayer());
        if (player != null && NMSFakePlayerMaker.fakePlayerMap.containsKey(player.getName().getString())) {
            Location loc = e.getPlayer().getLocation();
            e.getPlayer().spigot().respawn();
            ActionUtils.setupValues(player);
            if (settings.getBoolean("player.respawnBack")) {
                e.getPlayer().teleport(loc);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        ServerPlayer player = (ServerPlayer) getHandle(getCraftClass("entity.CraftPlayer"), e.getPlayer());
        if (player != null && NMSFakePlayerMaker.fakePlayerMap.containsKey(player.getName().getString())) {
            if (e.getPlayer().isDead()) {
                e.getPlayer().spigot().respawn();
            }
        }
    }
}