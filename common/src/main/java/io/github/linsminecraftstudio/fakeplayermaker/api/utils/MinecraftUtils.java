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
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class MinecraftUtils {
    public static final Logger LOGGER = LogManager.getLogManager().getLogger("FakePlayerMaker");

    private MinecraftUtils() {
    }

    public static ServerGamePacketListenerImpl getGamePacketListener(Connection connection, ServerPlayer player) {
        int code = Integer.parseInt(Bukkit.getMinecraftVersion().replaceAll("\\.", ""));

        //who needs 1.20?
        if (code < 1202) {
            final MethodDelegation delegation = MethodDelegation.to(new PacketListenerDelegation());

            try (DynamicType.Unloaded<ServerGamePacketListenerImpl> unloaded = new ByteBuddy()
                        .subclass(ServerGamePacketListenerImpl.class)
                        .name(MinecraftUtils.class.getPackage().getName() + ".FPMGamePacketListenerV1201")

                        .defineField("pl", ServerPlayer.class, Visibility.PRIVATE)
                        .constructor(ElementMatchers.any())

                    .intercept(MethodCall.invoke(
                                            ServerGamePacketListenerImpl.class.getDeclaredConstructor(MinecraftServer.class, Connection.class, ServerPlayer.class)
                                    )
                                .withArgument(0, 1, 2)
                                .andThen(FieldAccessor.ofField("pl").setsArgumentAt(2))
                        )

                        .method((ElementMatchers.named("a").or(ElementMatchers.named("internalTeleport"))))
                        .intercept(delegation)

                    .make()) {

                Constructor<? extends ServerGamePacketListenerImpl> constructor = unloaded.load(MinecraftUtils.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
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

    public static void scheduleNoDelay(PolymerPlugin plugin, Runnable runnable, boolean async) {
        BFScheduler scheduler = plugin.getScheduler();
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
        Object lpBukkitPlugin = getLuckPermsInstance();
        if (lpBukkitPlugin != null) {
            if (!LuckPermsProvider.get().getUserManager().isLoaded(p.getUniqueId())) {
                try {
                    //LuckPerms using different class loader
                    ClassLoader loader = lpBukkitPlugin.getClass().getClassLoader();

                    Object lpBootStrap = lpBukkitPlugin.getClass().getMethod("getBootstrap").invoke(lpBukkitPlugin);
                    Object storage = lpBukkitPlugin.getClass().getMethod("getStorage").invoke(lpBukkitPlugin);
                    Class<?> userClass = loader.loadClass("me.lucko.luckperms.common.model.User");
                    Method method = storage.getClass().getDeclaredMethod("saveUser", userClass);

                    //saveUser
                    Object user = userClass.getDeclaredConstructors()[0].newInstance(p.getUniqueId(), lpBukkitPlugin);
                    method.invoke(storage, user);

                    Field humanEntityPermissibleField = getCraftClass("entity.CraftHumanEntity").getDeclaredField("perm");
                    humanEntityPermissibleField.setAccessible(true);
                    Object currentPermissible = humanEntityPermissibleField.get(p);
                    Class<?> LuckPermsPermissible = loader.loadClass("me.lucko.luckperms.bukkit.inject.permissible.LuckPermsPermissible");

                    if (currentPermissible.getClass().equals(LuckPermsPermissible)) return;

                    Object permissible = LuckPermsPermissible.getDeclaredConstructors()[0].newInstance(p, user, lpBukkitPlugin);
                    Object pluginLogger = lpBootStrap.getClass().getMethod("getPluginLogger").invoke(lpBootStrap);

                    Class<?> PermissibleInjector = loader.loadClass("me.lucko.luckperms.bukkit.inject.permissible.PermissibleInjector");
                    Class<?> PluginLogger = loader.loadClass("me.lucko.luckperms.common.plugin.logging.PluginLogger");

                    PermissibleInjector.getMethod("inject", Player.class, LuckPermsPermissible, PluginLogger).invoke(null, p, permissible, pluginLogger);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static Object getLuckPermsInstance() {
        try {
            Plugin loaderPlugin = Bukkit.getPluginManager().getPlugin("LuckPerms");
            if (loaderPlugin == null) return null;
            Field field = loaderPlugin.getClass().getDeclaredField("plugin");
            field.setAccessible(true);
            Object bootstrap = field.get(loaderPlugin);
            LOGGER.warning(bootstrap.getClass().getName());
            Field plugin = bootstrap.getClass().getDeclaredField("plugin");
            plugin.setAccessible(true);
            return plugin.get(bootstrap);
        } catch (Exception e) {
            return null;
        }
    }
}
