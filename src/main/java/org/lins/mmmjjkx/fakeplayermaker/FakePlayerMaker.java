package org.lins.mmmjjkx.fakeplayermaker;

import io.github.linsminecraftstudio.polymer.command.PolymerCommand;
import io.github.linsminecraftstudio.polymer.objects.plugin.PolymerPlugin;
import io.github.linsminecraftstudio.polymer.objects.plugin.SimpleSettingsManager;
import io.github.linsminecraftstudio.polymer.objects.plugin.message.PolymerMessageHandler;
import org.bukkit.Location;
import org.lins.mmmjjkx.fakeplayermaker.command.FPMCommand;
import org.lins.mmmjjkx.fakeplayermaker.stress.StressTestSaver;
import org.lins.mmmjjkx.fakeplayermaker.utils.FakePlayerSaver;

import java.util.List;

public final class FakePlayerMaker extends PolymerPlugin{
    public static PolymerMessageHandler messageHandler;
    public static FakePlayerSaver fakePlayerSaver;
    public static FakePlayerMaker INSTANCE;
    public static SimpleSettingsManager settings;
    public static int randomNameLength;
    public static Location defaultLocation;
    public static StressTestSaver stressTestSaver;

    @Override
    public void onPlEnable() {
        // Plugin startup logic
        INSTANCE = this;
        completeLangFile("en-us","zh-cn");
        fakePlayerSaver = new FakePlayerSaver();
        stressTestSaver = new StressTestSaver();
        settings = new SimpleSettingsManager(getConfig());
        randomNameLength = settings.getInt("randomNameLength");
        defaultLocation = settings.getLocation("defaultSpawnLocation");

        fakePlayerSaver.reload();
        stressTestSaver.reload();
    }

    @Override
    public void onPlDisable() {
        stressTestSaver.stopAll();
    }

    @Override
    public List<PolymerCommand> registerCommands() {
        return List.of(new FPMCommand("fakeplayermaker", getConfig().getStringList("commandAliases")));
    }

    @Override
    public String requireVersion() {
        return "1.3.3";
    }
}
