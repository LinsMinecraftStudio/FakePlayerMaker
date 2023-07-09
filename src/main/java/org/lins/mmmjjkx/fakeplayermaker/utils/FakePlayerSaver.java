package org.lins.mmmjjkx.fakeplayermaker.utils;

import com.mojang.authlib.GameProfile;
import io.github.linsminecraftstudio.polymer.objects.plugin.AbstractFeatureManager;
import io.github.linsminecraftstudio.polymer.utils.ListUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FakePlayerSaver extends AbstractFeatureManager {
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
        section.set("skin", ListUtil.getIf(player.gameProfile.getProperties().get("textures"),
                p -> p.getName().equals("textures")));
        try {configuration.save(cfgFile);
        } catch (IOException e) {throw new RuntimeException(e);}
    }

    public void removeFakePlayer(String name) {
        configuration.set(name, null);
    }
    public List<ServerPlayer> getFakePlayers() {
        List<ServerPlayer> players = new ArrayList<>();
        for (String sectionName : configuration.getKeys(false)) {
            ConfigurationSection section = configuration.getConfigurationSection(sectionName);
            if (section == null) continue;
            UUID uuid = UUID.fromString(section.getString("uuid", Bukkit.getOfflinePlayer(sectionName).getUniqueId().toString()));
            ServerLevel level = (ServerLevel) Bukkit.getWorlds().get(0);
            ServerPlayer player = new ServerPlayer(MinecraftServer.getServer(), level, new GameProfile(uuid, sectionName));
            players.add(player);
        }
        return players;
    }

    @Nonnull
    private ConfigurationSection getOrElseCreate(String path){
        ConfigurationSection section = configuration.getConfigurationSection(path);
        if (section == null) {
            section = configuration.createSection(path);
        }
        return section;
    }
}
