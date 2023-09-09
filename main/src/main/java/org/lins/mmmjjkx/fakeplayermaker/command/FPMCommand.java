package org.lins.mmmjjkx.fakeplayermaker.command;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import io.github.linsminecraftstudio.fakeplayermaker.api.interfaces.IStressTester;
import org.lins.mmmjjkx.fakeplayermaker.WorldNotFoundException;
import io.github.linsminecraftstudio.polymer.Polymer;
import io.github.linsminecraftstudio.polymer.command.PolymerCommand;
import io.github.linsminecraftstudio.polymer.utils.ObjectConverter;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.stress.AreaStressTester;
import org.lins.mmmjjkx.fakeplayermaker.stress.RandomWorldStressTester;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
            return copyPartialMatches(args[0], List.of("add","reload","removeAll","remove", "stress"));
        } else if (args.length == 2) {
            return switch (args[0]) {
                case "remove","teleport","tp" -> copyPartialMatches(args[1], NMSFakePlayerMaker.fakePlayerMap.keySet());
                case "stress" -> copyPartialMatches(args[1], List.of("area","randomworld"));
                default -> new ArrayList<>();
            };
        } else if (args.length==3 & args[0].equals("stress")) {
            return List.of("start","stop","players","create");
        }
        return new ArrayList<>();
    }

    @Override
    public boolean execute(@NotNull CommandSender commandSender, @NotNull String s, @NotNull String[] strings) {
        if (hasCustomPermission(commandSender,"command")) {
            if (strings.length == 1) {
                return switch (strings[0]) {
                    case "add" -> {
                        Player p = toPlayer(commandSender);
                        if (p == null) {
                            sendMessage(commandSender, "SpecifyLocation");
                            yield false;
                        }
                        NMSFakePlayerMaker.spawnFakePlayer(p.getLocation(), null, commandSender);
                        sendMessage(commandSender, "CreateSuccess");
                        yield true;
                    }
                    case "reload" -> {
                        pluginInstance.reloadConfig();
                        FakePlayerMaker.fakePlayerSaver.reload();
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
                        Player p = toPlayer(commandSender);
                        if (p == null) {
                            Location location = ObjectConverter.toLocation(name);
                            if (location != null) {
                                NMSFakePlayerMaker.spawnFakePlayer(location, name, commandSender);
                                sendMessage(commandSender, "CreateSuccess");
                                yield true;
                            }
                            sendMessage(commandSender, "SpecifyLocation");
                            yield false;
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
                            sendMessage(commandSender, "Command.PlayerNotFound");
                            yield false;
                        }
                    }
                    case "teleport", "tp" -> {
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
            } /*else if (strings.length == 3) {
                if (strings[0].equals("skin")) {
                    String name = strings[1];
                    ServerPlayer player = NMSFakePlayerMaker.fakePlayerMap.get(name);
                    if (player != null) {
                        GameProfile profile = player.getGameProfile();
                        profile.getProperties().removeAll("textures");
                        profile.getProperties().put("textures", new Property("textures", strings[2]));
                        player.gameProfile = profile;
                        return true;
                    }
                    sendMessage(commandSender, "PlayerNotFound");
                    return true;
                } else {
                    Polymer.messageHandler.sendMessage(commandSender, "Command.ArgError");
                    return false;
                }
            }*/ else if (strings.length==4 && strings[0].equals("stress")) {
                return hasCustomPermission(commandSender, "command.stress") && switch (strings[1]) {
                    case "area" -> {
                        Optional<AreaStressTester> tester = FakePlayerMaker.stressTestSaver.getStressTesterArea(strings[3]);
                        switch (strings[2]) {
                            case "start" -> {
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
                        Optional<RandomWorldStressTester> tester = FakePlayerMaker.stressTestSaver.getStressTesterRandomWorld(strings[2]);
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
                int amount = toInteger(commandSender, strings[4], 5);
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

    private boolean handleAreaCreate(@NotNull CommandSender sender, @NotNull String[] strings) {
        return handleAreaCreate(sender, strings, 100);
    }

    private boolean handleAreaCreate(@NotNull CommandSender commandSender, @NotNull String[] strings, int amount) {
        Player p = toPlayer(commandSender);
        if (p != null) {
            com.sk89q.worldedit.entity.Player wep = BukkitAdapter.adapt(p);
            SessionManager manager = WorldEdit.getInstance().getSessionManager();
            LocalSession session = manager.get(wep);
            Region region;
            try {
                region = session.getSelection();
            } catch (IncompleteRegionException e) {
                sendMessage(commandSender, "Stress.SelectRegion");
                return false;
            }
            CuboidRegion cr = region.getBoundingBox();
            if (cr.getWorld() == null) {
                sendMessage(commandSender, "Stress.AreaWorldNotFound");
                return false;
            }
            World bkWorld = BukkitAdapter.adapt(cr.getWorld());
            BlockVector3 bv3p1 = cr.getPos1();
            BlockVector3 bv3p2 = cr.getPos2();
            Location pos1 = new Location(bkWorld, bv3p1.getX(), bv3p1.getY(), bv3p1.getZ());
            Location pos2 = new Location(bkWorld, bv3p2.getX(), bv3p2.getY(), bv3p2.getZ());
            FakePlayerMaker.stressTestSaver.newAreaTester(strings[2], pos1, pos2, amount);
            sendMessage(commandSender, "Stress.TesterCreated");
            return true;
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
}
