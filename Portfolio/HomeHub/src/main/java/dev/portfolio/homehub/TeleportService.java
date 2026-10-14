package dev.portfolio.homehub;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Handles cooldowns, warmup timers, and cancelling a pending teleport. */
public class TeleportService implements Listener {

    private final HomeHubPlugin plugin;
    private final Map<UUID, Long> lastTeleport = new HashMap<>();
    private final Map<UUID, BukkitTask> pending = new HashMap<>();

    public TeleportService(HomeHubPlugin plugin) {
        this.plugin = plugin;
    }

    public void teleport(Player player, Location destination, String label) {
        UUID id = player.getUniqueId();

        if (pending.containsKey(id)) {
            plugin.send(player, "<red>You already have a teleport pending.");
            return;
        }

        boolean bypass = player.hasPermission("homehub.bypass");

        if (!bypass) {
            long cooldownMs = plugin.getConfig().getLong("cooldown-seconds", 10) * 1000L;
            long remaining = lastTeleport.getOrDefault(id, 0L) + cooldownMs - System.currentTimeMillis();
            if (remaining > 0) {
                plugin.send(player, "<red>Wait <yellow>" + (long) Math.ceil(remaining / 1000.0)
                        + "s</yellow> before teleporting again.");
                return;
            }
        }

        int warmup = plugin.getConfig().getInt("warmup-seconds", 3);
        if (bypass || warmup <= 0) {
            complete(player, destination, label);
            return;
        }

        plugin.send(player, "Teleporting to <yellow>" + label + "</yellow> in <yellow>" + warmup
                + "s</yellow>. Don't move!");
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pending.remove(id);
            complete(player, destination, label);
        }, warmup * 20L);
        pending.put(id, task);
    }

    private void complete(Player player, Location destination, String label) {
        if (!player.isOnline()) return;
        player.teleportAsync(destination).thenAccept(success -> {
            if (!success) return;
            lastTeleport.put(player.getUniqueId(), System.currentTimeMillis());
            plugin.send(player, "Teleported to <yellow>" + label + "</yellow>.");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1f);
        });
    }

    private void cancel(Player player, String reason) {
        BukkitTask task = pending.remove(player.getUniqueId());
        if (task == null) return;
        task.cancel();
        if (reason != null) plugin.send(player, reason);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return; // ignore head rotation
        cancel(event.getPlayer(), "<red>Teleport cancelled because you moved.");
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            cancel(player, "<red>Teleport cancelled because you took damage.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer(), null);
    }
}
