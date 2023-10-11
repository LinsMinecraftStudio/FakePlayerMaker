package org.lins.mmmjjkx.fakeplayermaker.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.github.linsminecraftstudio.polymer.objects.plugin.file.SingleFileStorage;
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
import org.bukkit.entity.Player;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

import static io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils.getHandle;
import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getCraftClass;

public class FakePlayerSaver extends SingleFileStorage {
    private YamlConfiguration configuration;
    private final File cfgFile = new File(FakePlayerMaker.INSTANCE.getDataFolder(), "fakePlayers.yml");

    public FakePlayerSaver() {
        super(FakePlayerMaker.INSTANCE);
        configuration = handleConfig("fakePlayers.yml");
    }

    @Override
    public void reload() {
        configuration = handleConfig("fakePlayers.yml");
        NMSFakePlayerMaker.reloadMap(getFakePlayers());
    }

    public void syncPlayerInfo(ServerPlayer player) {
        Player bukkit = player.getBukkitEntity();
        ConfigurationSection section = getOrElseCreate(bukkit.getName());
        section.set("uuid", bukkit.getUniqueId());
        section.set("location", ObjectConverter.toLocationString(bukkit.getLocation()));

        {

            PlayerProfile playerProfile = bukkit.getPlayerProfile();
            Optional<ProfileProperty> skin = ListUtil.getIf(playerProfile.getProperties(), p -> p.getName().equals("textures"));
            if (skin.isPresent()) {
                section.set("skin", skin.get().getValue());
                section.set("skin-signature", skin.get().getSignature());
            }
        }

        try {
            configuration.save(cfgFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeFakePlayer(String name) {
        configuration.set(name, null);
        try {
            configuration.save(cfgFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<ServerPlayer, Location> getFakePlayers() {
        Map<ServerPlayer, Location> players = new HashMap<>();
        for (String sectionName : configuration.getKeys(false)) {
            ConfigurationSection section = configuration.getConfigurationSection(sectionName);
            if (section == null) continue;
            UUID uuid = UUID.fromString(section.getString("uuid", String.valueOf(UUIDUtil.createOfflinePlayerUUID(sectionName))));
            Location location = ObjectConverter.toLocation(section.getString("location", ""));
            if (location == null) continue;
            String skin = null, signature = null;
            if (section.contains("skin")) {
                skin = section.getString("skin", "");
                signature = section.getString("skin-signature", "");
            }
            ServerLevel level = (ServerLevel) getHandle(getCraftClass("CraftWorld"), location.getWorld());
            GameProfile profile = new GameProfile(uuid, sectionName);
            if (!Strings.isNullOrEmpty(skin) && !Strings.isNullOrEmpty(signature)) {
                profile.getProperties().put("textures", new Property("textures", skin, signature));
            }
            if (level == null) {
                FakePlayerMaker.INSTANCE.getLogger().log(Level.WARNING,
                        "Failed to create fake player for " + sectionName + ": world is null or the world not found");
                continue;
            }
            ServerPlayer player = new ServerPlayer(MinecraftServer.getServer(), level, profile);
            players.put(player, location);
        }
        return players;
    }

    @Nonnull
    private ConfigurationSection getOrElseCreate(String path) {
        ConfigurationSection section = configuration.getConfigurationSection(path);
        if (section == null) {
            section = configuration.createSection(path);
            try {
                configuration.save(cfgFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return section;
    }
}
