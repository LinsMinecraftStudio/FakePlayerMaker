package org.lins.mmmjjkx.fakeplayermaker.objects;

public class SpecialFeatures {
    private final boolean firePlayerJoinEvent;
    private final boolean firePlayerQuitEvent;
    public SpecialFeatures(boolean firePJE, boolean firePQE) {
        this.firePlayerJoinEvent = firePJE;
        this.firePlayerQuitEvent = firePQE;
    }

    public boolean firePlayerJoinEvent() {
        return firePlayerJoinEvent;
    }

    public boolean firePlayerQuitEvent() {
        return firePlayerQuitEvent;
    }
}
