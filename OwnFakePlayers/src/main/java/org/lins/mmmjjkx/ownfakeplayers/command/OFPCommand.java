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
    protected void sendMessage(CommandSender sender, String message, Object... args) {
        OwnFakePlayers.messageHandler.sendMessage(sender, message, args);
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

    @Override
    public boolean execute(@NotNull CommandSender commandSender, @NotNull String s, @NotNull String[] strings) {
        if (strings.length == 1) {
            if (strings[0].equals("create")) {
                Player player = toPlayer(commandSender);
                if (player == null) {
                    return false;
                }

            }
        }
        return false;
    }
}
