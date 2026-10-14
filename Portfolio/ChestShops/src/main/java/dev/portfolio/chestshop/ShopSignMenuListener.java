package dev.portfolio.chestshop;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/** Handles every click inside the /shopsign menu. Items are only copied as templates, never taken. */
public class ShopSignMenuListener implements Listener {

    private final ChestShopPlugin plugin;

    public ShopSignMenuListener(ChestShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ShopSignMenu.Holder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof ShopSignMenu.Holder holder)) return;
        event.setCancelled(true); // nothing may be moved in or out of this menu
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) return;

        if (clicked == top) {
            handleMenuClick(player, holder, event);
        } else {
            handleInventoryPick(player, holder, event.getCurrentItem());
        }
        ShopSignMenu.render(holder);
    }

    /** Player clicked one of their own items: use it for the first empty slot. */
    private void handleInventoryPick(Player player, ShopSignMenu.Holder h, ItemStack clicked) {
        if (clicked == null || clicked.getType().isAir()) return;
        ItemStack template = clicked.asOne();

        if (h.need == null) {
            if (h.pay != null && h.pay.isSimilar(template)) {
                same(player);
                return;
            }
            h.need = template;
            h.needAmount = Math.min(h.needAmount, h.maxNeed());
        } else if (h.pay == null) {
            if (h.need.isSimilar(template)) {
                same(player);
                return;
            }
            h.pay = template;
            h.payAmount = Math.min(h.payAmount, h.maxPay());
        } else {
            plugin.send(player, "<red>Both items are chosen. Click a slot in the menu to clear it first.");
            return;
        }
        click(player);
    }

    private void handleMenuClick(Player player, ShopSignMenu.Holder h, InventoryClickEvent event) {
        int step = event.isShiftClick() ? 8 : 1;
        ItemStack cursor = event.getCursor();
        boolean hasCursor = cursor != null && !cursor.getType().isAir();

        switch (event.getSlot()) {
            case ShopSignMenu.NEED_SLOT -> {
                if (!hasCursor) {
                    h.need = null;
                    h.needAmount = 1;
                } else if (h.pay != null && h.pay.isSimilar(cursor.asOne())) {
                    same(player);
                    return;
                } else {
                    h.need = cursor.asOne();
                    h.needAmount = Math.min(h.needAmount, h.maxNeed());
                }
                click(player);
            }
            case ShopSignMenu.PAY_SLOT -> {
                if (!hasCursor) {
                    h.pay = null;
                    h.payAmount = 1;
                } else if (h.need != null && h.need.isSimilar(cursor.asOne())) {
                    same(player);
                    return;
                } else {
                    h.pay = cursor.asOne();
                    h.payAmount = Math.min(h.payAmount, h.maxPay());
                }
                click(player);
            }
            case ShopSignMenu.NEED_MINUS -> {
                h.needAmount = clamp(h.needAmount - step, h.maxNeed());
                click(player);
            }
            case ShopSignMenu.NEED_PLUS -> {
                h.needAmount = clamp(h.needAmount + step, h.maxNeed());
                click(player);
            }
            case ShopSignMenu.PAY_MINUS -> {
                h.payAmount = clamp(h.payAmount - step, h.maxPay());
                click(player);
            }
            case ShopSignMenu.PAY_PLUS -> {
                h.payAmount = clamp(h.payAmount + step, h.maxPay());
                click(player);
            }
            case ShopSignMenu.CONFIRM -> confirm(player, h);
            case ShopSignMenu.CANCEL -> player.closeInventory();
            default -> {
            }
        }
    }

    private void confirm(Player player, ShopSignMenu.Holder h) {
        if (!h.complete()) {
            plugin.send(player, "<red>Choose both the item you need and the item you pay first.");
            return;
        }
        ShopTerms terms = new ShopTerms(h.need, h.needAmount, h.pay, h.payAmount);
        ItemStack sign = plugin.registry().createSignItem(plugin.signMaterial(), terms);

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(sign);
        if (!leftover.isEmpty()) {
            plugin.send(player, "<red>Your inventory is full. Make room and click again.");
            return;
        }
        plugin.send(player, "<green>Shop sign created!</green> Needs <white>" + terms.describeNeed()
                + "</white>, pays <white>" + terms.describePay() + "</white>.");
        plugin.send(player, "Now <yellow>sneak + right-click</yellow> the side of a chest you placed.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
        player.closeInventory();
    }

    private int clamp(int value, int max) {
        return Math.max(1, Math.min(max, value));
    }

    private void same(Player player) {
        plugin.send(player, "<red>The needed item and the payment can't be the same item.");
    }

    private void click(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
    }
}
