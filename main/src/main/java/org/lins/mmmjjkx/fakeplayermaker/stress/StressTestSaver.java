package org.lins.mmmjjkx.fakeplayermaker.stress;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import io.github.linsminecraftstudio.polymer.objects.plugin.AbstractFeatureManager;
import io.github.linsminecraftstudio.polymer.utils.ObjectConverter;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StressTestSaver extends AbstractFeatureManager {
    private YamlConfiguration configuration;
    private final Map<String, AreaStressTester> areaTesterMap = new HashMap<>();
    private final Map<String, RandomWorldStressTester> randomWorldStressTesterMap = new HashMap<>();
    public StressTestSaver() {
        super(FakePlayerMaker.INSTANCE);
        configuration = handleConfig("stresses.yml");
        loadStressTesters();
    }

    private void loadStressTesters(){
        Logger logger = FakePlayerMaker.INSTANCE.getLogger();
        for (String key : configuration.getKeys(false)) {
            ConfigurationSection section = configuration.getConfigurationSection(key);
            if (section == null) {
                logger.log(Level.WARNING,"""
                        Failed to load stress tester {} from configuration,
                        because it isn't present.
                        """, key);
                continue;
            }
            String type = section.getString("type","").toLowerCase();

            int amount = section.getInt("amount");
            if (amount == 0) {
                logger.log(Level.WARNING, """
                        Stress tester {} hasn't set the amount,
                        default set to 100.
                        """, key);
                amount = 100;
            }

            if (type.equals("area")) {
                Location loc1 = ObjectConverter.toLocation(section.getString("pos1", ""));
                Location loc2 = ObjectConverter.toLocation(section.getString("pos2", ""));
                if (loc1 == null || loc2 == null) {
                    logger.log(Level.WARNING, """
                            Failed to load stress tester {} from configuration,
                            because it has no starting and ending locations.
                            """, key);
                    continue;
                }
                areaTesterMap.put(key, new AreaStressTester(new CuboidRegion(
                        BukkitAdapter.adapt(loc1.getWorld()),
                        BukkitAdapter.asBlockVector(loc1), BukkitAdapter.asBlockVector(loc2)
                ), amount));
            } else if (type.equals("randomworld")) {
                boolean b = section.getBoolean("amountPerWorld", false);
                randomWorldStressTesterMap.put(key, new RandomWorldStressTester(b, amount));
            } else {
                logger.log(Level.WARNING, """
                        Failed to load stress tester {} from configuration,
                        because it has no type set or the type is invalid.
                        """, key);
            }
        }
    }

    public Optional<AreaStressTester> getStressTesterArea(String name) {
        return Optional.ofNullable(areaTesterMap.get(name));
    }

    public Optional<RandomWorldStressTester> getStressTesterRandomWorld(String name){
        return Optional.ofNullable(randomWorldStressTesterMap.get(name));
    }

    @SuppressWarnings("unused")
    public void newRandomWorldTester(String name) {
        newRandomWorldTester(name, 100);
    }

    public void newRandomWorldTester(String name, int amount) {
        newRandomWorldTester(name, amount, false);
    }

    public void newRandomWorldTester(String name, int amount, boolean isAmountPerWorld) {
        ConfigurationSection section = configuration.createSection(name);
        section.set("type", "randomworld");
        section.set("amountPerWorld", isAmountPerWorld);
        section.set("amount",amount);
        randomWorldStressTesterMap.put(name,new RandomWorldStressTester(false, amount));
        try {configuration.save(new File(FakePlayerMaker.INSTANCE.getDataFolder(), "stresses.yml"));
        } catch (IOException e) {throw new RuntimeException(e);}
    }

    @SuppressWarnings("unused")
    public void newAreaTester(String name, Location pos1, Location pos2){
        newAreaTester(name, pos1, pos2, 100);
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
        randomWorldStressTesterMap.values().forEach(RandomWorldStressTester::stop);
        areaTesterMap.clear();
        randomWorldStressTesterMap.clear();
    }

    @Override
    public void reload() {
        configuration = handleConfig("stresses.yml");
    }
}
