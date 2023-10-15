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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MinecraftUtils {
    private static final Logger LOGGER = Logger.getLogger("FakePlayerMaker");
    private static boolean modernSchedulers = false;

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            modernSchedulers = true;
        } catch (ClassNotFoundException ignored) {
        }
    }

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

    /**
     * Get craft bukkit class
     * If not found it will throw {@link CraftBukkitClassNotFoundError}
     *
     * @param name the class name
     * @return a craft bukkit class
     */
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
        } catch (Exception ignored) {
        }
    }

    public static void preventListen(Class<?> clazz) {
        if (clazz.getSuperclass() == JavaPlugin.class) {
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

    public static void schedule(JavaPlugin plugin, Runnable runnable, long delay, boolean async) {
        if (async) {
            if (modernSchedulers) {
                Bukkit.getAsyncScheduler().runDelayed(plugin, t -> runnable.run(), delay, TimeUnit.MILLISECONDS);
            } else {
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay * 20L);
            }
        } else {
            if (modernSchedulers) {
                Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> runnable.run(), delay * 20L);
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, runnable, delay * 20L);
            }
        }
    }

    public static void scheduleNoDelay(JavaPlugin plugin, Runnable runnable, boolean async) {
        if (async) {
            if (modernSchedulers) {
                Bukkit.getAsyncScheduler().runNow(plugin, t -> runnable.run());
            } else {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            }
        } else {
            if (modernSchedulers) {
                Bukkit.getGlobalRegionScheduler().run(plugin, t -> runnable.run());
            } else {
                Bukkit.getScheduler().runTask(plugin, runnable);
            }
        }
    }
}
