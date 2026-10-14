package dev.portfolio.economy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** In-memory balances backed by balances.yml. */
public class BalanceManager {

    private final EconomyPlugin plugin;
    private final File file;
    private final Map<UUID, Double> balances = new HashMap<>();
    private boolean dirty = false;

    public BalanceManager(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "balances.yml");
    }

    public void load() {
        balances.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("balances");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                balances.put(UUID.fromString(key), section.getDouble(key));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Skipping invalid UUID in balances.yml: " + key);
            }
        }
    }

    public void save() {
        if (!dirty) return;
        YamlConfiguration yaml = new YamlConfiguration();
        balances.forEach((uuid, bal) -> yaml.set("balances." + uuid, bal));
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save balances.yml: " + e.getMessage());
        }
    }

    public double get(UUID uuid) {
        return balances.getOrDefault(uuid, plugin.getConfig().getDouble("starting-balance", 100.0));
    }

    public void set(UUID uuid, double amount) {
        balances.put(uuid, Math.max(0, round(amount)));
        dirty = true;
    }

    public void deposit(UUID uuid, double amount) {
        set(uuid, get(uuid) + amount);
    }

    public boolean has(UUID uuid, double amount) {
        return get(uuid) + 0.0001 >= amount;
    }

    /** @return true if the money was taken, false if the player could not afford it. */
    public boolean withdraw(UUID uuid, double amount) {
        if (!has(uuid, amount)) return false;
        set(uuid, get(uuid) - amount);
        return true;
    }

    public Map<UUID, Double> all() {
        return Map.copyOf(balances);
    }

    public static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
