package org.lins.mmmjjkx.fakeplayermaker;

import io.github.linsminecraftstudio.fakeplayermaker.api.implementation.ActionImpl;
import io.github.linsminecraftstudio.fakeplayermaker.api.implementation.Implementations;
import io.github.linsminecraftstudio.polymer.command.PolymerCommand;
import io.github.linsminecraftstudio.polymer.objects.plugin.PolymerPlugin;
import io.github.linsminecraftstudio.polymer.objects.plugin.SimpleSettingsManager;
import io.github.linsminecraftstudio.polymer.utils.Metrics;
import io.github.linsminecraftstudio.polymer.utils.OtherUtils;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.lins.mmmjjkx.fakeplayermaker.command.FPMCommand;
import org.lins.mmmjjkx.fakeplayermaker.gui.ListFakePlayerGUIHandler;
import org.lins.mmmjjkx.fakeplayermaker.impl.V1202ActionImpl;
import org.lins.mmmjjkx.fakeplayermaker.impl.V1202Impl;
import org.lins.mmmjjkx.fakeplayermaker.impl_1182.V1182ActionImpl;
import org.lins.mmmjjkx.fakeplayermaker.impl_1182.V1182Impl;
import org.lins.mmmjjkx.fakeplayermaker.impl_1194.V1194ActionImpl;
import org.lins.mmmjjkx.fakeplayermaker.impl_1194.V1194Impl;
import org.lins.mmmjjkx.fakeplayermaker.impl_1201.V1201ActionImpl;
import org.lins.mmmjjkx.fakeplayermaker.impl_1201.V1201Impl;
import org.lins.mmmjjkx.fakeplayermaker.stress.StressTestSaver;
import org.lins.mmmjjkx.fakeplayermaker.utils.FakePlayerSaver;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;

import java.util.List;

import static io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils.getCraftClass;
import static io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils.getHandle;

public class FakePlayerMaker extends PolymerPlugin implements Listener {
    public static FakePlayerSaver fakePlayerSaver;
    public static FakePlayerMaker INSTANCE;
    public static SimpleSettingsManager settings;
    public static ListFakePlayerGUIHandler guiHandler;
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
                """.formatted(getDescription().getVersion()));
        suggestSpark();
        INSTANCE = this;

        handleImplementations();

        settings = new SimpleSettingsManager(this);
        fakePlayerSaver = new FakePlayerSaver();
        stressTestSaver = new StressTestSaver();

        new Metrics(this, 19345);
        randomNameLength = settings.getInt("randomNameLength");
        defaultSpawnLocation = settings.getLocation("defaultSpawnLocation");

        fakePlayerSaver.reload(false);
        stressTestSaver.reload();

        getServer().getPluginManager().registerEvents(this, this);

        guiHandler = new ListFakePlayerGUIHandler(fakePlayerSaver.getFakePlayers().keySet().stream().toList());

        if (settings.getBoolean("checkUpdate")) {
            new OtherUtils.Updater(111767, (ver, success) -> {
                if (success) {
                    if (ver.equals(getDescription().getVersion())) {
                        getLogger().info("You are using the latest version!");
                    } else {
                        getLogger().warning("There is a new version available! New version: " + ver + " Old version: " + getPluginMeta().getVersion());
                    }
                } else {
                    getLogger().warning("Failed to check for updates!");
                }
            });
        }
    }

    private void handleImplementations() {
        Implementations.register(new V1202Impl());
        ActionImpl.register(new V1202ActionImpl());

        Implementations.register(new V1201Impl());
        ActionImpl.register(new V1201ActionImpl());

        Implementations.register(new V1194Impl());
        ActionImpl.register(new V1194ActionImpl());

        Implementations.register(new V1182Impl());
        ActionImpl.register(new V1182ActionImpl());
    }

    @Override
    public void onPlDisable() {
        stressTestSaver.stopAll();
    }

    @Override
    public List<PolymerCommand> registerCommands() {
        return List.of(new FPMCommand());
    }

    @Override
    public String requireVersion() {
        return "1.4.1";
    }

    @Override
    public int requireApiVersion() {
        return 2;
    }

    public static boolean isProtocolLibLoaded() {
        return Bukkit.getPluginManager().isPluginEnabled("ProtocolLib");
    }

    public void reload() {
        INSTANCE.reloadConfig();
        settings = new SimpleSettingsManager(FakePlayerMaker.INSTANCE);
        fakePlayerSaver.reload(false);
        stressTestSaver.reload();

        randomNameLength = settings.getInt("randomNameLength");
        defaultSpawnLocation = settings.getLocation("defaultSpawnLocation");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        SimpleSettingsManager settings = FakePlayerMaker.settings;
        ServerPlayer player = (ServerPlayer) getHandle(getCraftClass("entity.CraftPlayer"), e.getEntity().getPlayer());
        if (player != null && NMSFakePlayerMaker.fakePlayerMap.containsKey(Implementations.getName(player))) {
            Location loc = e.getPlayer().getLocation();
            e.getPlayer().spigot().respawn();
            ActionImpl.setupValues(settings, player);
            if (settings.getBoolean("player.respawnBack")) {
                e.getPlayer().teleport(loc);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        ServerPlayer nms = (ServerPlayer) getHandle(getCraftClass("entity.CraftPlayer"), e.getPlayer());
        if (nms != null && NMSFakePlayerMaker.fakePlayerMap.containsKey(Implementations.getName(nms))) {
            if (player.isDead()) {
                player.spigot().respawn();
            }
        }
    }
}
