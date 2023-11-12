package org.lins.mmmjjkx.fakeplayermaker.stress;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import io.github.linsminecraftstudio.polymer.objects.plugin.file.SingleFileStorage;
import io.github.linsminecraftstudio.polymer.utils.ObjectConverter;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StressTestSaver extends SingleFileStorage {
    private final YamlConfiguration configuration;
    private final Map<String, AreaStressTester> areaTesterMap = new HashMap<>();
    private final Map<String, RandomWorldStressTester> randomWorldStressTesterMap = new HashMap<>();

    public StressTestSaver() {
        super(FakePlayerMaker.INSTANCE, new File(FakePlayerMaker.INSTANCE.getDataFolder(), "stresses.yml"));
        configuration = getConfiguration();
        loadStressTesters();
    }

    private void loadStressTesters() {
        Logger logger = FakePlayerMaker.INSTANCE.getLogger();
        for (String key : configuration.getKeys(false)) {
            ConfigurationSection section = configuration.getConfigurationSection(key);
            if (section == null) {
                logger.log(Level.WARNING, """
                        Failed to load stress tester {} from configuration,
                        because it isn't present.
                        """.replace("{}", key));
                continue;
            }
            String type = section.getString("type", "").toLowerCase();

            int amount = section.getInt("amount");
            if (amount <= 1) {
                logger.log(Level.WARNING, "Stress tester " + key + " hasn't set the amount or that's too low, default set to 100.");
                amount = 100;
            }

            if (type.equals("area")) {
                if (!FakePlayerMaker.settings.getBoolean("areaStressTesters")) {
                    continue;
                }
                Location loc1 = ObjectConverter.toLocation(section.getString("pos1", ""));
                Location loc2 = ObjectConverter.toLocation(section.getString("pos2", ""));
                if (loc1 == null || loc2 == null) {
                    logger.log(Level.WARNING, """
                            Failed to load stress tester {} from configuration,
                            because it has no starting or ending locations.
                            """.replace("{}", key));
                    continue;
                }
                areaTesterMap.put(key, new AreaStressTester(new CuboidRegion(
                        BukkitAdapter.adapt(loc1.getWorld()),
                        BukkitAdapter.asBlockVector(loc1), BukkitAdapter.asBlockVector(loc2)
                ), amount));
            } else if (type.equals("randomworld")) {
                boolean b = section.getBoolean("amountPerWorld", false);
                randomWorldStressTesterMap.put(key, new RandomWorldStressTester(b, amount, section.getStringList("ignoreWorlds")));
            } else {
                logger.log(Level.WARNING, """
                        Failed to load stress tester {} from configuration,
                        because it has no type set or the type is invalid.
                        """.replace("{}",key));
            }
        }
    }

    public Optional<AreaStressTester> getStressTesterArea(String name) {
        return Optional.ofNullable(areaTesterMap.get(name));
    }

    public Optional<RandomWorldStressTester> getStressTesterRandomWorld(String name){
        return Optional.ofNullable(randomWorldStressTesterMap.get(name));
    }

    public void newRandomWorldTester(String name, int amount) {
        newRandomWorldTester(name, amount, false);
    }

    public void newRandomWorldTester(String name, int amount, boolean isAmountPerWorld) {
        newRandomWorldTester(name, amount, isAmountPerWorld, new ArrayList<>());
    }

    public void newRandomWorldTester(String name, int amount, boolean isAmountPerWorld, List<String> ignoreWorlds) {
        ConfigurationSection section = configuration.createSection(name);
        section.set("type", "randomworld");
        section.set("amountPerWorld", isAmountPerWorld);
        section.set("amount",amount);
        section.set("ignoreWorlds", ignoreWorlds);
        randomWorldStressTesterMap.put(name,new RandomWorldStressTester(false, amount, ignoreWorlds));
        try {configuration.save(new File(FakePlayerMaker.INSTANCE.getDataFolder(), "stresses.yml"));
        } catch (IOException e) {throw new RuntimeException(e);}
    }

    public void newAreaTester(String name, Location pos1, Location pos2, int amount){
        ConfigurationSection section = configuration.createSection(name);
        section.set("type", "area");
        section.set("pos1",ObjectConverter.toLocationString(pos1));
        section.set("pos2",ObjectConverter.toLocationString(pos2));
        section.set("amount",amount);
        areaTesterMap.put(name,new AreaStressTester(new CuboidRegion(BukkitAdapter.adapt(pos1.getWorld()),BukkitAdapter.asBlockVector(pos1),BukkitAdapter.asBlockVector(pos2)), amount));
        try {configuration.save(new File(FakePlayerMaker.INSTANCE.getDataFolder(), "stresses.yml"));
        } catch (IOException e) {throw new RuntimeException(e);}
    }

    public void stopAll(){
        areaTesterMap.values().forEach(AreaStressTester::stop);
        areaTesterMap.clear();
        randomWorldStressTesterMap.values().forEach(RandomWorldStressTester::stop);
        randomWorldStressTesterMap.clear();
    }

    public Set<String> getRWTesterNames() {
        return randomWorldStressTesterMap.keySet();
    }

    public Set<String> getAreaTesterNames() {
        return areaTesterMap.keySet();
    }

    public void reload() {
        stopAll();
        super.reload(configuration);
        loadStressTesters();
    }
}
