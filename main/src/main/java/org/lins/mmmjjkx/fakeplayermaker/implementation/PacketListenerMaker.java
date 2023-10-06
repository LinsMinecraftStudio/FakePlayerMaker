package org.lins.mmmjjkx.fakeplayermaker.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.objects.FPMPacketListener;

import java.lang.reflect.Constructor;

public class PacketListenerMaker {
    public static ServerGamePacketListenerImpl getGamePacketListener(Connection connection, ServerPlayer player) {
        if (!Bukkit.getMinecraftVersion().equals("1.20.2")) {
            try {
                final MethodDelegation delegation = MethodDelegation.to(new PacketListenerDelegation());

                Constructor<ServerGamePacketListenerImpl> connectionConstructor = ServerGamePacketListenerImpl.class.getDeclaredConstructor(MinecraftServer.class, Connection.class, ServerPlayer.class);

                Constructor<? extends ServerGamePacketListenerImpl> constructor = new ByteBuddy()
                        .subclass(ServerGamePacketListenerImpl.class)
                        .name(PacketListenerMaker.class.getPackage().getName() + ".FPMGamePacketListenerBEFORE1202V")

                        .defineField("pl", ServerPlayer.class, Visibility.PRIVATE)
                        .constructor(ElementMatchers.any())

                        .intercept(MethodCall.invoke(connectionConstructor)
                                .withArgument(0, 1, 2)
                                .andThen(FieldAccessor.ofField("pl").setsArgumentAt(2))
                        )

                        .method((ElementMatchers.named("a").or(ElementMatchers.named("internalTeleport"))))
                        .intercept(delegation)

                        .make()
                        .load(PacketListenerMaker.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
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
