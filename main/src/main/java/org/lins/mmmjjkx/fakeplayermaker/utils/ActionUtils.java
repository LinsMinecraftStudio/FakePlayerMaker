package org.lins.mmmjjkx.fakeplayermaker.utils;

import io.github.linsminecraftstudio.fakeplayermaker.api.implementation.Implementations;
import io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class ActionUtils {
    private static final Class<ServerPlayer> clazz = ServerPlayer.class;
    public static void chat(ServerPlayer player, String message) {
        MinecraftUtils.scheduleNoDelay(FakePlayerMaker.INSTANCE, () -> {
            ChatDecorator.ModernResult result = new ChatDecorator.ModernResult(Component.text(message), true, true);
            PlayerChatMessage message1 = new PlayerChatMessage(SignedMessageLink.unsigned(Implementations.getUUID(player)), null, SignedMessageBody.unsigned(message), null, FilterMask.PASS_THROUGH, result);
            ChatProcessor processor = new ChatProcessor(MinecraftServer.getServer(), player, message1, true);
            processor.process();
        }, true);
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
            case UP    -> look(player, player.getBukkitYaw(), -90);
            case DOWN  -> look(player, player.getBukkitYaw(), 90);
        }
    }

    public static void look(ServerPlayer player, float yaw, float pitch) {
        player.setYRot(yaw % 360); //set yaw
        player.setXRot(Mth.clamp(pitch, -90, 90)); // set pitch

        Player bukkit = Implementations.bukkitEntity(player);

        MinecraftUtils.getNMSServer().getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(player, (byte) (bukkit.getYaw() % 360 * 256 / 360)));
        MinecraftUtils.getNMSServer().getPlayerList().broadcastAll(new ClientboundMoveEntityPacket.Rot(getID(player), (byte) (bukkit.getYaw() % 360 * 256 / 360), (byte) (bukkit.getPitch() % 360 * 256 / 360), bukkit.isOnGround()));
    }

    public static void mountNearest(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        AABB boundingBox = switch (Bukkit.getMinecraftVersion()) {
            case "1.20.1" -> {
                try {
                    yield (AABB) clazz.getMethod("cE").invoke(player);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
            default -> player.getBoundingBox();
        };
        List<Entity> rideable = ListUtil.getAllMatches(level.getEntities(player, boundingBox.inflate(3,2,3)), e ->
                e instanceof Minecart || e instanceof AbstractHorse || e instanceof Boat);
        if (!rideable.isEmpty()) {
            Entity entity = rideable.get(0);
            player.startRiding(entity, true);
        }
    }

    public static void unmount(ServerPlayer player) {
        try {
            switch (Bukkit.getMinecraftVersion()) {
                case "1.20.1" -> clazz.getMethod("Y").invoke(player);
                case "1.20.2" -> clazz.getMethod("aa").invoke(player);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setupValues(ServerPlayer player) {
        SimpleSettingsManager settings = FakePlayerMaker.settings;
        player.setInvulnerable(settings.getBoolean("player.invulnerable"));
        player.bukkitPickUpLoot = settings.getBoolean("player.canPickupItems");
        player.collides = settings.getBoolean("player.collision");
    }

    private static int getID(ServerPlayer player) {
        try {
            return switch (Bukkit.getMinecraftVersion()) {
                case "1.20.1" -> (int) clazz.getMethod("af").invoke(player);
                case "1.20.2" -> player.getId();
                default -> 0;
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
