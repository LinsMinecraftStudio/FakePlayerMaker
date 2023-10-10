package io.github.linsminecraftstudio.fakeplayermaker.api.utils;

import net.bytebuddy.implementation.bind.annotation.*;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.lang.reflect.Method;

public class PacketListenerDelegation {
    PacketListenerDelegation() {
    }

    @RuntimeType
    public Object delegate(@This Object o, @Origin Method method, @FieldValue("pl") ServerPlayer player, @AllArguments Object... args) {
        try {
            switch (method.getName()) {
                case "a" -> {
                    return null;
                }
                case "internalTeleport" -> {
                    method.invoke(o, args);
                    if (player.serverLevel().getPlayerByUUID(
                            UUIDUtil.createOfflinePlayerUUID(player.getName().getString())
                    ) != null) {
                        ServerGamePacketListenerImpl.class.getMethod("d").invoke(this, args);
                        player.serverLevel().getChunkSource().move(player);
                    }
                    return null;
                }
            }


            return method.invoke(o, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
