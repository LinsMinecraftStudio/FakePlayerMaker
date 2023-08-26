package io.github.linsminecraftstudio.fakeplayermaker.api.events;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FakePlayerRemoveEvent extends Event {
    private final Player p;
    private final CommandSender o;

    public FakePlayerRemoveEvent(Player p, @Nullable CommandSender operator) {
        super();
        this.p = p;
        this.o = operator;
    }

    public Player getPlayer() {
        return p;
    }

    public CommandSender getOperator() {
        return o;
    }

    public static HandlerList getHandlerList() {
        return new HandlerList();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return new HandlerList();
    }
}