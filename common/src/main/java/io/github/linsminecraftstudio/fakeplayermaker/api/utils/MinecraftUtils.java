package io.github.linsminecraftstudio.fakeplayermaker.api.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.linsminecraftstudio.fakeplayermaker.api.implementation.Implementations;
import io.github.linsminecraftstudio.fakeplayermaker.api.objects.CraftBukkitClassNotFoundError;
import io.github.linsminecraftstudio.fakeplayermaker.api.objects.FPMPacketListener;
import io.github.linsminecraftstudio.polymer.objects.plugin.PolymerPlugin;
import io.github.linsminecraftstudio.polymer.schedule.BFScheduler;
import me.lucko.luckperms.bukkit.loader.BukkitLoaderPlugin;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class MinecraftUtils {
    public static final Logger LOGGER = LogManager.getLogManager().getLogger("FakePlayerMaker");

    private MinecraftUtils() {
    }

    public static ServerGamePacketListenerImpl getGamePacketListener(Connection connection, ServerPlayer player) {
        if (!Bukkit.getMinecraftVersion().equals("1.20.2")) {
            try {
                final MethodDelegation delegation = MethodDelegation.to(new PacketListenerDelegation());

                Constructor<ServerGamePacketListenerImpl> connectionConstructor = ServerGamePacketListenerImpl.class.getDeclaredConstructor(MinecraftServer.class, Connection.class, ServerPlayer.class);

                Constructor<? extends ServerGamePacketListenerImpl> constructor = new ByteBuddy()
                        .subclass(ServerGamePacketListenerImpl.class)
                        .name(MinecraftUtils.class.getPackage().getName() + ".FPMGamePacketListenerV1201")

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

    public static void schedule(PolymerPlugin plugin, Runnable runnable, long delay, boolean async) {
        BFScheduler scheduler = new BFScheduler(plugin);
        if (async) {
            scheduler.scheduleDelayAsync(runnable, delay, delay / 20L);
        } else {
            scheduler.scheduleDelay(runnable, delay);
        }
    }

    public static void scheduleNoDelay(PolymerPlugin plugin, Runnable runnable, boolean async) {
        BFScheduler scheduler = new BFScheduler(plugin);
        if (async) {
            scheduler.scheduleAsync(runnable);
        } else {
            scheduler.schedule(runnable);
        }
    }

    public static void skinChange(ServerPlayer player, String targetName) throws IOException {
        Player bukkit = Implementations.bukkitEntity(player);
        PlayerProfile playerProfile = bukkit.getPlayerProfile();
        URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + targetName);
        InputStreamReader reader = new InputStreamReader(url.openStream());
        String uuid = JsonParser.parseReader(reader).getAsJsonObject().get("id").getAsString();
        URL url1 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
        InputStreamReader reader1 = new InputStreamReader(url1.openStream());
        JsonObject properties = JsonParser.parseReader(reader1).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();

        String value = properties.get("value").getAsString();
        String signature = properties.get("signature").getAsString();

        playerProfile.setProperty(new ProfileProperty("textures", value, signature));
        bukkit.setPlayerProfile(playerProfile);
    }

    public static void handlePlugins(Player p) {
        //inject permissible base and load User
        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            if (!LuckPermsProvider.get().getUserManager().isLoaded(p.getUniqueId())) {
                try {
                    Object lpBukkitPlugin = getLuckPermsInstance();
                    Object storage = lpBukkitPlugin.getClass().getMethod("getStorage").invoke(lpBukkitPlugin);
                    Method method = storage.getClass().getDeclaredMethod("savePlayerData", UUID.class, String.class);
                    Method method1 = storage.getClass().getDeclaredMethod("loadUser", UUID.class, String.class);

                    //savePlayerData
                    CompletableFuture<?> future = (CompletableFuture<?>) method.invoke(storage, p.getUniqueId(), p.getName());
                    future.get();

                    //loadUser
                    CompletableFuture<?> future1 = (CompletableFuture<?>) method1.invoke(storage, p.getUniqueId(), p.getName());

                    //common model User
                    Object user = future1.get();

                    //LuckPerms using different class loader
                    ClassLoader loader = lpBukkitPlugin.getClass().getClassLoader();
                    Class<?> LuckPermsPermissible = loader.loadClass("me.lucko.luckperms.bukkit.inject.permissible.LuckPermsPermissible");
                    Object permissible = LuckPermsPermissible.getDeclaredConstructors()[0].newInstance(p, user, lpBukkitPlugin);

                    Class<?> craftPlayer = getCraftClass("entity.CraftHumanEntity");
                    Field permissibleField = craftPlayer.getDeclaredField("perm");
                    permissibleField.setAccessible(true);
                    permissibleField.set(p, permissible);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static Object getLuckPermsInstance() {
        try {
            Field field = BukkitLoaderPlugin.class.getDeclaredField("plugin");
            field.setAccessible(true);
            BukkitLoaderPlugin loaderPlugin = JavaPlugin.getPlugin(BukkitLoaderPlugin.class);
            Object bootstrap = field.get(loaderPlugin);
            LOGGER.warning(bootstrap.getClass().getName());
            Field plugin = bootstrap.getClass().getDeclaredField("plugin");
            plugin.setAccessible(true);
            return plugin.get(bootstrap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
