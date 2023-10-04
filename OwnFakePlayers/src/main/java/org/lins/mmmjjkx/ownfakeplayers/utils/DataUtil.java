package org.lins.mmmjjkx.ownfakeplayers.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.common.io.Files;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.github.linsminecraftstudio.polymer.utils.ListUtil;
import io.github.linsminecraftstudio.polymer.utils.ObjectConverter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.implementation.Implementations;
import org.lins.mmmjjkx.ownfakeplayers.objects.OwnableFakePlayer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getCraftClass;
import static org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker.getHandle;

public class DataUtil {
    private final File folder;
    private final Map<UUID, OwnableFakePlayer> map = new HashMap<>(); //fake player uuid
    public DataUtil(File folder) {
        this.folder = folder;
        load();
    }

    public void load() {
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            UUID owner = UUID.fromString(Files.getNameWithoutExtension(file.getName()));
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
            for (String uuid : configuration.getKeys(false)) {
                UUID uuid1 = UUID.fromString(uuid);
                ConfigurationSection section = configuration.getConfigurationSection(uuid);
                if (section == null) continue;
                String name = section.getString("name");
                Location location = ObjectConverter.toLocation(section.getString("location"));
                GameProfile profile = new GameProfile(uuid1, name);
                if (section.contains("skin") && section.contains("skin-signature")) {
                    profile.getProperties().put("textures", new Property("textures",
                            section.getString("skin",""), section.getString("skin-signature", "")));
                }
                map.put(uuid1, new OwnableFakePlayer(owner, new ServerPlayer(FakePlayerMaker.getNMSServer(), getLevel(location), profile)));
            }
        }
    }

    public void add(OwnableFakePlayer player) {
        File file = getOrCreate(player.owner().toString());
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ServerPlayer nms = player.player();
        String name = nms.getName().getString();
        Player bukkit = Implementations.bukkitEntity(nms);
        PlayerProfile playerProfile = bukkit.getPlayerProfile();

        ConfigurationSection section = configuration.createSection(bukkit.getUniqueId().toString());
        section.set("name", name);
        section.set("location", ObjectConverter.toLocationString(bukkit.getLocation()));

        {
            Optional<ProfileProperty> profileProperty = ListUtil.getIf(playerProfile.getProperties(), p -> p.getName().equals("textures"));
            if (profileProperty.isPresent()) {
                section.set("skin", profileProperty.get().getValue());
                section.set("skin-signature", profileProperty.get().getSignature());
            }
        }

        try {
            configuration.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        map.put(bukkit.getUniqueId(), player);
    }

    private File getOrCreate(String uuid) {
        File file = new File(folder, uuid+".yml");
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return file;
    }

    public void remove(UUID owner, UUID uuid) {
        File file = getOrCreate(owner.toString());
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        if (configuration.contains(uuid.toString())) {
            configuration.set(uuid.toString(), null);
            map.remove(uuid);
            try {
                configuration.save(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private ServerLevel getLevel(Location l) {
        return (ServerLevel) getHandle(getCraftClass("CraftWorld"), l.getWorld());
    }

    Map<UUID, OwnableFakePlayer> getMap() {
        return map;
    }
}
