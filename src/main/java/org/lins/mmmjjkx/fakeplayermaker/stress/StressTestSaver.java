package org.lins.mmmjjkx.fakeplayermaker.stress;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import io.github.linsminecraftstudio.polymer.objects.plugin.AbstractFeatureManager;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.utils.ObjectConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StressTestSaver extends AbstractFeatureManager {
    private YamlConfiguration configuration;
    private final Map<String, AreaStressTester> testerMap = new HashMap<>();
    public StressTestSaver() {
        super(FakePlayerMaker.INSTANCE);
        configuration = handleConfig("stresses.yml");
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
            Location loc1 = ObjectConverter.toLocation(section.getString("pos1",""));
            Location loc2 = ObjectConverter.toLocation(section.getString("pos2",""));
            if (loc1 == null || loc2 == null) {
                logger.log(Level.WARNING, """
                        Failed to load stress tester {} from configuration,
                        because it has no starting and ending locations.
                        """);
                continue;
            }
            int amount = section.getInt("amount");
            if (amount == 0) {
                logger.log(Level.WARNING, """
                        Stress tester {} hasn't set the amount,
                        default set to 100.
                        """);
                amount = 100;
            }
            testerMap.put(key, new AreaStressTester(new CuboidRegion(
                    BukkitAdapter.adapt(loc1.getWorld()),
                    BukkitAdapter.asBlockVector(loc1), BukkitAdapter.asBlockVector(loc2)
            ), amount));
        }
    }

    public Optional<AreaStressTester> getStressTester(String name) {
        return Optional.ofNullable(testerMap.get(name));
    }

    @Override
    public void reload() {
        configuration = handleConfig("stresses.yml");
    }
}
