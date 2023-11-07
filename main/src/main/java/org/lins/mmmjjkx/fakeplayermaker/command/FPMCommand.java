package org.lins.mmmjjkx.fakeplayermaker.command;

import io.github.linsminecraftstudio.fakeplayermaker.api.implementation.Implementations;
import io.github.linsminecraftstudio.fakeplayermaker.api.interfaces.IStressTester;
import io.github.linsminecraftstudio.fakeplayermaker.api.objects.WorldNotFoundException;
import io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils;
import io.github.linsminecraftstudio.polymer.command.PolymerCommand;
import io.github.linsminecraftstudio.polymer.command.presets.sub.SubReloadCommand;
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
import java.util.*;
import java.util.stream.Collectors;

import static org.lins.mmmjjkx.fakeplayermaker.hook.WEHook.handleAreaCreate;

public class FPMCommand extends PolymerCommand {
    public FPMCommand() {
        super("fakeplayermaker", new ArrayList<>(List.of("fpm")));
        registerSubCommand(new SubReloadCommand(FakePlayerMaker.INSTANCE));
    }

    @Override
    public String requirePlugin() {
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return copyPartialMatches(args[0], List.of(
                    "add","reload","removeAll","remove","stress","join","chat","teleport",
                    "tp","skin","lookat","jump","mount","unmount","pose","inventory","sneak","look"));
        } else if (args.length == 2) {
            return switch (args[0]) {
                case "remove","teleport","tp","join","chat","skin","lookat","jump","mount","unmount","pose","inventory","sneak","look"
                        -> copyPartialMatches(args[1], NMSFakePlayerMaker.fakePlayerMap.keySet());
                case "stress" -> copyPartialMatches(args[1], List.of("area","randomworld"));
                default -> new ArrayList<>();
            };
        } else if (args.length==3) {
            return switch (args[0]) {
                case "stress" -> copyPartialMatches(args[2], List.of("start","stop","players","create"));
                case "skin" -> copyPartialMatches(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> !NMSFakePlayerMaker.fakePlayerMap.containsKey(n)).collect(Collectors.toList()));
                case "look" -> copyPartialMatches(args[2], Arrays.stream(Direction.values()).map(d -> d.getName().toUpperCase()).collect(Collectors.toList()));
                case "jump" -> copyPartialMatches(args[2], List.of("1","2","3","4","5","hold","stop"));
                case "pose" -> copyPartialMatches(args[2], Arrays.stream(Pose.values()).map(Pose::toString).collect(Collectors.toList()));
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
                    case "removeAll" -> {
                        NMSFakePlayerMaker.removeAllFakePlayers(commandSender);
                        sendMessage(commandSender, "RemoveAllSuccess");
                    }
                    default -> sendPolymerMessage(commandSender, "Command.ArgError");
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
                            player.teleport(Implementations.bukkitEntity(serverPlayer));
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
                            return;
                        }
                        ActionUtils.unmount(player);
                    }
                    case "inventory" -> {
                        ServerPlayer player = NMSFakePlayerMaker.fakePlayerMap.get(name);
                        if (player == null) {
                            sendMessage(commandSender, "PlayerNotFound");
                            return;
                        }
                        Player p1 = toPlayer();
                        if (p1 == null) return;
                        Player p2 = Implementations.bukkitEntity(player);
                        p1.openInventory(p2.getInventory());
                    }
                    case "sneak" -> {
                        ServerPlayer player = NMSFakePlayerMaker.fakePlayerMap.get(name);
                        if (player == null) {
                            sendMessage(commandSender, "PlayerNotFound");
                            return;
                        }
                        Implementations.bukkitEntity(player).setSneaking(true);
                        player.setPose(player.isShiftKeyDown() ? Pose.CROUCHING : Pose.STANDING);
                    }
                    default -> sendPolymerMessage(commandSender, "Command.ArgError");
                }
            } else if (strings.length == 3) {
                //function commands here
                String name = strings[1];
                switch (strings[0]) {
                    case "skin" -> {
                        String skin = strings[2];
                        ServerPlayer player = NMSFakePlayerMaker.fakePlayerMap.get(name);
                        if (player != null) {
                            try {
                                MinecraftUtils.skinChange(player, skin);
                                sendMessage("SkinChanged");
                            } catch (IOException e) {
                                sendMessage("SkinChangeFailed");
                            }
                            FakePlayerMaker.fakePlayerSaver.syncPlayerInfo(player);
                            return;
                        }
                        sendMessage(commandSender, "PlayerNotFound");
                        return;
                    }
                    case "chat" -> {
                        String chat = strings[2];
                        ServerPlayer player = NMSFakePlayerMaker.fakePlayerMap.get(name);
                        if (player == null) {
                            sendMessage("PlayerNotFound");
                            return;
                        }
                        ActionUtils.chat(player, chat);
                        return;
                    }
                    case "look" -> {
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
                                sendPolymerMessage(commandSender, "Value.TooLow", 3);
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
                sendPolymerMessage(commandSender, "Command.ArgError");
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
                                default -> sendPolymerMessage(commandSender, "Command.ArgError");
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
                                default -> sendPolymerMessage(commandSender, "Command.ArgError");
                            }
                        }
                        default -> sendPolymerMessage(commandSender, "Command.ArgError");
                    }
                }
            } else if (strings.length == 5) {
                switch (strings[0]) {
                    case "stress" -> {
                        if (hasCustomPermission(commandSender, "command.stress")) {
                            int amount = (int) getArgAsDoubleOrInt(4, true, false);
                            switch (strings[1]) {
                                case "area" -> {
                                    if (strings[2].equals("create")) {
                                        if (amount != PolymerConstants.ERROR_CODE) {
                                            handleAreaCreate(commandSender, strings, amount);
                                        }
                                    }
                                    sendPolymerMessage(commandSender, "Command.ArgError");
                                }
                                case "randomworld" -> {
                                    if (strings[2].equals("create")) {
                                        if (amount != PolymerConstants.ERROR_CODE) {
                                            FakePlayerMaker.stressTestSaver.newRandomWorldTester(strings[3], amount);
                                            sendMessage(commandSender, "Stress.TesterCreated");
                                            return;
                                        }
                                    }
                                    sendPolymerMessage(commandSender, "Command.ArgError");
                                }
                                default -> sendPolymerMessage(commandSender, "Command.ArgError");
                            }
                        }
                    }

                    case "lookat" -> {
                        double x, y, z;
                        String name = strings[1];
                        x = getArgAsDoubleOrInt(2, false, true);
                        y = getArgAsDoubleOrInt(3, false, true);
                        z = getArgAsDoubleOrInt(4, false, true);
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
                sendPolymerMessage(commandSender, "Command.ArgError");
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
}
