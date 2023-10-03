package org.lins.mmmjjkx.ownfakeplayers;

import io.github.linsminecraftstudio.polymer.command.PolymerCommand;
import io.github.linsminecraftstudio.polymer.objects.plugin.PolymerPlugin;
import io.github.linsminecraftstudio.polymer.objects.plugin.SimpleSettingsManager;
import io.github.linsminecraftstudio.polymer.objects.plugin.message.PolymerMessageHandler;
import org.lins.mmmjjkx.ownfakeplayers.command.OFPCommand;

import java.util.List;

public final class OwnFakePlayers extends PolymerPlugin {
    public static OwnFakePlayers INSTANCE;
    public static PolymerMessageHandler messageHandler;
    public static SimpleSettingsManager settings;

    @Override
    public void onPlEnable() {
        getLogger().info("""
                \n
                  ___                 _____     _        ____  _                          \s
                 / _ \\__      ___ __ |  ___|_ _| | _____|  _ \\| | __ _ _   _  ___ _ __ ___\s
                | | | \\ \\ /\\ / / '_ \\| |_ / _` | |/ / _ \\ |_) | |/ _` | | | |/ _ \\ '__/ __|
                | |_| |\\ V  V /| | | |  _| (_| |   <  __/  __/| | (_| | |_| |  __/ |  \\__ \\
                 \\___/  \\_/\\_/ |_| |_|_|  \\__,_|_|\\_\\___|_|   |_|\\__,_|\\__, |\\___|_|  |___/
                                                                       |___/              \s
                                                                       
                version %s by mmmmjkx
                """.formatted(getPluginMeta().getVersion()));
        INSTANCE = this;
        settings = new SimpleSettingsManager(this);
        messageHandler = new PolymerMessageHandler(this);
    }

    @Override
    public void onPlDisable() {

    }

    @Override
    public List<PolymerCommand> registerCommands() {
        return List.of(new OFPCommand());
    }

    @Override
    public String requireVersion() {
        return "1.3.5";
    }
}
