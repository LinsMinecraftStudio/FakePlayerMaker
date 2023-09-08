package io.github.linsminecraftstudio.fakeplayermaker.api.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
public class FakePlayerCreateEvent extends Event {
    private final Player player;
    @Getter
    private final CommandSender operator;

    public static HandlerList getHandlerList() {
        return new HandlerList();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return new HandlerList();
    }
}