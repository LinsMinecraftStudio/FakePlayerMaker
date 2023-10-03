package org.lins.mmmjjkx.fakeplayermaker.listeners;

import io.github.linsminecraftstudio.polymer.objects.plugin.SimpleSettingsManager;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.command.FPMCommand;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;

import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getCraftClass;
import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getHandle;

public class FPMListener implements Listener {
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        SimpleSettingsManager settings = FakePlayerMaker.settings;
        ServerPlayer player = (ServerPlayer) getHandle(getCraftClass("entity.CraftPlayer"), e.getEntity().getPlayer());
        if (player != null && NMSFakePlayerMaker.fakePlayerMap.containsKey(player.getName().getString())) {
            Location loc = e.getPlayer().getLocation();
            e.getPlayer().spigot().respawn();
            player.setInvulnerable(settings.getBoolean("player.invulnerable"));
            player.bukkitPickUpLoot = settings.getBoolean("player.canPickupItems");
            player.collides = settings.getBoolean("player.collision");
            if (settings.getBoolean("player.respawnBack")) {
                e.getPlayer().teleport(loc);
            }
        }
    }
}
