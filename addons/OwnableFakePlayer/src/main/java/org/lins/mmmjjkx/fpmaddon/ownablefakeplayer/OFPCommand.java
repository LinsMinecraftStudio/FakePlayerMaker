package org.lins.mmmjjkx.fpmaddon.ownablefakeplayer;

import io.github.linsminecraftstudio.polymer.command.PolymerCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OFPCommand extends PolymerCommand {
    public OFPCommand() {
        super("ownablefakeplayer", List.of("ofp","ownablefp"));
    }

    @Override
    public String requirePlugin() {
        return null;
    }

    @Override
    public boolean execute(@NotNull CommandSender commandSender, @NotNull String s, @NotNull String[] strings) {
        return false;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length == 0) {
            return List.of("create","remove","tphere","tph","tp","sneak","runcmd","reload");
        }
        return List.of();
    }
}
