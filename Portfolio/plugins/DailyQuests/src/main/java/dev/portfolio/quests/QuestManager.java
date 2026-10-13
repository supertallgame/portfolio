package dev.portfolio.quests;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuestManager {

    private final QuestsPlugin plugin;
    private final File file;
    private YamlConfiguration data;
    private final Map<String, QuestDef> definitions = new LinkedHashMap<>();
    private final Map<UUID, PlayerQuests> cache = new HashMap<>();

    public QuestManager(QuestsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    // ---------- loading / saving ----------

    public void loadDefinitions() {
        definitions.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("quests");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            try {
                QuestType type = QuestType.valueOf(s.getString("type", "").toUpperCase());
                int amount = Math.max(1, s.getInt("amount", 1));
                definitions.put(id, new QuestDef(
                        id,
                        s.getString("name", id),
                        type,
                        s.getString("target", "ANY"),
                        amount,
                        s.getString("reward-text", ""),
                        s.getStringList("reward-commands")));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Quest '" + id + "' has an invalid type, skipping.");
            }
        }
        plugin.getLogger().info("Loaded " + definitions.size() + " quest definitions.");
    }

    public void loadData() {
        data = YamlConfiguration.loadConfiguration(file);
    }

    public void saveAll() {
        cache.forEach(this::writeToYaml);
        try {
            plugin.getDataFolder().mkdirs();
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }

    public void unload(UUID uuid) {
        PlayerQuests pq = cache.remove(uuid);
        if (pq != null) writeToYaml(uuid, pq);
    }

    private void writeToYaml(UUID uuid, PlayerQuests pq) {
        String base = "players." + uuid;
        data.set(base, null);
        data.set(base + ".date", pq.date);
        data.set(base + ".active", pq.active);
        data.set(base + ".completed", new ArrayList<>(pq.completed));
        pq.progress.forEach((id, value) -> data.set(base + ".progress." + id, value));
    }

    private PlayerQuests readFromYaml(UUID uuid) {
        PlayerQuests pq = new PlayerQuests();
        ConfigurationSection s = data.getConfigurationSection("players." + uuid);
        if (s == null) return pq;
        pq.date = s.getString("date", "");
        pq.active.addAll(s.getStringList("active"));
        pq.completed.addAll(s.getStringList("completed"));
        ConfigurationSection prog = s.getConfigurationSection("progress");
        if (prog != null) {
            for (String id : prog.getKeys(false)) pq.progress.put(id, prog.getInt(id));
        }
        return pq;
    }

    // ---------- quest state ----------

    /** Returns the player's quests, rolling a fresh set if the day has changed. */
    public PlayerQuests get(Player player) {
        PlayerQuests pq = cache.computeIfAbsent(player.getUniqueId(), this::readFromYaml);
        if (!pq.date.equals(today())) reroll(pq);
        return pq;
    }

    public void reroll(PlayerQuests pq) {
        pq.date = today();
        pq.active.clear();
        pq.progress.clear();
        pq.completed.clear();

        List<String> pool = new ArrayList<>(definitions.keySet());
        Collections.shuffle(pool);
        int count = Math.max(1, Math.min(4, plugin.getConfig().getInt("daily-quest-count", 3)));
        pq.active.addAll(pool.subList(0, Math.min(count, pool.size())));
    }

    public QuestDef definition(String id) {
        return definitions.get(id);
    }

    private String today() {
        return LocalDate.now().toString();
    }

    // ---------- progress ----------

    public void addProgress(Player player, QuestType type, String target) {
        PlayerQuests pq = get(player);
        for (String id : pq.active) {
            QuestDef def = definitions.get(id);
            if (def == null || pq.completed.contains(id) || !def.matches(type, target)) continue;

            int now = pq.progress.merge(id, 1, Integer::sum);
            if (now >= def.amount()) complete(player, pq, def);
        }
    }

    private void complete(Player player, PlayerQuests pq, QuestDef def) {
        pq.completed.add(def.id());
        for (String command : def.rewardCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
        plugin.send(player, "<green>Quest complete: <yellow>" + def.name() + "</yellow>! Reward: <gold>"
                + def.rewardText());
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }
}
