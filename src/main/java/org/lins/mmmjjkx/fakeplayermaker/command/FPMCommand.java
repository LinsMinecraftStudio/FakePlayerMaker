package org.lins.mmmjjkx.fakeplayermaker.command;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.github.linsminecraftstudio.polymer.Polymer;
import io.github.linsminecraftstudio.polymer.command.PolymerCommand;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.utils.ObjectConverter;

import java.util.ArrayList;
import java.util.List;

public class FPMCommand extends PolymerCommand {
    public FPMCommand(@NotNull String name, List<String> aliases) {
        super(name, aliases);
    }

    @Override
    public String requirePlugin() {
        return null;
    }

    @Override
    public void sendMessage(CommandSender sender, String message, Object... args) {
        FakePlayerMaker.messageHandler.sendMessage(sender, message, args);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length == 1) {
            return copyPartialMatches(args[0], List.of("add","reload","removeAll","remove"));
        } else if (args.length == 2) {
            return switch (args[0]) {
                case "remove","teleport","tp" -> copyPartialMatches(args[1], NMSFakePlayerMaker.fakePlayerMap.keySet());
                default -> new ArrayList<>();
            };
        }
        return new ArrayList<>();
    }

    @Override
    public boolean execute(@NotNull CommandSender commandSender, @NotNull String s, @NotNull String[] strings) {
        if (hasPermission(commandSender)) {
            if (strings.length == 1) {
                return switch (strings[0]) {
                    case "add" -> {
                        Player p = toPlayer(commandSender);
                        if (p == null) {
                            sendMessage(commandSender, "SpecifyLocation");
                            yield false;
                        }
                        NMSFakePlayerMaker.spawnFakePlayer(p.getLocation(), null);
                        sendMessage(commandSender, "CreateSuccess");
                        yield true;
                    }
                    case "reload" -> {
                        FakePlayerMaker.INSTANCE.reloadConfig();
                        FakePlayerMaker.fakePlayerSaver.reload();
                        sendMessage(commandSender, "ReloadSuccess");
                        yield true;
                    }
                    case "removeAll" -> {
                        NMSFakePlayerMaker.removeAllFakePlayers();
                        sendMessage(commandSender, "RemoveAllSuccess");
                        yield true;
                    }
                    default -> {
                        Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                        yield false;
                    }
                };
            } else if (strings.length == 2) {
                String name = strings[1];
                return switch (strings[0]) {
                    case "add" -> {
                        Player p = toPlayer(commandSender);
                        if (p == null) {
                            Location location = ObjectConverter.toLocation(name);
                            if (location != null) {
                                NMSFakePlayerMaker.spawnFakePlayer(location, name);
                                sendMessage(commandSender, "CreateSuccess");
                                yield true;
                            }
                            sendMessage(commandSender, "SpecifyLocation");
                            yield false;
                        }
                        NMSFakePlayerMaker.spawnFakePlayer(p.getLocation(), name);
                        sendMessage(commandSender, "CreateSuccess");
                        yield true;
                    }
                    case "remove" -> {
                        if (NMSFakePlayerMaker.fakePlayerMap.containsKey(name)) {
                            NMSFakePlayerMaker.removeFakePlayer(name);
                            sendMessage(commandSender, "RemoveSuccess");
                            yield true;
                        } else {
                            sendMessage(commandSender, "Command.PlayerNotFound");
                            yield false;
                        }
                    }
                    case "teleport","tp" -> {
                        if (NMSFakePlayerMaker.fakePlayerMap.containsKey(name)) {
                            Player player = toPlayer(commandSender);
                            ServerPlayer serverPlayer = NMSFakePlayerMaker.fakePlayerMap.get(name);
                            if (player == null) {
                                yield false;
                            }
                            player.teleport(serverPlayer.getBukkitEntity().getLocation());
                            FakePlayerMaker.fakePlayerSaver.syncPlayerInfo(serverPlayer);
                            sendMessage(commandSender, "TeleportSuccess");
                            yield true;
                        } else {
                            sendMessage(commandSender, "Command.PlayerNotFound");
                            yield false;
                        }
                    }
                    default -> {
                        Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                        yield false;
                    }
                };
            } else if (strings.length == 3) {
                return switch (strings[0]) {
                    case "skin" -> {
                        String name = strings[1];
                        ServerPlayer player = NMSFakePlayerMaker.fakePlayerMap.get(name);
                        if (player != null) {
                            GameProfile profile = player.getGameProfile();
                            profile.getProperties().removeAll("textures");
                            profile.getProperties().put("textures", new Property("textures", strings[2]));
                            player.gameProfile = profile;
                            yield true;
                        }
                        sendMessage(commandSender,  "PlayerNotFound");
                        yield false;
                    }
                    default -> {
                        Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                        yield false;
                    }
                };
            }
        }
        return false;
    }
}
