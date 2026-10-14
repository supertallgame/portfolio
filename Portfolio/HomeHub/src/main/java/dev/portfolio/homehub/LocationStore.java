package dev.portfolio.homehub;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Tiny YAML-backed store of named locations grouped into sections.
 * Used twice: homes.yml (section = player UUID) and warps.yml (section = "warps").
 */
public class LocationStore {

    private final HomeHubPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public LocationStore(HomeHubPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public Set<String> names(String section) {
        ConfigurationSection s = yaml.getConfigurationSection(section);
        return s == null ? Set.of() : s.getKeys(false);
    }

    public Location get(String section, String name) {
        return yaml.getLocation(section + "." + name);
    }

    public boolean exists(String section, String name) {
        return yaml.contains(section + "." + name);
    }

    public void set(String section, String name, Location loc) {
        yaml.set(section + "." + name, loc);
        save();
    }

    public void delete(String section, String name) {
        yaml.set(section + "." + name, null);
        save();
    }

    private void save() {
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }
}
