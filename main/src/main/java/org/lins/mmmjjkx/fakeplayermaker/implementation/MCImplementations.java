package org.lins.mmmjjkx.fakeplayermaker.implementation;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.NotNull;

public final class MCImplementations {
    public static void setup() {
        Implementations.register(new v1_19_3());
        Implementations.register(new v1_20_1());
    }
}

class v1_19_3 extends Implementations {
    @Override
    public @NotNull GameProfile profile(ServerPlayer player) {
        try {
            return (GameProfile) player.getClass().getField("co").get(player);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setConnection(ServerPlayer player, ServerGamePacketListenerImpl connection) {
        try {
            player.getClass().getField("b").set(player, connection);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String[] minecraftVersion() {
        return new String[]{"1.19.3","1.19.4"};
    }
}

class v1_20_1 extends Implementations {
    @Override
    public @NotNull GameProfile profile(ServerPlayer player) {
        return player.getGameProfile();
    }

    @Override
    public void setConnection(ServerPlayer player, ServerGamePacketListenerImpl connection) {
        player.connection = connection;
    }

    @Override
    public String[] minecraftVersion() {
        return new String[]{"1.20.1"};
    }
}
