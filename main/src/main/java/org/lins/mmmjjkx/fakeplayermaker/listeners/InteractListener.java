package org.lins.mmmjjkx.fakeplayermaker.listeners;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;

import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getCraftClass;
import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getHandle;

public class InteractListener implements Listener {
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        ServerPlayer player = (ServerPlayer) getHandle(getCraftClass("entity.CraftPlayer"), e.getEntity().getPlayer());
        if (NMSFakePlayerMaker.fakePlayerMap.containsKey(player.getName().getString())) {
            e.getPlayer().spigot().respawn();
        }
    }
}
