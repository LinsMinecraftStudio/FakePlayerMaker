package org.lins.mmmjjkx.fakeplayermaker.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.github.linsminecraftstudio.polymer.objects.plugin.AbstractFileStorage;
import io.github.linsminecraftstudio.polymer.utils.ListUtil;
import io.github.linsminecraftstudio.polymer.utils.ObjectConverter;
import joptsimple.internal.Strings;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getCraftClass;
import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getHandle;

public class FakePlayerSaver extends AbstractFileStorage {
    private YamlConfiguration configuration;
    private final File cfgFile = new File(FakePlayerMaker.INSTANCE.getDataFolder(), "fakePlayers.yml");
    public FakePlayerSaver(){
        super(FakePlayerMaker.INSTANCE);
        configuration = handleConfig("fakePlayers.yml");
    }

    @Override
    public void reload() {
        configuration = handleConfig("fakePlayers.yml");
        NMSFakePlayerMaker.reloadMap(getFakePlayers());
    }

    public void syncPlayerInfo(ServerPlayer player) {
        ConfigurationSection section = getOrElseCreate(player.getName().getString());
        section.set("uuid", player.getUUID().toString());
        section.set("location", ObjectConverter.toLocationString(player.getBukkitEntity().getLocation()));
        Optional<Property> prop = ListUtil.getIf(player.gameProfile.getProperties().get("textures"), p -> p.getName().equals("textures"));
        prop.ifPresent(property -> section.set("skin", property.getValue()));
        try {configuration.save(cfgFile);
        } catch (IOException e) {throw new RuntimeException(e);}
    }

    public void removeFakePlayer(String name) {
        configuration.set(name, null);
        try {configuration.save(cfgFile);
        } catch (IOException e) {throw new RuntimeException(e);}
    }

    public List<ServerPlayer> getFakePlayers() {
        List<ServerPlayer> players = new ArrayList<>();
        for (String sectionName : configuration.getKeys(false)) {
            ConfigurationSection section = configuration.getConfigurationSection(sectionName);
            if (section == null) continue;
            UUID uuid = UUID.fromString(section.getString("uuid", String.valueOf(UUIDUtil.createOfflinePlayerUUID(sectionName))));
            Location location = ObjectConverter.toLocation(section.getString("location", ""));
            if (location == null) continue;
            String skin = null;
            if (section.contains("skin")) skin = section.getString("skin", "");
            ServerLevel level = (ServerLevel) getHandle(getCraftClass("CraftWorld"), location.getWorld());
            GameProfile profile = new GameProfile(uuid, sectionName);
            if (!Strings.isNullOrEmpty(skin)) {
                profile.getProperties().put("textures", new Property("textures", skin));
            }
            if (level == null) {
                FakePlayerMaker.INSTANCE.getLogger().log(Level.WARNING,
                        "Failed to create fake player for " + sectionName + ": world is null or the world not found");
                continue;
            }
            ServerPlayer player = new ServerPlayer(MinecraftServer.getServer(), level, profile);
            player.teleportTo(level, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            players.add(player);
        }
        return players;
    }

    @Nonnull
    private ConfigurationSection getOrElseCreate(String path){
        ConfigurationSection section = configuration.getConfigurationSection(path);
        if (section == null) {
            section = configuration.createSection(path);
            try {configuration.save(cfgFile);
            } catch (IOException e) {throw new RuntimeException(e);}
        }
        return section;
    }
}
