package dev.portfolio.quests;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class QuestGUI {

    private QuestGUI() {
    }

    public static final class Holder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public static void open(QuestsPlugin plugin, Player player) {
        PlayerQuests pq = plugin.quests().get(player);

        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, 27,
                QuestsPlugin.mm().deserialize("<dark_purple>Daily Quests"));
        holder.inventory = inv;

        for (int i = 0; i < pq.active.size(); i++) {
            QuestDef def = plugin.quests().definition(pq.active.get(i));
            if (def == null) continue;

            boolean done = pq.completed.contains(def.id());
            int progress = Math.min(def.amount(), pq.progress.getOrDefault(def.id(), 0));

            Material icon = done ? Material.LIME_CONCRETE
                    : def.type() == QuestType.BREAK_BLOCK ? Material.IRON_PICKAXE : Material.IRON_SWORD;
            ItemStack stack = new ItemStack(icon);
            ItemMeta meta = stack.getItemMeta();
            meta.displayName(line((done ? "<green>" : "<yellow>") + def.name()));

            List<Component> lore = new ArrayList<>();
            lore.add(line("<gray>" + describe(def)));
            lore.add(line(bar(progress, def.amount()) + " <white>" + progress + "/" + def.amount()));
            lore.add(line("<gold>Reward: <white>" + def.rewardText()));
            if (done) lore.add(line("<green>Completed!"));
            meta.lore(lore);

            stack.setItemMeta(meta);
            inv.setItem(10 + i * 2, stack);
        }
        player.openInventory(inv);
    }

    private static String describe(QuestDef def) {
        String target = def.target().equalsIgnoreCase("ANY") ? "any mob"
                : def.target().toLowerCase().replace('_', ' ');
        return switch (def.type()) {
            case BREAK_BLOCK -> "Break " + def.amount() + " " + target;
            case KILL_MOB -> "Kill " + def.amount() + " " + target;
        };
    }

    private static String bar(int progress, int total) {
        int filled = (int) Math.round(10.0 * progress / total);
        return "<green>" + "█".repeat(filled) + "</green><dark_gray>" + "█".repeat(10 - filled) + "</dark_gray>";
    }

    private static Component line(String mini) {
        return QuestsPlugin.mm().deserialize("<!italic>" + mini);
    }
}
