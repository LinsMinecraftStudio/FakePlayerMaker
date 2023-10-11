package io.github.linsminecraftstudio.fakeplayermaker.api.utils;

import io.github.linsminecraftstudio.fakeplayermaker.api.objects.CraftBukkitClassNotFoundError;
import io.github.linsminecraftstudio.fakeplayermaker.api.objects.FPMPacketListener;
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
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MinecraftUtils {
    private static final Logger LOGGER = Logger.getLogger("FakePlayerMaker");
    public static ServerGamePacketListenerImpl getGamePacketListener(Connection connection, ServerPlayer player) {
        if (!Bukkit.getMinecraftVersion().equals("1.20.2")) {
            try {
                final MethodDelegation delegation = MethodDelegation.to(new PacketListenerDelegation());

                Constructor<ServerGamePacketListenerImpl> connectionConstructor = ServerGamePacketListenerImpl.class.getDeclaredConstructor(MinecraftServer.class, Connection.class, ServerPlayer.class);

                Constructor<? extends ServerGamePacketListenerImpl> constructor = new ByteBuddy()
                        .subclass(ServerGamePacketListenerImpl.class)
                        .name(MinecraftUtils.class.getPackage().getName() + ".FPMGamePacketListenerBEFORE1202V")

                        .defineField("pl", ServerPlayer.class, Visibility.PRIVATE)
                        .constructor(ElementMatchers.any())

                        .intercept(MethodCall.invoke(connectionConstructor)
                                .withArgument(0, 1, 2)
                                .andThen(FieldAccessor.ofField("pl").setsArgumentAt(2))
                        )

                        .method((ElementMatchers.named("a").or(ElementMatchers.named("internalTeleport"))))
                        .intercept(delegation)

                        .make()
                        .load(MinecraftUtils.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                        .getLoaded()
                        .getDeclaredConstructor(MinecraftServer.class, Connection.class, ServerPlayer.class);

                return constructor.newInstance(getNMSServer(), connection, player);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return new FPMPacketListener(connection, player);
    }

    public static MinecraftServer getNMSServer() {
        try {
            return (MinecraftServer) getCraftClass("CraftServer").getMethod("getServer").invoke(Bukkit.getServer());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> getCraftClass(String name) {
        String version = Bukkit.getServer().getClass().getName().split("\\.")[3];
        String className = "org.bukkit.craftbukkit." + version + "." + name;
        Class<?> c;
        try {
            c = Class.forName(className);
        } catch (Exception e) {
            throw new CraftBukkitClassNotFoundError(name, e);
        }
        return c;
    }

    public static void preventListen(String clazzName) {
        try {
            Class<?> c = Class.forName(clazzName);
            preventListen(c);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void preventListen(Class<?> clazz) {
        if (JavaPlugin.class.isAssignableFrom(clazz)) {
            JavaPlugin plugin = JavaPlugin.getPlugin((Class<? extends JavaPlugin>) clazz);
            HandlerList.unregisterAll(plugin);
        }
    }

    public static Object getHandle(Class<?> craftClazz, Object obj) {
        try {
            return craftClazz.getDeclaredMethod("getHandle").invoke(craftClazz.cast(obj));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Could not get handle of " + obj + " the class is " + craftClazz.getName() + ",");
        }
        return null;
    }
}
