package org.lins.mmmjjkx.fakeplayermaker.utils;

import io.papermc.paper.adventure.ChatProcessor;
import net.kyori.adventure.text.Component;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.scheduler.BukkitRunnable;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;

public class ActionUtils {
    public static void chat(ServerPlayer player, String message) {
        new BukkitRunnable() {
            @Override
            public void run() {
                ChatDecorator.ModernResult result = new ChatDecorator.ModernResult(Component.text(message), true, true);
                PlayerChatMessage message1 = new PlayerChatMessage(SignedMessageLink.unsigned(player.getUUID()), null, SignedMessageBody.unsigned(message), null, FilterMask.PASS_THROUGH, result);
                ChatProcessor processor = new ChatProcessor(MinecraftServer.getServer(), player, message1, true);
                processor.process();
            }
        }.runTaskAsynchronously(FakePlayerMaker.INSTANCE);
    }
}
