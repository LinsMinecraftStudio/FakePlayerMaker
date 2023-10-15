package org.lins.mmmjjkx.fakeplayermaker.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.github.linsminecraftstudio.fakeplayermaker.api.implementation.Implementations;
import io.github.linsminecraftstudio.polymer.objects.plugin.file.SingleFileStorage;
import io.github.linsminecraftstudio.polymer.utils.ListUtil;
import io.github.linsminecraftstudio.polymer.utils.ObjectConverter;
import joptsimple.internal.Strings;
import net.minecraft.core.UUIDUtil;
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
import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getHandle;

public class FakePlayerSaver extends SingleFileStorage {
    private YamlConfiguration configuration;
    public FakePlayerSaver(){
        super(FakePlayerMaker.INSTANCE, new File(FakePlayerMaker.INSTANCE.getDataFolder(), "fakePlayers.yml"));
        configuration = getConfiguration();
    }

    @Override
    public void reload() {
        configuration = getConfiguration();
        NMSFakePlayerMaker.reloadMap(getFakePlayers());
    }

    public void syncPlayerInfo(ServerPlayer player) {
        ConfigurationSection section = getOrElseCreate(Implementations.getName(player));
        section.set("uuid", Implementations.getUUID(player).toString());
        section.set("location", ObjectConverter.toLocationString(Implementations.bukkitEntity(player).getLocation()));

        {
            Player bukkit = Implementations.bukkitEntity(player);
            PlayerProfile playerProfile = bukkit.getPlayerProfile();
            Optional<ProfileProperty> skin = ListUtil.getIf(playerProfile.getProperties(), p -> p.getName().equals("textures"));
            if (skin.isPresent()) {
                section.set("skin", skin.get().getValue());
                section.set("skin-signature", skin.get().getSignature());
            }
        }

        reload();
    }

    public void removeFakePlayer(String name) {
        configuration.set(name, null);
        reload();
    }

    public Map<ServerPlayer, Location> getFakePlayers() {
        Map<ServerPlayer,Location> players = new HashMap<>();
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
            ServerPlayer player = Implementations.get().create(level, profile);
            players.put(player, location);
        }
        return players;
    }

    @Nonnull
    private ConfigurationSection getOrElseCreate(String path){
        ConfigurationSection section = configuration.getConfigurationSection(path);
        if (section == null) {
            section = configuration.createSection(path);
            reload();
        }
        return section;
    }
}
