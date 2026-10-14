package dev.portfolio.economy;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class MobRewardListener implements Listener {

    private final EconomyPlugin plugin;

    public MobRewardListener(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead instanceof Player) return;

        Player killer = dead.getKiller();
        if (killer == null) return;

        double reward = plugin.getConfig().getDouble("mob-rewards." + dead.getType().name(), 0);
        if (reward <= 0) return;

        plugin.balances().deposit(killer.getUniqueId(), reward);
        killer.sendActionBar(EconomyPlugin.mm().deserialize("<green>+" + plugin.format(reward)));
    }
}
