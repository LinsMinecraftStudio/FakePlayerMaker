package org.lins.mmmjjkx.fakeplayermaker.listeners;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;

public class PlayerListener implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        Player player = e.getPlayer();

        if (FakePlayerMaker.countFakePlayersOnMOTD) {
            MinecraftServer server = MinecraftServer.getServer();
            if (NMSFakePlayerMaker.fakePlayerMap.containsKey(player.getName())) {
                ServerPlayer serverPlayer = NMSFakePlayerMaker.fakePlayerMap.get(player.getName());
                server.getPlayerList().players.add(serverPlayer);
            }
        }
    }
}
