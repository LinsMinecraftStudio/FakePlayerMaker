package org.lins.mmmjjkx.fakeplayermaker.listeners;

import io.github.linsminecraftstudio.polymer.objects.plugin.SimpleSettingsManager;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;

import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getCraftClass;
import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getHandle;

public class InteractListener implements Listener {
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        SimpleSettingsManager settings = FakePlayerMaker.settings;
        ServerPlayer player = (ServerPlayer) getHandle(getCraftClass("entity.CraftPlayer"), e.getEntity().getPlayer());
        if (NMSFakePlayerMaker.fakePlayerMap.containsKey(player.getName().getString())) {
            e.getPlayer().spigot().respawn();
            player.setInvulnerable(settings.getBoolean("player.invulnerable"));
            player.bukkitPickUpLoot = settings.getBoolean("player.canPickupItems");
            player.collides = settings.getBoolean("player.collision");
        }
    }
}
