package org.lins.mmmjjkx.fakeplayermaker;

import io.github.linsminecraftstudio.polymer.command.PolymerCommand;
import io.github.linsminecraftstudio.polymer.objects.plugin.PolymerPlugin;
import io.github.linsminecraftstudio.polymer.objects.plugin.SimpleSettingsManager;
import io.github.linsminecraftstudio.polymer.objects.plugin.message.PolymerMessageHandler;
import io.github.linsminecraftstudio.polymer.utils.Metrics;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.lins.mmmjjkx.fakeplayermaker.command.FPMCommand;
import org.lins.mmmjjkx.fakeplayermaker.implementation.Implementations;
import org.lins.mmmjjkx.fakeplayermaker.listeners.FPMListener;
import org.lins.mmmjjkx.fakeplayermaker.stress.StressTestSaver;
import org.lins.mmmjjkx.fakeplayermaker.utils.FakePlayerSaver;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getCraftClass;

public class FakePlayerMaker extends PolymerPlugin{
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
        Implementations.setup();
        settings = new SimpleSettingsManager(this);
        completeLangFile("en-us","zh-cn");
        messageHandler = new PolymerMessageHandler(this);
        fakePlayerSaver = new FakePlayerSaver();
        stressTestSaver = new StressTestSaver();
        new Metrics(this, 19435);
        randomNameLength = settings.getInt("randomNameLength");
        defaultSpawnLocation = settings.getLocation("defaultSpawnLocation");

        fakePlayerSaver.reload();
        stressTestSaver.reload();

        getServer().getPluginManager().registerEvents(new FPMListener(), this);
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
        try {
            return (MinecraftServer) getCraftClass("CraftServer").getMethod("getServer").invoke(Bukkit.getServer());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
