package io.github.linsminecraftstudio.fakeplayermaker.api.interfaces;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface FakePlayerController {
    boolean isFakePlayer(String name);

    List<ServerPlayer> getAllFakePlayers();

    Player asBukkitEntity(ServerPlayer player);

    ServerPlayer spawnFakePlayer(@Nullable String name, Location location);
}
