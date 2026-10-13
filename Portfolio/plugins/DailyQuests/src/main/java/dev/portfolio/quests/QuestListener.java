package dev.portfolio.quests;

import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuestListener implements Listener {

    private final QuestsPlugin plugin;

    public QuestListener(QuestsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return; // no creative farming
        plugin.quests().addProgress(player, QuestType.BREAK_BLOCK, event.getBlock().getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMobKill(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead instanceof Player) return;
        Player killer = dead.getKiller();
        if (killer == null) return;
        plugin.quests().addProgress(killer, QuestType.KILL_MOB, dead.getType().name());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.quests().unload(event.getPlayer().getUniqueId());
    }

    // Keep players from pulling items out of the quest menu
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof QuestGUI.Holder) event.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof QuestGUI.Holder) event.setCancelled(true);
    }
}
