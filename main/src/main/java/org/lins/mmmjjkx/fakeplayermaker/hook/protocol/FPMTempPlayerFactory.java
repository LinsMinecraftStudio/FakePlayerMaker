package org.lins.mmmjjkx.fakeplayermaker.hook.protocol;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.injector.temporary.MinimalInjector;
import com.comphenix.protocol.injector.temporary.TemporaryPlayer;
import com.comphenix.protocol.injector.temporary.TemporaryPlayerFactory;
import com.comphenix.protocol.utility.ChatExtensions;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.nullability.AlwaysNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getCraftClass;
import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getHandle;

public class FPMTempPlayerFactory {
    private static final Constructor<? extends Player> CONSTRUCTOR = getConstructor();

    private static Constructor<? extends Player> getConstructor() {
        final MethodDelegation implementation = MethodDelegation.to(new Object() {
            @RuntimeType
            public Object delegate(
                    @This Object obj,
                    @Origin Method method,
                    @FieldValue("server") Server server,
                    @FieldValue("name") String name,
                    @AllArguments Object... args
            ) throws Throwable {
                String methodName = method.getName();
                Player player = (Player) obj;
                MinimalInjector injector = TemporaryPlayerFactory.getInjectorFromPlayer(player);

                if (injector == null) {
                    throw new IllegalStateException("Unable to find injector.");
                }

                // Use the socket to get the address
                else if (methodName.equals("getPlayer")) {
                    return injector.getPlayer();
                } else if (methodName.equals("getAddress")) {
                    return injector.getAddress();
                } else if (methodName.equals("getServer")) {
                    return server;
                }

                // Handle send message methods
                if (methodName.equals("chat") || methodName.equals("sendMessage")) {
                    try {
                        Object argument = args[0];

                        // Dynamic overloading
                        if (argument instanceof String) {
                            return sendMessage(injector, (String) argument);
                        } else if (argument instanceof String[]) {
                            for (String message : (String[]) argument) {
                                sendMessage(injector, message);
                            }
                            return null;
                        }
                    } catch (Exception exception) {
                        throw exception.getCause();
                    }
                }

                // Also, handle kicking
                if (methodName.equals("kickPlayer")) {
                    injector.disconnect((String) args[0]);
                    return null;
                }
                
                if (methodName.equals("kick")) {
                    Component paper = (Component) args[0]; 
                    injector.disconnect(LegacyComponentSerializer.legacyAmpersand().serialize(paper));
                    return null;
                }

                Player updated = injector.getPlayer();
                if (updated != obj && updated != null) {
                    return method.invoke(updated, args);
                }

                // Methods that are supported in the fallback instance
                switch (methodName) {
                    case "isOnline" -> {
                        return injector.isConnected();
                    }
                    case "getName" -> {
                        return name;
                    }
                    case "getUniqueId" -> {
                        return UUIDUtil.createOfflinePlayerUUID(name);
                    }
                }

                if (methodName.equals("isSneaking")) {
                    return getNMSPlayer(player).isShiftKeyDown();
                }

                if (methodName.equals("setSneaking")) {
                    getNMSPlayer(player).setShiftKeyDown((boolean) args[0]);
                    return null;
                }

                if (methodName.equals("isSprinting")) {
                    return getNMSPlayer(player).isSprinting();
                }

                if (methodName.equals("setSprinting")) {
                    getNMSPlayer(player).setSprinting((boolean) args[0]);
                    return null;
                }

                if (methodName.equals("isSwimming")) {
                    return getNMSPlayer(player).isSwimming();
                }

                if (methodName.equals("setSwimming")) {
                    getNMSPlayer(player).setSwimming((boolean) args[0]);
                    return null;
                }

                // Ignore all other methods
                throw new UnsupportedOperationException(
                        "The method " + method.getName() + " is not supported for temporary players(From FakePlayerMaker).");
            }
        });

        final ElementMatcher.Junction<ByteCodeElement> callbackFilter = ElementMatchers.not(
                ElementMatchers.isDeclaredBy(Object.class).or(ElementMatchers.isDeclaredBy(TemporaryPlayer.class)));

        try {
            final Constructor<?> constructor = new ByteBuddy()
                    .subclass(TemporaryPlayer.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                    .name(FPMTempPlayerFactory.class.getPackage().getName() + ".FPMTemporaryPlayerInvocationHandler")
                    .implement(Player.class)

                    .defineField("server", Server.class, Visibility.PRIVATE)
                    .defineField("name", String.class, Visibility.PRIVATE)
                    .defineConstructor(Visibility.PUBLIC)
                    .withParameters(Server.class, String.class)

                    .intercept(MethodCall.invoke(TemporaryPlayer.class.getDeclaredConstructor())
                            .andThen(FieldAccessor.ofField("server").setsArgumentAt(0))
                            .andThen(FieldAccessor.ofField("name").setsArgumentAt(1)))

                    .method(callbackFilter)
                    .intercept(implementation)
                    .make()
                    .load(FPMTempPlayerFactory.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded()
                    .getDeclaredConstructor(Server.class, String.class);
            return (Constructor<? extends Player>) constructor;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to find Temporary Player constructor!", e);
        }
    }

    @AlwaysNull
    private static Object sendMessage(MinimalInjector injector, String message) {
        for (PacketContainer packet : ChatExtensions.createChatPackets(message)) {
            injector.sendServerPacket(packet.getHandle(), null, false);
        }

        return null;
    }

    private static ServerPlayer getNMSPlayer(Player bukkit) {
        return (ServerPlayer) getHandle(getCraftClass("entity.CraftPlayer"), bukkit);
    }

    /**
    Supported methods:
    <ul>
     <li>{@link Player#getServer()}</li>
     <li>{@link Player#sendMessage(String)}</li>
     <li>{@link Player#isSneaking()}</li>
     <li>{@link Player#setSneaking(boolean)}</li>
     <li>{@link Player#isSprinting()}</li>
     <li>{@link Player#setSprinting(boolean)}</li>
     <li>{@link Player#isSwimming()}</li>
     <li>{@link Player#setSwimming(boolean)}</li>
     <li>{@link Player#kickPlayer(String)}</li>
     <li>{@link Player#kick(Component)}</li>
     <li>{@link Player#chat(String)}</li>
     <li>{@link Player#sendMessage(String[])}</li>
     <li>{@link Player#getAddress()}</li>
     <li>{@link Player#getUniqueId()}</li>
     </ul>
     */
    public static Player createPlayer(final Server server, String name) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Player p = CONSTRUCTOR.newInstance(server, name);
        TemporaryPlayerFactory.setInjectorInPlayer(p, new FPMMinimalInjector(name));
        return p;
    }
}
