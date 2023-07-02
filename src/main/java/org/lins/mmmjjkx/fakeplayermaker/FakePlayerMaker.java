package org.lins.mmmjjkx.fakeplayermaker;

import io.github.linsminecraftstudio.polymer.command.PolymerCommand;
import io.github.linsminecraftstudio.polymer.objects.plugin.PolymerMessageHandler;
import io.github.linsminecraftstudio.polymer.objects.plugin.PolymerPlugin;
import org.bukkit.Bukkit;
import org.lins.mmmjjkx.fakeplayermaker.command.FPMCommand;
import org.lins.mmmjjkx.fakeplayermaker.listeners.PlayerListener;
import org.lins.mmmjjkx.fakeplayermaker.objects.SpecialFeatures;
import org.lins.mmmjjkx.fakeplayermaker.utils.FakePlayerSaver;

import java.util.List;

public final class FakePlayerMaker extends PolymerPlugin {
    public static PolymerMessageHandler messageHandler;
    public static SpecialFeatures specialFeatures;
    public static FakePlayerSaver fakePlayerSaver;
    public static int randomNameLength;
    public static boolean countFakePlayersOnMOTD;
    public static FakePlayerMaker INSTANCE;

    @Override
    public void onPluginEnable() {
        // Plugin startup logic
        INSTANCE = this;
        completeDefaultConfig();
        completeLangFile("en-us","zh-cn");
        specialFeatures = new SpecialFeatures(
                getConfig().getBoolean("specialFeatures.firePlayerJoinEvent"),
                getConfig().getBoolean("specialFeatures.firePlayerQuitEvent")
        );
        fakePlayerSaver = new FakePlayerSaver();
        randomNameLength = getConfig().getInt("randomNameLength");
        countFakePlayersOnMOTD = getConfig().getBoolean("countFakePlayersOnMOTD");
        messageHandler = new PolymerMessageHandler(this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(), this);
    }

    @Override
    public void onPluginDisable() {
        // Plugin shutdown logic
    }

    @Override
    public List<PolymerCommand> registerCommands() {
        return List.of(new FPMCommand("fakeplayermaker", getConfig().getStringList("commandAliases")));
    }

    @Override
    public String requireVersion() {
        return "1.3.1";
    }
}
