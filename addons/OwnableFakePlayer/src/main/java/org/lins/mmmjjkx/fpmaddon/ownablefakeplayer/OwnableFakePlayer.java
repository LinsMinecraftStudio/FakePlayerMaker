package org.lins.mmmjjkx.fpmaddon.ownablefakeplayer;

import io.github.linsminecraftstudio.polymer.command.PolymerCommand;
import io.github.linsminecraftstudio.polymer.objects.plugin.PolymerPlugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class OwnableFakePlayer extends PolymerPlugin {
    @Override
    public void onPlEnable() {
        saveDefaultConfig();

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
        return "1.3.4";
    }
}
