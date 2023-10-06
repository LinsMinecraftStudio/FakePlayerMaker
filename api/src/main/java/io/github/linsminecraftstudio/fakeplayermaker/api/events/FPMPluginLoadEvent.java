package io.github.linsminecraftstudio.fakeplayermaker.api.events;

import io.github.linsminecraftstudio.fakeplayermaker.api.interfaces.FakePlayerController;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class FPMPluginLoadEvent extends Event {
    private final FakePlayerController controller;

    public FPMPluginLoadEvent(FakePlayerController controller) {
        super(true);
        this.controller = controller;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return new HandlerList();
    }

    public @NotNull HandlerList getHandlerList() {
        return new HandlerList();
    }
}
