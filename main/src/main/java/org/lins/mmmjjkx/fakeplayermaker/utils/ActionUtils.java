package org.lins.mmmjjkx.fakeplayermaker.utils;

import io.github.linsminecraftstudio.polymer.objects.plugin.SimpleSettingsManager;
import io.github.linsminecraftstudio.polymer.utils.ListUtil;
import io.papermc.paper.adventure.ChatProcessor;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.phys.Vec3;
import org.bukkit.scheduler.BukkitRunnable;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;

import java.util.List;

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

    public static void lookAtBlock(ServerPlayer player, Vec3 v3) {
        player.lookAt(EntityAnchorArgument.Anchor.EYES, v3);
    }

    public static void look(ServerPlayer player, Direction direction) {
        switch (direction) {
            case NORTH -> look(player, 180, 0);
            case SOUTH -> look(player, 0, 0);
            case EAST  -> look(player, -90, 0);
            case WEST  -> look(player, 90, 0);
            case UP    -> look(player, player.getYRot(), -90);
            case DOWN  -> look(player, player.getYRot(), 90);
        }
    }

    public static void look(ServerPlayer player, float yaw, float pitch) {
        player.setYRot(yaw % 360); //set yaw
        player.setXRot(Mth.clamp(pitch, -90, 90)); // set pitch

        FakePlayerMaker.getNMSServer().getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(player, (byte) (player.getYRot()%360*256/360)));
        FakePlayerMaker.getNMSServer().getPlayerList().broadcastAll(new ClientboundMoveEntityPacket.Rot(player.getId(), (byte) (player.getYRot()%360*256/360), (byte) (player.getXRot()%360*256/360), player.onGround()));
    }

    public static void mountNearest(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<Entity> rideable = ListUtil.getAllMatches(level.getEntities(player, player.getBoundingBox().inflate(3,2,3)), e ->
                e instanceof Minecart || e instanceof AbstractHorse || e instanceof Boat);
        if (!rideable.isEmpty()) {
            Entity entity = rideable.get(0);
            player.startRiding(entity, true);
        }
    }

    public static void unmount(ServerPlayer player) {
        player.stopRiding();
    }

    public static void setupValues(ServerPlayer player) {
        SimpleSettingsManager settings = FakePlayerMaker.settings;
        player.setInvulnerable(settings.getBoolean("player.invulnerable"));
        player.bukkitPickUpLoot = settings.getBoolean("player.canPickupItems");
        player.collides = settings.getBoolean("player.collision");
    }
}
