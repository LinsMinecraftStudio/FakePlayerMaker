package org.lins.mmmjjkx.ownfakeplayers.command.subs;

import io.github.linsminecraftstudio.polymer.command.SubCommand;
import io.github.linsminecraftstudio.polymer.objects.MapBuilder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class SubCreate extends SubCommand {
    public SubCreate() {
        super("create");
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public Map<Integer, List<String>> tabCompletion(CommandSender sender) {
        return MapBuilder.empty();
    }

    @Override
    public void execute(CommandSender sender, String alias) {
        Player player = toPlayer();
        if (player == null) {
            return;
        }

    }
}
