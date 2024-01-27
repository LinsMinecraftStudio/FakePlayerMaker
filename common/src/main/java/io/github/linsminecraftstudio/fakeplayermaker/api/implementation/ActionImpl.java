package io.github.linsminecraftstudio.fakeplayermaker.api.implementation;

import io.github.linsminecraftstudio.polymer.objects.plugin.PolymerPlugin;
import io.github.linsminecraftstudio.polymer.objects.plugin.SimpleSettingsManager;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public abstract class ActionImpl {
    private static ActionImpl current;

    public ActionImpl() {
    }

    public static void register(ActionImpl impl) {
        current = impl;
    }

    public static ActionImpl get() {
        return current;
    }

    public abstract void lookAtBlock(ServerPlayer player, Vec3 v3);
    public abstract void look(ServerPlayer player, Direction direction);
    public abstract void look(ServerPlayer player, float yaw, float pitch);
    public abstract void mountNearest(ServerPlayer player);
    public abstract void unmount(ServerPlayer player);
    public abstract void chat(PolymerPlugin plugin, ServerPlayer player, String message);

    public static void setupValues(SimpleSettingsManager settings, ServerPlayer player) {
        player.setInvulnerable(settings.getBoolean("player.invulnerable"));
        player.bukkitPickUpLoot = settings.getBoolean("player.canPickupItems");
        player.collides = settings.getBoolean("player.collision");
    }
}
