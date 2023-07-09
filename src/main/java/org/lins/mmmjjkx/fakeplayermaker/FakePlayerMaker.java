package org.lins.mmmjjkx.fakeplayermaker;

import io.github.linsminecraftstudio.polymer.command.PolymerCommand;
import io.github.linsminecraftstudio.polymer.objects.plugin.PolymerPlugin;
import io.github.linsminecraftstudio.polymer.objects.plugin.SimpleSettingsManager;
import io.github.linsminecraftstudio.polymer.objects.plugin.message.PolymerMessageHandler;
import org.bukkit.Location;
import org.lins.mmmjjkx.fakeplayermaker.command.FPMCommand;
import org.lins.mmmjjkx.fakeplayermaker.utils.FakePlayerSaver;

import java.util.List;

public final class FakePlayerMaker extends PolymerPlugin{
    public static PolymerMessageHandler messageHandler;
    public static FakePlayerSaver fakePlayerSaver;
    public static FakePlayerMaker INSTANCE;
    public static SimpleSettingsManager settings;
    public static int randomNameLength;
    public static Location defaultLocation;

    @Override
    public void onEnable() {
        // Plugin startup logic
        INSTANCE = this;
        completeDefaultConfig();
        completeLangFile("en-us","zh-cn");
        fakePlayerSaver = new FakePlayerSaver();
        settings = new SimpleSettingsManager(getConfig());
        randomNameLength = settings.getInt("randomNameLength");
        defaultLocation = settings.getLocation("defaultSpawnLocation");
        messageHandler = new PolymerMessageHandler(this);

        fakePlayerSaver.reload();
    }

    @Override
    public List<PolymerCommand> registerCommands() {
        return List.of(new FPMCommand("fakeplayermaker", getConfig().getStringList("commandAliases")));
    }

    @Override
    public String requireVersion() {
        return "1.3.2";
    }
}
