package io.github.linsminecraftstudio.fakeplayermaker.api.events;

import io.github.linsminecraftstudio.fakeplayermaker.api.interfaces.IStressTester;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class StressTesterStartEvent extends Event {
    private final IStressTester stressTester;

    public StressTesterStartEvent(IStressTester stressTester) {
        super();
        this.stressTester = stressTester;
    }

    public IStressTester getStressTester() {
        return stressTester;
    }

    public static HandlerList getHandlerList() {
        return new HandlerList();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return new HandlerList();
    }
}
