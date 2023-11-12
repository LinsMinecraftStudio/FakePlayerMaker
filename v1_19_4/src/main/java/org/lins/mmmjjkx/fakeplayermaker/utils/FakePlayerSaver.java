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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

import static io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils.getCraftClass;
import static io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils.getHandle;

public class FakePlayerSaver extends SingleFileStorage {
    private final YamlConfiguration configuration;

    public FakePlayerSaver() {
        super(FakePlayerMaker.INSTANCE, new File(FakePlayerMaker.INSTANCE.getDataFolder(), "fakePlayers.yml"));
        configuration = getConfiguration();
    }

    public void reload(boolean removeAll) {
        super.reload(configuration);
        NMSFakePlayerMaker.reloadMap(removeAll, getFakePlayers());
    }

    public void syncPlayerInfo(ServerPlayer player) {
        Player bukkit = player.getBukkitEntity();
        ConfigurationSection section = getOrElseCreate(bukkit.getName());
        section.set("uuid", bukkit.getUniqueId().toString());
        section.set("location", ObjectConverter.toLocationString(bukkit.getLocation()));

        {

            PlayerProfile playerProfile = bukkit.getPlayerProfile();
            Optional<ProfileProperty> skin = ListUtil.getIf(playerProfile.getProperties(), p -> p.getName().equals("textures"));
            if (skin.isPresent()) {
                section.set("skin", skin.get().getValue());
                section.set("skin-signature", skin.get().getSignature());
            }
        }

        reload(false);
    }

    public void removeFakePlayer(String name) {
        configuration.set(name, null);
        reload(false);
    }

    public void removeAllFakePlayers() {
        for (String key : configuration.getKeys(false)) {
            configuration.set(key, null);
        }
        reload(true);
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
            reload(false);
        }
        return section;
    }
}
