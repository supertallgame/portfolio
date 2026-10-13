package dev.portfolio.economy;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ShopListener implements Listener {

    private final EconomyPlugin plugin;

    public ShopListener(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ShopGUI.Holder) event.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopGUI.Holder)) return;
        event.setCancelled(true); // never let players take items out of the GUI

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getInventory()) return;

        List<ShopItem> items = plugin.shopItems();
        int slot = event.getSlot();
        if (slot < 0 || slot >= items.size()) return;

        ShopItem item = items.get(slot);
        int amount = event.isShiftClick() ? 16 : 1;

        if (event.isLeftClick()) buy(player, item, amount);
        else if (event.isRightClick()) sell(player, item, amount);
    }

    private void buy(Player player, ShopItem item, int amount) {
        if (item.buy() < 0) {
            plugin.send(player, "<red>That item can't be bought.");
            return;
        }
        double cost = BalanceManager.round(item.buy() * amount);
        BalanceManager bank = plugin.balances();

        if (!bank.withdraw(player.getUniqueId(), cost)) {
            plugin.send(player, "<red>You need <yellow>" + plugin.format(cost) + "</yellow> to buy that.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Refund anything that didn't fit in the inventory
        int leftover = player.getInventory().addItem(new ItemStack(item.material(), amount))
                .values().stream().mapToInt(ItemStack::getAmount).sum();
        if (leftover > 0) bank.deposit(player.getUniqueId(), BalanceManager.round(item.buy() * leftover));

        int bought = amount - leftover;
        if (bought == 0) {
            plugin.send(player, "<red>Your inventory is full.");
            return;
        }
        plugin.send(player, "Bought <white>" + bought + "x " + pretty(item) + "</white> for <green>"
                + plugin.format(BalanceManager.round(item.buy() * bought)));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
    }

    private void sell(Player player, ShopItem item, int amount) {
        if (item.sell() < 0) {
            plugin.send(player, "<red>That item can't be sold.");
            return;
        }
        ItemStack probe = new ItemStack(item.material());
        if (!player.getInventory().containsAtLeast(probe, amount)) {
            plugin.send(player, "<red>You don't have " + amount + "x " + pretty(item) + ".");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        player.getInventory().removeItem(new ItemStack(item.material(), amount));
        double earned = BalanceManager.round(item.sell() * amount);
        plugin.balances().deposit(player.getUniqueId(), earned);
        plugin.send(player, "Sold <white>" + amount + "x " + pretty(item) + "</white> for <green>"
                + plugin.format(earned));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.8f);
    }

    private String pretty(ShopItem item) {
        String raw = item.material().name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
