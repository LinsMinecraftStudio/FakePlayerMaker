package org.lins.mmmjjkx.fakeplayermaker.hook;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import io.github.linsminecraftstudio.polymer.Polymer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;

public class WEHook {
    public static boolean handleAreaCreate(@NotNull CommandSender sender, @NotNull String[] strings) {
        return handleAreaCreate(sender, strings, 100);
    }

    public static boolean handleAreaCreate(@NotNull CommandSender commandSender, @NotNull String[] strings, int amount) {
        if (!FakePlayerMaker.settings.getBoolean("areaStressTesters")) {
            sendMessage(commandSender, "Stress.AreaNotEnabled");
            return false;
        }

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
            FakePlayerMaker.stressTestSaver.newAreaTester(strings[3], pos1, pos2, amount);
            sendMessage(commandSender, "Stress.TesterCreated");
            return true;
        }
        return false;
    }

    private static void sendMessage(CommandSender sender, String key, Object... args) {
        FakePlayerMaker.INSTANCE.getMessageHandler().sendMessage(sender, key, args);
    }

    private static Player toPlayer(CommandSender cs){
        if (cs instanceof Player p){
            return p;
        }else {
            Polymer.INSTANCE.getMessageHandler().sendMessage(cs, "Command.RunAsConsole");
            return null;
        }
    }
}
