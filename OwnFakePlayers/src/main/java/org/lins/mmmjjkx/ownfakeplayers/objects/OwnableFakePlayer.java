package org.lins.mmmjjkx.ownfakeplayers.objects;

import net.minecraft.server.level.ServerPlayer;

public record OwnableFakePlayer(String owner, ServerPlayer player) {
}
