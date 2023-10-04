package org.lins.mmmjjkx.ownfakeplayers.objects;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public record OwnableFakePlayer(UUID owner, ServerPlayer player){
}
