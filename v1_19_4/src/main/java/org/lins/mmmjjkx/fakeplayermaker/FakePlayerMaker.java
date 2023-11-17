package org.lins.mmmjjkx.fakeplayermaker;

import io.github.linsminecraftstudio.fakeplayermaker.api.events.FPMPluginLoadEvent;
import io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils;
import io.github.linsminecraftstudio.polymer.command.PolymerCommand;
import io.github.linsminecraftstudio.polymer.objects.plugin.PolymerPlugin;
import io.github.linsminecraftstudio.polymer.objects.plugin.SimpleSettingsManager;
import io.github.linsminecraftstudio.polymer.utils.OtherUtils;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.lins.mmmjjkx.fakeplayermaker.command.FPMCommand;
import org.lins.mmmjjkx.fakeplayermaker.gui.ListFakePlayerGUIHandler;
import org.lins.mmmjjkx.fakeplayermaker.stress.StressTestSaver;
import org.lins.mmmjjkx.fakeplayermaker.utils.ActionUtils;
import org.lins.mmmjjkx.fakeplayermaker.utils.FakePlayerSaver;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;

import java.util.List;

import static io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils.getCraftClass;
import static io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils.getHandle;

public class FakePlayerMaker extends PolymerPlugin implements Listener {
    public static FakePlayerSaver fakePlayerSaver;
    public static FakePlayerMaker INSTANCE;
    public static SimpleSettingsManager settings;
    public static volatile int randomNameLength;
    public static volatile Location defaultSpawnLocation;
    public static StressTestSaver stressTestSaver;
    public static ListFakePlayerGUIHandler guiHandler;

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
                                                                 
                 version %s by mmmjjkx(special version on 1.19.4)
                """.formatted(getPluginMeta().getVersion()));
        suggestSpark();
        INSTANCE = this;
        settings = new SimpleSettingsManager(this);
        fakePlayerSaver = new FakePlayerSaver();
        stressTestSaver = new StressTestSaver();

        startMetrics(19435);

        randomNameLength = settings.getInt("randomNameLength");
        defaultSpawnLocation = settings.getLocation("defaultSpawnLocation");

        fakePlayerSaver.reload(false);
        stressTestSaver.reload();

        MinecraftUtils.scheduleNoDelay(this, () -> new FPMPluginLoadEvent(NMSFakePlayerMaker.asController()).callEvent(), true);

        getServer().getPluginManager().registerEvents(this, this);

        guiHandler = new ListFakePlayerGUIHandler(fakePlayerSaver.getFakePlayers().keySet().stream().toList());

        if (settings.getBoolean("checkUpdate")) {
            new OtherUtils.Updater(111767, (ver, success) -> {
                if (success) {
                    if (ver.equals(getPluginMeta().getVersion())) {
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
        return "1.4";
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
