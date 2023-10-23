package org.lins.mmmjjkx.ownfakeplayers;

import io.github.linsminecraftstudio.polymer.command.PolymerCommand;
import io.github.linsminecraftstudio.polymer.objects.plugin.PolymerPlugin;
import io.github.linsminecraftstudio.polymer.objects.plugin.SimpleSettingsManager;
import io.github.linsminecraftstudio.polymer.objects.plugin.message.PolymerMessageHandler;
import net.minecraft.server.level.ServerPlayer;
import org.lins.mmmjjkx.ownfakeplayers.command.OFPCommand;
import org.lins.mmmjjkx.ownfakeplayers.utils.DataUtil;

import java.io.File;
import java.util.List;

public final class OwnFakePlayers extends PolymerPlugin {
    public static OwnFakePlayers INSTANCE;
    public static PolymerMessageHandler messageHandler;
    public static SimpleSettingsManager settings;
    public static DataUtil data;

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
        data = new DataUtil(new File(getDataFolder(), "data"));
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

    @Override
    public int requireApiVersion() {
        return 2;
    }

    public static void setupValues(ServerPlayer player) {
        player.setInvulnerable(settings.getBoolean("player.invulnerable"));
        player.bukkitPickUpLoot = settings.getBoolean("player.canPickupItems");
        player.collides = settings.getBoolean("player.collision");
    }
}
