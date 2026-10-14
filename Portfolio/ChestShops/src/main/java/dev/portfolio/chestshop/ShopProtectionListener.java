package dev.portfolio.chestshop;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

/** Only the player who placed a shop can open or break its chest and sign. */
public class ShopProtectionListener implements Listener {

    private final ChestShopPlugin plugin;

    public ShopProtectionListener(ChestShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        ShopRegistry registry = plugin.registry();

        // --- breaking the sign ---
        UUID signOwner = registry.ownerOf(block);
        if (signOwner != null) {
            if (!signOwner.equals(player.getUniqueId()) && !player.hasPermission("playershops.admin")) {
                event.setCancelled(true);
                plugin.send(player, "<red>Only the player who placed this shop can break it.");
                return;
            }
            // Give back a proper shop sign (with its terms) instead of a plain sign
            registry.read(block).ifPresent(shop -> {
                event.setDropItems(false);
                dropSign(player, block.getLocation(), shop);
            });
            return;
        }

        // --- breaking the chest ---
        if (ShopRegistry.isContainer(block)) {
            if (!registry.canAccess(player, block)) {
                event.setCancelled(true);
                plugin.send(player, "<red>Only the player who placed this shop can break its chest.");
                return;
            }
            // The owner is removing the chest: turn its sign(s) back into shop sign items
            for (Block sign : registry.signsFor(block)) {
                registry.read(sign).ifPresent(shop -> dropSign(player, sign.getLocation(), shop));
                sign.setType(Material.AIR, false);
            }
        }
    }

    private void dropSign(Player player, Location where, ShopData shop) {
        if (player.getGameMode() == GameMode.CREATIVE) return;
        where.getWorld().dropItemNaturally(where.clone().add(0.5, 0.5, 0.5),
                plugin.registry().createSignItem(plugin.signMaterial(), shop.terms()));
    }

    /** Nobody but the owner can open the chest behind a shop; everyone else uses the sign. */
    @EventHandler
    public void onOpen(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || !ShopRegistry.isContainer(block)) return;

        if (!plugin.registry().canAccess(event.getPlayer(), block)) {
            event.setCancelled(true);
            plugin.send(event.getPlayer(), "<red>This is a shop chest. Right-click the sign to trade.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent event) {
        if (!plugin.getConfig().getBoolean("protect-from-hoppers", true)) return;
        if (plugin.registry().holderHasShop(event.getSource().getHolder(false))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (plugin.getConfig().getBoolean("protect-from-explosions", true)) {
            event.blockList().removeIf(plugin.registry()::isProtected);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (plugin.getConfig().getBoolean("protect-from-explosions", true)) {
            event.blockList().removeIf(plugin.registry()::isProtected);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (plugin.getConfig().getBoolean("protect-from-explosions", true)
                && event.getBlocks().stream().anyMatch(plugin.registry()::isProtected)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (plugin.getConfig().getBoolean("protect-from-explosions", true)
                && event.getBlocks().stream().anyMatch(plugin.registry()::isProtected)) {
            event.setCancelled(true);
        }
    }
}
