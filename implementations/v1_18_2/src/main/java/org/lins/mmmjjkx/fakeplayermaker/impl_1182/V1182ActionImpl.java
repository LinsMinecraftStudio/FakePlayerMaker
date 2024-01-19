package org.lins.mmmjjkx.fakeplayermaker.impl_1182;

import io.github.linsminecraftstudio.fakeplayermaker.api.implementation.ActionImpl;
import io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils;
import io.github.linsminecraftstudio.polymer.objects.plugin.PolymerPlugin;
import io.github.linsminecraftstudio.polymer.utils.IterableUtil;
import io.papermc.paper.adventure.ChatProcessor;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Direction;
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

import java.util.List;

public class V1182ActionImpl extends ActionImpl {
    @Override
    public String[] minecraftVersion() {
        return new String[]{"1.18.2"};
    }

    @Override
    public void lookAtBlock(ServerPlayer player, Vec3 v3) {
        player.lookAt(EntityAnchorArgument.Anchor.EYES, v3);
    }

    @Override
    public void look(ServerPlayer player, Direction direction) {
        switch (direction) {
            case NORTH -> look(player, 180, 0);
            case SOUTH -> look(player, 0, 0);
            case EAST -> look(player, -90, 0);
            case WEST -> look(player, 90, 0);
            case UP -> look(player, player.getYRot(), -90);
            case DOWN -> look(player, player.getYRot(), 90);
        }
    }

    @Override
    public void look(ServerPlayer player, float yaw, float pitch) {
        player.setYRot(yaw % 360); //set yaw
        player.setXRot(Mth.clamp(pitch, -90, 90)); // set pitch

        MinecraftServer server = MinecraftUtils.getNMSServer();

        server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(player, (byte) (player.getYRot() % 360 * 256 / 360)));
        server.getPlayerList().broadcastAll(new ClientboundMoveEntityPacket.Rot(player.getId(), (byte) (player.getYRot() % 360 * 256 / 360), (byte) (player.getXRot() % 360 * 256 / 360), player.onGround));
    }

    @Override
    public void mountNearest(ServerPlayer player) {
        ServerLevel level = player.getLevel();
        AABB boundingBox = player.getBoundingBox();
        List<Entity> rideable = IterableUtil.getAllMatches(level.getEntities(player, boundingBox.inflate(3, 2, 3)), e ->
                e instanceof Minecart || e instanceof AbstractHorse || e instanceof Boat);
        if (!rideable.isEmpty()) {
            Entity entity = rideable.get(0);
            player.startRiding(entity, true);
        }
    }

    @Override
    public void unmount(ServerPlayer player) {
        player.stopRiding();
    }

    @Override
    public void chat(PolymerPlugin plugin, ServerPlayer player, String message) {
        MinecraftUtils.scheduleNoDelay(plugin, () -> {
            ChatProcessor processor = new ChatProcessor(MinecraftUtils.getNMSServer(), player, message, true);
            processor.process();
        }, true);
    }
}
