package org.lins.mmmjjkx.fakeplayermaker.command;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.linsminecraftstudio.fakeplayermaker.api.interfaces.IStressTester;
import io.github.linsminecraftstudio.fakeplayermaker.api.objects.WorldNotFoundException;
import io.github.linsminecraftstudio.polymer.Polymer;
import io.github.linsminecraftstudio.polymer.command.PolymerCommand;
import io.github.linsminecraftstudio.polymer.objects.PolymerConstants;
import io.github.linsminecraftstudio.polymer.utils.ObjectConverter;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.stress.AreaStressTester;
import org.lins.mmmjjkx.fakeplayermaker.stress.RandomWorldStressTester;
import org.lins.mmmjjkx.fakeplayermaker.utils.ActionUtils;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
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

    protected void sendMessage(CommandSender sender, String message, Object... args) {
        FakePlayerMaker.messageHandler.sendMessage(sender, message, args);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return copyPartialMatches(args[0], List.of(
                    "add", "reload", "removeAll", "remove", "stress", "join", "chat", "teleport",
                    "tp", "skin", "lookat", "jump", "mount", "unmount", "pose", "inventory", "sneak", "look"));
        } else if (args.length == 2) {
            return switch (args[0]) {
                case "remove", "teleport", "tp", "join", "chat", "skin", "lookat", "jump", "mount", "unmount", "pose", "inventory", "sneak", "look" ->
                        copyPartialMatches(args[1], NMSFakePlayerMaker.fakePlayerMap.keySet());
                case "stress" -> copyPartialMatches(args[1], List.of("area", "randomworld"));
                default -> new ArrayList<>();
            };
        } else if (args.length == 3) {
            return switch (args[0]) {
                case "stress" -> copyPartialMatches(args[2], List.of("start", "stop", "players", "create"));
                case "skin" ->
                        copyPartialMatches(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> !NMSFakePlayerMaker.fakePlayerMap.containsKey(n)).collect(Collectors.toList()));
                case "look" ->
                        copyPartialMatches(args[2], Arrays.stream(Direction.values()).map(d -> d.getName().toUpperCase()).collect(Collectors.toList()));
                case "jump" -> copyPartialMatches(args[2], List.of("1", "2", "3", "4", "5", "hold", "stop"));
                case "pose" ->
                        copyPartialMatches(args[2], Arrays.stream(Pose.values()).map(Pose::toString).collect(Collectors.toList()));
                default -> new ArrayList<>();
            };
        } else if (args.length == 4 && args[0].equals("stress") && !args[2].equals("create")) {
            if (args[1].equalsIgnoreCase("area")) {
                return copyPartialMatches(args[3], FakePlayerMaker.stressTestSaver.getAreaTesterNames());
            } else if (args[1].equalsIgnoreCase("randomworld")) {
                return copyPartialMatches(args[3], FakePlayerMaker.stressTestSaver.getRWTesterNames());
            }
        }
        return new ArrayList<>();
    }

    @Override
    public void execute(@NotNull CommandSender commandSender, @NotNull String s) {
        if (hasCustomPermission(commandSender, "command")) {
            String[] strings = arguments.args();
            if (strings.length == 1) {
                switch (strings[0]) {
                    case "add" -> {
                        if (commandSender instanceof Player p) {
                            NMSFakePlayerMaker.spawnFakePlayer(p.getLocation(), null, commandSender);
                            sendMessage(commandSender, "CreateSuccess");
                        } else {
                            if (FakePlayerMaker.defaultSpawnLocation != null) {
                                NMSFakePlayerMaker.spawnFakePlayer(FakePlayerMaker.defaultSpawnLocation, null, commandSender);
                                sendMessage(commandSender, "CreateSuccess");
                            } else {
                                sendMessage(commandSender, "SpecifyLocation");
                            }
                        }
                    }
                    case "reload" -> {
                        FakePlayerMaker.reload();
                        sendMessage(commandSender, "ReloadSuccess");
                    }
                    case "removeAll" -> {
                        NMSFakePlayerMaker.removeAllFakePlayers(commandSender);
                        sendMessage(commandSender, "RemoveAllSuccess");
                    }
                    default -> Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                }
            } else if (strings.length == 2) {
                String name = strings[1];
                switch (strings[0]) {
                    case "add" -> {
                        if (!(commandSender instanceof Player p)) {
                            if (FakePlayerMaker.defaultSpawnLocation == null) {
                                sendMessage(commandSender, "SpecifyLocation");
                                return;
                            }
                            NMSFakePlayerMaker.spawnFakePlayer(FakePlayerMaker.defaultSpawnLocation, name, commandSender);
                            sendMessage(commandSender, "CreateSuccess");
                            return;
                        }
                        NMSFakePlayerMaker.spawnFakePlayer(p.getLocation(), name, commandSender);
                        sendMessage(commandSender, "CreateSuccess");
                    }
                    case "remove" -> {
                        if (NMSFakePlayerMaker.fakePlayerMap.containsKey(name)) {
                            NMSFakePlayerMaker.removeFakePlayer(name, commandSender);
                            sendMessage(commandSender, "RemoveSuccess");
                        } else {
                            sendMessage(commandSender, "PlayerNotFound");
                        }
                    }
                    case "teleport", "tp" -> {
                        if (NMSFakePlayerMaker.fakePlayerMap.containsKey(name)) {
                            Player player = toPlayer();
                            ServerPlayer serverPlayer = NMSFakePlayerMaker.fakePlayerMap.get(name);
                            if (player == null) {
                                sendMessage(commandSender, "PlayerNotFound");
                                return;
                            }
                            player.teleport(serverPlayer.getBukkitEntity());
                            FakePlayerMaker.fakePlayerSaver.syncPlayerInfo(serverPlayer);
                            sendMessage(commandSender, "TeleportSuccess");
                        } else {
                            sendMessage(commandSender, "PlayerNotFound");
                        }
                    }
                    case "join" -> {
                        if (NMSFakePlayerMaker.fakePlayerMap.containsKey(name)) {
                            NMSFakePlayerMaker.joinFakePlayer(name);
                            return;
                        }
                        sendMessage(commandSender, "PlayerNotFound");
                    }
                    case "mount" -> {
                        ServerPlayer player = NMSFakePlayerMaker.fakePlayerMap.get(name);
                        if (player == null) {
                            sendMessage(commandSender, "PlayerNotFound");
                            return;
                        }
                        ActionUtils.mountNearest(player);
                    }
                    case "unmount" -> {
                        ServerPlayer player = NMSFakePlayerMaker.fakePlayerMap.get(name);
                        if (player == null) {
                            sendMessage(commandSender, "PlayerNotFound");
                        }
                        ActionUtils.unmount(player);
                    }
                    case "inventory" -> {
                        ServerPlayer player = NMSFakePlayerMaker.fakePlayerMap.get(name);
                        if (player == null) {
                            sendMessage(commandSender, "PlayerNotFound");
                        }
                        Player p1 = toPlayer();
                        if (p1 == null) return;
                        Player p2 = player.getBukkitEntity();
                        p1.openInventory(p2.getInventory());
                    }
                    case "sneak" -> {
                        ServerPlayer player = NMSFakePlayerMaker.fakePlayerMap.get(name);
                        if (player == null) {
                            sendMessage(commandSender, "PlayerNotFound");
                            return;
                        }
                        player.setShiftKeyDown(true);
                        player.setPose(player.isShiftKeyDown() ? Pose.CROUCHING : Pose.STANDING);
                    }
                    default -> Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                }
            } else if (strings.length == 3) {
                //function commands here
                String name = strings[1];
                switch (strings[0]) {
                    case "skin" -> {
                        String skin = strings[2];
                        ServerPlayer player = NMSFakePlayerMaker.fakePlayerMap.get(name);
                        if (player != null) {
                            skinChange(player, commandSender, skin);
                            return;
                        }
                        sendMessage(commandSender, "PlayerNotFound");
                        return;
                    }
                    case "chat" -> {
                        String chat = strings[2];
                        ServerPlayer player = NMSFakePlayerMaker.fakePlayerMap.get(name);
                        if (player == null) {
                            sendMessage(commandSender, "PlayerNotFound");
                            return;
                        }
                        ActionUtils.chat(player, chat);
                        return;
                    }
                    case "look" -> {
                        commandSender.sendMessage("You can't use the command till a bug fixed");
                        /*
                        ServerPlayer player = NMSFakePlayerMaker.fakePlayerMap.get(name);
                        if (player == null) {
                            sendMessage(commandSender, "PlayerNotFound");
                            return;
                        }
                        try {
                            Direction direction = Direction.valueOf(strings[2].toUpperCase());
                            ActionUtils.look(player, direction);
                            return;
                        } catch (IllegalArgumentException e) {
                            sendMessage(commandSender, "InvalidDirection");
                            return;
                        }

                         */
                        return;
                    }
                    case "jump" -> {
                        ServerPlayer player = NMSFakePlayerMaker.fakePlayerMap.get(name);
                        if (player == null) {
                            sendMessage(commandSender, "PlayerNotFound");
                            return;
                        }
                        try {
                            int times = Integer.parseInt(strings[2]);
                            if (times <= 0) {
                                Polymer.messageHandler.sendMessage(commandSender, "Value.TooLow", 3);
                                return;
                            }
                            for (int i = 0; i < times; i++) {
                                player.jumpFromGround();
                            }
                            return;
                        } catch (NumberFormatException e) {
                            if (strings[2].equals("hold")) {
                                player.setJumping(true);
                                return;
                            } else if (strings[2].equals("stop")) {
                                player.setJumping(false);
                                return;
                            } else {
                                sendMessage(commandSender, "InvalidJumpValue");
                                return;
                            }
                        }
                    }
                    case "pose" -> {
                        ServerPlayer player = NMSFakePlayerMaker.fakePlayerMap.get(name);
                        if (player == null) {
                            sendMessage(commandSender, "PlayerNotFound");
                            return;
                        }
                        try {
                            Pose pose = Pose.valueOf(strings[2]);
                            player.setPose(pose);
                            return;
                        } catch (IllegalArgumentException e) {
                            sendMessage(commandSender, "InvalidPoseValue");
                            return;
                        }
                    }
                }
                //function commands end
                if (strings[0].equals("add")) {
                    Location location = ObjectConverter.toLocation(strings[2]);
                    if (location == null) {
                        sendMessage(commandSender, "SpecifyLocation");
                        return;
                    }
                    NMSFakePlayerMaker.spawnFakePlayer(location, name, commandSender);
                    sendMessage(commandSender, "CreateSuccess");
                    return;
                }
                Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
            } else if (strings.length == 4 && strings[0].equals("stress")) {
                if (hasCustomPermission(commandSender, "command.stress")) {
                    switch (strings[1]) {
                        case "area" -> {
                            Optional<AreaStressTester> tester = FakePlayerMaker.stressTestSaver.getStressTesterArea(strings[3]);
                            switch (strings[2]) {
                                case "start" -> {
                                    if (!FakePlayerMaker.settings.getBoolean("areaStressTesters")) {
                                        sendMessage(commandSender, "Stress.AreaNotEnabled");
                                        return;
                                    }
                                    if (tester.isEmpty()) {
                                        sendMessage(commandSender, "Stress.NotFound");
                                        return;
                                    }
                                    AreaStressTester stressTester = tester.get();
                                    try {
                                        stressTester.run();
                                    } catch (WorldNotFoundException e) {
                                        sendMessage(commandSender, "Stress.AreaWorldNotFound");
                                    } catch (IllegalStateException e) {
                                        sendMessage(commandSender, "Stress.StartFast");
                                    }
                                }
                                case "stop" -> {
                                    if (!FakePlayerMaker.settings.getBoolean("areaStressTesters")) {
                                        sendMessage(commandSender, "Stress.AreaNotEnabled");
                                        return;
                                    }
                                    if (tester.isEmpty()) {
                                        sendMessage(commandSender, "Stress.NotFound");
                                        return;
                                    }
                                    AreaStressTester stressTester = tester.get();
                                    if (!stressTester.isStarted()) {
                                        sendMessage(commandSender, "Stress.NotStarted");
                                        return;
                                    }
                                    stressTester.stop();
                                }
                                case "create" -> handleAreaCreate(commandSender, strings);
                                case "players" -> listPlayers(commandSender, tester);
                                default -> Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                            }
                        }
                        case "randomworld" -> {
                            Optional<RandomWorldStressTester> tester = FakePlayerMaker.stressTestSaver.getStressTesterRandomWorld(strings[3]);
                            switch (strings[2]) {
                                case "start" -> {
                                    if (tester.isEmpty()) {
                                        sendMessage(commandSender, "Stress.NotFound");
                                        return;
                                    }
                                    RandomWorldStressTester stressTester = tester.get();
                                    try {
                                        stressTester.run();
                                    } catch (IllegalStateException e) {
                                        sendMessage(commandSender, "Stress.StartFast");
                                    }
                                }
                                case "stop" -> {
                                    if (tester.isEmpty()) {
                                        sendMessage(commandSender, "Stress.NotFound");
                                        return;
                                    }
                                    RandomWorldStressTester stressTester = tester.get();
                                    if (!stressTester.isStarted()) {
                                        sendMessage(commandSender, "Stress.NotStarted");
                                        return;
                                    }
                                    stressTester.stop();
                                }
                                case "create" -> {
                                    FakePlayerMaker.stressTestSaver.newRandomWorldTester(strings[3], 100);
                                    sendMessage(commandSender, "Stress.TesterCreated");
                                }
                                case "players" -> listPlayers(commandSender, tester);
                                default -> Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                            }
                        }
                        default -> Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                    }
                }
            } else if (strings.length == 5) {
                switch (strings[0]) {
                    case "stress" -> {
                        int amount = (int) getArgAsDoubleOrInt(4, true, false);
                        if (hasCustomPermission(commandSender, "command.stress")) {
                            switch (strings[1]) {
                                case "area" -> {
                                    if (strings[2].equals("create")) {
                                        if (amount != PolymerConstants.ERROR_CODE) {
                                            handleAreaCreate(commandSender, strings, amount);
                                        }
                                    }
                                    Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                                }
                                case "randomworld" -> {
                                    if (strings[2].equals("create")) {
                                        if (amount != PolymerConstants.ERROR_CODE) {
                                            FakePlayerMaker.stressTestSaver.newRandomWorldTester(strings[3], amount);
                                            sendMessage(commandSender, "Stress.TesterCreated");
                                            return;
                                        }
                                    }
                                    Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                                }
                                default -> Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                            }
                        }
                    }
                    case "lookat" -> {
                        double x, y, z;
                        String name = strings[1];
                        int err = 0;
                        try {
                            err = 3;
                            x = Double.parseDouble(strings[2]);
                            err = 4;
                            y = Double.parseDouble(strings[3]);
                            err = 5;
                            z = Double.parseDouble(strings[4]);
                        } catch (NumberFormatException e) {
                            Polymer.messageHandler.sendMessage(commandSender, "Value.NotDouble", err);
                            return;
                        }
                        if (x != PolymerConstants.ERROR_CODE && y != PolymerConstants.ERROR_CODE && z != PolymerConstants.ERROR_CODE) {
                            ServerPlayer player = NMSFakePlayerMaker.fakePlayerMap.get(name);
                            if (player != null) {
                                ActionUtils.lookAtBlock(player, new Vec3(x, y, z));
                            } else {
                                sendMessage(commandSender, "PlayerNotFound");
                            }
                        }
                    }
                }
            } else {
                Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
            }
        }
    }

    private void listPlayers(@NotNull CommandSender commandSender, Optional<? extends IStressTester> tester) {
        if (tester.isEmpty()) {
            sendMessage(commandSender, "Stress.NotFound");
            return;
        }
        Set<String> names = tester.get().getTempPlayers().keySet();
        commandSender.sendMessage(names.toString());
    }

    private void skinChange(ServerPlayer player, CommandSender operator, String targetName) {
        Player bukkit = player.getBukkitEntity();
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
        } catch (IllegalStateException | IOException | NullPointerException exception) {
            sendMessage(operator, "SkinChangeFailed");
        }
    }
}
