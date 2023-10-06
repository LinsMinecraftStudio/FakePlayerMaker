package org.lins.mmmjjkx.fakeplayermaker.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatchers;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.objects.FPMPacketListener;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class PacketListenerMaker {
    public static ServerGamePacketListenerImpl getGamePacketListener(Connection connection, ServerPlayer player) {
        if (!Bukkit.getMinecraftVersion().equals("1.20.2")) {
            try {
                final MethodDelegation delegation = MethodDelegation.to(new Object() {
                    @RuntimeType
                    public Object delegate(
                            @This Object o,
                            @Origin Method method,
                            @FieldValue("pl") ServerPlayer player,
                            @AllArguments Object... args
                    ) {
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
                                        o.getClass().getMethod("d").invoke(this, args);
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
                });

                Constructor<? extends ServerGamePacketListenerImpl> constructor = new ByteBuddy()
                        .subclass(ServerGamePacketListenerImpl.class)
                        .name(PacketListenerMaker.class.getPackage().getName() + ".FPMGamePacketListenerBEFORE1202V")

                        .defineField("pl", ServerPlayer.class, Visibility.PRIVATE)
                        .defineConstructor(Visibility.PUBLIC)
                        .withParameters(MinecraftServer.class, Connection.class, ServerPlayer.class)
                        .intercept(MethodCall.invoke(ServerGamePacketListenerImpl.class.getDeclaredConstructor(MinecraftServer.class, Connection.class, ServerPlayer.class))
                                .withArgument(0, 1, 2)
                                .andThen(FieldAccessor.ofField("pl").setsArgumentAt(2))
                        )

                        .method((ElementMatchers.named("a").or(ElementMatchers.named("internalTeleport"))))
                        .intercept(MethodDelegation.to(delegation))

                        .make()
                        .load(ServerGamePacketListenerImpl.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                        .getLoaded()
                        .getDeclaredConstructor(MinecraftServer.class, Connection.class, ServerPlayer.class);

                return constructor.newInstance(FakePlayerMaker.getNMSServer(), connection, player);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return new FPMPacketListener(connection, player);
    }
}
