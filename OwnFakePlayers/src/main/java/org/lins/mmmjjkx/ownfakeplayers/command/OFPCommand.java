package org.lins.mmmjjkx.ownfakeplayers.command;

import io.github.linsminecraftstudio.polymer.command.PolymerCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.lins.mmmjjkx.ownfakeplayers.OwnFakePlayers;

import java.util.ArrayList;
import java.util.List;

public class OFPCommand extends PolymerCommand {
    public OFPCommand() {
        super("ownfakeplayers", List.of("ofp"));
    }

    @Override
    public String requirePlugin() {
        return "FakePlayerMaker";
    }

    @Override
    public void execute(CommandSender sender, String alias) {
        if (argSize() == 1) {
            if (getArg(0).equals("create")) {
                Player player = toPlayer();
                if (player == null) {
                    return;
                }
            }
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length == 1) {
            return copyPartialMatches(args[0], List.of("create","list","remove","reload","lookat","tp",
                    "tphere","skin","mount","unmount","inventory","sneak","runcmd"));
        } else if (args.length == 2) {
            return switch (args[0]) {
                case "remove","lookat","tp","tphere","skin","mount","unmount","inventory","sneak","runcmd" -> {
                    if (sender instanceof Player p) {
                        yield OwnFakePlayers.data.getOwnFakePlayerNames(p.getUniqueId());
                    }
                    yield new ArrayList<>();
                }
                default -> new ArrayList<>();
            };
        }
        return new ArrayList<>();
    }
}
