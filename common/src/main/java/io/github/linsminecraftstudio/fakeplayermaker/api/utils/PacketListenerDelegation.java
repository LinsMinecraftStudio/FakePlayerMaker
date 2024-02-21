package io.github.linsminecraftstudio.fakeplayermaker.api.utils;

import io.github.linsminecraftstudio.fakeplayermaker.api.implementation.Implementations;
import net.bytebuddy.implementation.bind.annotation.*;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.lang.reflect.Method;

public class PacketListenerDelegation {
    PacketListenerDelegation() {
    }

    @RuntimeType
    public Object delegate(@This ServerGamePacketListenerImpl o, @Origin Method method, @FieldValue("pl") ServerPlayer player,
                           @SuperMethod Method superMethod, @AllArguments Object... args) {
        try {
            if (method.getName().equals("a")) {
                return null;
            } else if (method.getName().equals("internalTeleport")) {
                superMethod.invoke(o, args);
                Method serverLevel = ServerPlayer.class.getMethod("x");
                ServerLevel level = (ServerLevel) serverLevel.invoke(player);
                if (level.getPlayerByUUID(Implementations.getUUID(player)) != null) {
                    ServerGamePacketListenerImpl.class.getMethod("d").invoke(o);
                    ServerChunkCache source = level.getChunkSource();
                    source.move(player);
                }
                return null;
            }

            return method.invoke(o, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
