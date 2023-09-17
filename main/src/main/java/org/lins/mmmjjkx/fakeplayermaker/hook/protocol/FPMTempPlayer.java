package org.lins.mmmjjkx.fakeplayermaker.hook.protocol;

import com.comphenix.protocol.injector.temporary.TemporaryPlayer;
import com.comphenix.protocol.injector.temporary.TemporaryPlayerFactory;
import org.bukkit.entity.Player;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;

public class FPMTempPlayer extends TemporaryPlayer {
    private final FPMMinimalInjector fpmInjector;

    public FPMTempPlayer() {
        fpmInjector = new FPMMinimalInjector(NMSFakePlayerMaker.getRandomName(FakePlayerMaker.randomNameLength));
        TemporaryPlayerFactory.setInjectorInPlayer((Player) this, fpmInjector);
    }

    public Player get() {
        return fpmInjector.getPlayer();
    }
}
