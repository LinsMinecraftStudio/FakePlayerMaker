package org.lins.mmmjjkx.fakeplayermaker.command;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.linsminecraftstudio.fakeplayermaker.api.interfaces.IStressTester;
import io.github.linsminecraftstudio.polymer.Polymer;
import io.github.linsminecraftstudio.polymer.command.PolymerCommand;
import io.github.linsminecraftstudio.polymer.utils.ObjectConverter;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.WorldNotFoundException;
import org.lins.mmmjjkx.fakeplayermaker.implementation.Implementations;
import org.lins.mmmjjkx.fakeplayermaker.stress.AreaStressTester;
import org.lins.mmmjjkx.fakeplayermaker.stress.RandomWorldStressTester;
import org.lins.mmmjjkx.fakeplayermaker.utils.ActionUtils;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.lins.mmmjjkx.fakeplayermaker.hook.WEHook.handleAreaCreate;

public class FPMCommand extends PolymerCommand {
    public FPMCommand(@NotNull String name) {
        super(name, new ArrayList<>(List.of("fpm")));
    }

    @Override
    public String requirePlugin() {
        return null;
    }

    @Override
    protected void sendMessage(CommandSender sender, String message, Object... args) {
        FakePlayerMaker.messageHandler.sendMessage(sender, message, args);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return copyPartialMatches(args[0], List.of(
                    "add","reload","removeAll","remove","stress","join","chat","teleport","tp","skin"));
        } else if (args.length == 2) {
            return switch (args[0]) {
                case "remove","teleport","tp","join","chat","skin"
                        -> copyPartialMatches(args[1], NMSFakePlayerMaker.fakePlayerMap.keySet());
                case "stress" -> copyPartialMatches(args[1], List.of("area","randomworld"));
                default -> new ArrayList<>();
            };
        } else if (args.length==3) {
            return switch (args[0]) {
                case "stress" -> List.of("start","stop","players","create");
                case "skin" -> Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> !NMSFakePlayerMaker.fakePlayerMap.containsKey(n)).collect(Collectors.toList());
                default -> new ArrayList<>();
            };
        } else if (args.length == 4 && args[0].equals("stress") && !args[2].equals("create")) {
            if (args[1].equalsIgnoreCase("area")) {
                return copyPartialMatches(args[3], FakePlayerMaker.stressTestSaver.getAreaTesterNames());
            } else if (args[1].equalsIgnoreCase("randomworld")){
                return copyPartialMatches(args[3], FakePlayerMaker.stressTestSaver.getRWTesterNames());
            }
        }
        return new ArrayList<>();
    }

    @Override
    public boolean execute(@NotNull CommandSender commandSender, @NotNull String s, @NotNull String[] strings) {
        if (hasCustomPermission(commandSender,"command")) {
            if (strings.length == 1) {
                return switch (strings[0]) {
                    case "add" -> {
                        if (commandSender instanceof Player p) {
                            NMSFakePlayerMaker.spawnFakePlayer(p.getLocation(), null, commandSender);
                            sendMessage(commandSender, "CreateSuccess");
                            yield true;
                        } else {
                            if (FakePlayerMaker.defaultSpawnLocation != null) {
                                NMSFakePlayerMaker.spawnFakePlayer(FakePlayerMaker.defaultSpawnLocation, null, commandSender);
                                sendMessage(commandSender, "CreateSuccess");
                                yield true;
                            } else {
                                sendMessage(commandSender, "SpecifyLocation");
                                yield false;
                            }
                        }
                    }
                    case "reload" -> {
                        FakePlayerMaker.reload();
                        sendMessage(commandSender, "ReloadSuccess");
                        yield true;
                    }
                    case "removeAll" -> {
                        NMSFakePlayerMaker.removeAllFakePlayers(commandSender);
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
                        if (!(commandSender instanceof Player p)) {
                            if (FakePlayerMaker.defaultSpawnLocation == null) {
                                sendMessage(commandSender, "SpecifyLocation");
                                yield false;
                            }
                            NMSFakePlayerMaker.spawnFakePlayer(FakePlayerMaker.defaultSpawnLocation, name, commandSender);
                            sendMessage(commandSender, "CreateSuccess");
                            yield true;
                        }
                        NMSFakePlayerMaker.spawnFakePlayer(p.getLocation(), name, commandSender);
                        sendMessage(commandSender, "CreateSuccess");
                        yield true;
                    }
                    case "remove" -> {
                        if (NMSFakePlayerMaker.fakePlayerMap.containsKey(name)) {
                            NMSFakePlayerMaker.removeFakePlayer(name, commandSender);
                            sendMessage(commandSender, "RemoveSuccess");
                            yield true;
                        } else {
                            sendMessage(commandSender, "PlayerNotFound");
                            yield false;
                        }
                    }
                    case "teleport", "tp" -> {
                        if (NMSFakePlayerMaker.fakePlayerMap.containsKey(name)) {
                            Player player = toPlayer(commandSender);
                            ServerPlayer serverPlayer = NMSFakePlayerMaker.fakePlayerMap.get(name);
                            if (player == null) {
                                sendMessage(commandSender, "PlayerNotFound");
                                yield false;
                            }
                            player.teleport(Implementations.bukkitEntity(serverPlayer));
                            FakePlayerMaker.fakePlayerSaver.syncPlayerInfo(serverPlayer);
                            sendMessage(commandSender, "TeleportSuccess");
                            yield true;
                        } else {
                            sendMessage(commandSender, "PlayerNotFound");
                            yield false;
                        }
                    }
                    case "join" -> {
                        if (NMSFakePlayerMaker.fakePlayerMap.containsKey(name)) {
                            NMSFakePlayerMaker.joinFakePlayer(name);
                            yield true;
                        }
                        sendMessage(commandSender, "PlayerNotFound");
                        yield false;
                    }
                    default -> {
                        Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                        yield false;
                    }
                };
            } else if (strings.length == 3) {
                //function commands here
                switch (strings[0]) {
                    case "skin" -> {
                        String name = strings[2];
                        ServerPlayer player = NMSFakePlayerMaker.fakePlayerMap.get(strings[1]);
                        if (player != null) {
                            return skinChange(player, commandSender, name);
                        }
                        sendMessage(commandSender, "PlayerNotFound");
                        return false;
                    }
                    case "chat" -> {
                        String name = strings[1];
                        String chat = strings[2];
                        ServerPlayer player = NMSFakePlayerMaker.fakePlayerMap.get(name);
                        if (player == null) {
                            sendMessage(commandSender, "PlayerNotFound");
                            return false;
                        }
                        ActionUtils.chat(player, chat);
                        return false;
                    }
                }
                //function commands end
                if (strings[0].equals("add")) {
                    String name = strings[1];
                    Location location = ObjectConverter.toLocation(strings[2]);
                    if (location == null) {
                        sendMessage(commandSender, "SpecifyLocation");
                        return false;
                    }
                    NMSFakePlayerMaker.spawnFakePlayer(location, name, commandSender);
                    sendMessage(commandSender, "CreateSuccess");
                    return true;
                }
                Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                return false;
            } else if (strings.length==4 && strings[0].equals("stress")) {
                return hasCustomPermission(commandSender, "command.stress") && switch (strings[1]) {
                    case "area" -> {
                        Optional<AreaStressTester> tester = FakePlayerMaker.stressTestSaver.getStressTesterArea(strings[3]);
                        switch (strings[2]) {
                            case "start" -> {
                                if (!FakePlayerMaker.settings.getBoolean("areaStressTesters")) {
                                    sendMessage(commandSender, "Stress.AreaNotEnabled");
                                    yield false;
                                }
                                if (tester.isEmpty()) {
                                    sendMessage(commandSender, "Stress.NotFound");
                                    yield false;
                                }
                                AreaStressTester stressTester = tester.get();
                                try {
                                    stressTester.run();
                                } catch (WorldNotFoundException e) {
                                    sendMessage(commandSender, "Stress.AreaWorldNotFound");
                                    yield false;
                                } catch (IllegalStateException e) {
                                    sendMessage(commandSender, "Stress.StartFast");
                                    yield false;
                                }
                                yield true;
                            }
                            case "stop" -> {
                                if (!FakePlayerMaker.settings.getBoolean("areaStressTesters")) {
                                    sendMessage(commandSender, "Stress.AreaNotEnabled");
                                    yield false;
                                }
                                if (tester.isEmpty()) {
                                    sendMessage(commandSender, "Stress.NotFound");
                                    yield false;
                                }
                                AreaStressTester stressTester = tester.get();
                                if (!stressTester.isStarted()) {
                                    sendMessage(commandSender, "Stress.NotStarted");
                                    yield false;
                                }
                                stressTester.stop();
                                yield true;
                            }
                            case "create" -> {
                                yield handleAreaCreate(commandSender, strings);
                            }
                            case "players" -> listPlayers(commandSender, tester);
                            default -> {
                                Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                                yield false;
                            }
                        }
                        yield false;
                    }
                    case "randomworld" -> {
                        Optional<RandomWorldStressTester> tester = FakePlayerMaker.stressTestSaver.getStressTesterRandomWorld(strings[3]);
                        switch (strings[2]) {
                            case "start" -> {
                                if (tester.isEmpty()) {
                                    sendMessage(commandSender, "Stress.NotFound");
                                    yield false;
                                }
                                RandomWorldStressTester stressTester = tester.get();
                                try {
                                    stressTester.run();
                                } catch (IllegalStateException e) {
                                    sendMessage(commandSender, "Stress.StartFast");
                                    yield false;
                                }
                                yield true;
                            }
                            case "stop" -> {
                                if (tester.isEmpty()) {
                                    sendMessage(commandSender, "Stress.NotFound");
                                    yield false;
                                }
                                RandomWorldStressTester stressTester = tester.get();
                                if (!stressTester.isStarted()) {
                                    sendMessage(commandSender, "Stress.NotStarted");
                                    yield false;
                                }
                                stressTester.stop();
                                yield true;
                            }
                            case "create" -> {
                                FakePlayerMaker.stressTestSaver.newRandomWorldTester(strings[3], 100);
                                sendMessage(commandSender, "Stress.TesterCreated");
                                yield true;
                            }
                            case "players" -> listPlayers(commandSender, tester);
                            default -> {
                                Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                                yield false;
                            }
                        }
                        yield false;
                    }
                    default -> {
                        Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                        yield false;
                    }
                };
            } else if (strings.length==5 && strings[0].equals("stress")){
                int amount = (int) toIntOrDouble(commandSender, strings[4], 5, true);
                return hasCustomPermission(commandSender, "command.stress") && switch (strings[1]) {
                    case "area" -> {
                        if (strings[2].equals("create")){
                            if (amount != -100) {
                                yield handleAreaCreate(commandSender, strings, amount);
                            }
                        }
                        Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                        yield false;
                    }
                    case "randomworld" -> {
                        if (strings[2].equals("create")) {
                            if (amount != -100) {
                                FakePlayerMaker.stressTestSaver.newRandomWorldTester(strings[3], amount);
                                sendMessage(commandSender, "Stress.TesterCreated");
                                yield true;
                            }
                        }
                        Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                        yield false;
                    }
                    default -> {
                        Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                        yield false;
                    }
                };
            } else {
                Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                return false;
            }
        }
        return false;
    }

    private <I extends IStressTester> void listPlayers(@NotNull CommandSender commandSender, Optional<I> tester) {
        if (tester.isEmpty()) {
            sendMessage(commandSender, "Stress.NotFound");
            return;
        }
        Set<String> names = tester.get().getTempPlayers().keySet();
        commandSender.sendMessage(names.toString());
    }

    private boolean skinChange(ServerPlayer player, CommandSender operator, String targetName) {
        Player bukkit = Implementations.bukkitEntity(player);
        PlayerProfile playerProfile = bukkit.getPlayerProfile();
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + targetName);
            InputStreamReader reader = new InputStreamReader(url.openStream());
            String uuid = JsonParser.parseReader(reader).getAsJsonObject().get("id").getAsString();
            URL url1 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            InputStreamReader reader1 = new InputStreamReader(url1.openStream());
            JsonObject properties = JsonParser.parseReader(reader1).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();

            String value = properties.get("value").getAsString();
            String signature = properties.get("signature").getAsString();

            playerProfile.setProperty(new ProfileProperty("textures", value, signature));
            bukkit.setPlayerProfile(playerProfile);

            FakePlayerMaker.fakePlayerSaver.syncPlayerInfo(player);

            sendMessage(operator, "SkinChanged");
            return true;
        } catch (IllegalStateException | IOException | NullPointerException exception) {
            sendMessage(operator, "SkinChangeFailed");
            return false;
        }
    }
}
