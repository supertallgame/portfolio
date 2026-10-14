package dev.portfolio.chestshop;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/** Right-click a shop sign to trade. The owner right-clicks to see stats. */
public class ShopTradeListener implements Listener {

    private final ChestShopPlugin plugin;

    public ShopTradeListener(ChestShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block signBlock = event.getClickedBlock();
        if (signBlock == null) return;

        Optional<ShopData> maybeShop = plugin.registry().read(signBlock);
        if (maybeShop.isEmpty()) return;

        event.setCancelled(true); // never place blocks against a shop sign
        if (event.getHand() != EquipmentSlot.HAND) return; // fires once per hand

        if (!(signBlock.getBlockData() instanceof WallSign wall)) return;
        Block chestBlock = signBlock.getRelative(wall.getFacing().getOppositeFace());
        Player player = event.getPlayer();

        if (!ShopRegistry.isContainer(chestBlock)) {
            plugin.send(player, "<red>This shop's chest is missing.");
            return;
        }
        Inventory stock = ((Container) chestBlock.getState()).getInventory();
        ShopData shop = maybeShop.get();

        if (shop.owner().equals(player.getUniqueId())) {
            showOwnerInfo(player, shop, stock);
        } else {
            trade(player, signBlock, shop, stock);
        }
    }

    private void showOwnerInfo(Player owner, ShopData shop, Inventory stock) {
        ShopTerms t = shop.terms();
        int payItems = InventoryUtil.count(stock, t.pay());
        plugin.send(owner, "<gold>Your shop</gold>");
        plugin.send(owner, "Customers hand over: <white>" + t.describeNeed());
        plugin.send(owner, "Customers receive: <white>" + t.describePay());
        plugin.send(owner, "Payment left in chest: <white>" + payItems + "</white> items <dark_gray>("
                + (payItems / t.payAmount()) + " trades)</dark_gray>");
        plugin.send(owner, "Completed trades: <white>" + shop.sales());
    }

    private void trade(Player customer, Block signBlock, ShopData shop, Inventory stock) {
        if (!customer.hasPermission("playershops.use")) {
            reject(customer, "<red>You don't have permission to use shops.");
            return;
        }

        ShopTerms t = shop.terms();
        ItemStack need = t.needStack();
        ItemStack pay = t.payStack();

        // 1. Does the customer have what the shop needs?
        if (!customer.getInventory().containsAtLeast(t.need(), t.needAmount())) {
            reject(customer, "<red>You need <yellow>" + t.describeNeed() + "</yellow> to trade here.");
            return;
        }

        // 2. Is there enough payment in the chest?  (Dry-run on a copy so nothing changes on failure.)
        Inventory chestSim = InventoryUtil.simulate(stock);
        if (!chestSim.removeItem(pay).isEmpty()) {
            reject(customer, "<red>This shop doesn't have enough payment left. Trade rejected.");
            return;
        }

        // 3. Is there room in the chest for what the customer hands over?
        if (!chestSim.addItem(need).isEmpty()) {
            reject(customer, "<red>This shop's chest is full. Trade rejected.");
            return;
        }

        // 4. Will the payment fit in the customer's inventory once they've handed over their items?
        Inventory customerSim = InventoryUtil.simulate(customer.getInventory());
        customerSim.removeItem(need);
        if (!customerSim.addItem(pay).isEmpty()) {
            reject(customer, "<red>Your inventory is full.");
            return;
        }

        // Everything checked out: make the trade.
        customer.getInventory().removeItem(need);
        stock.removeItem(pay);
        stock.addItem(need);
        customer.getInventory().addItem(pay);

        // Count the trade on the sign
        if (signBlock.getState() instanceof Sign sign) {
            plugin.registry().write(sign, shop.withSale());
            sign.update();
        }

        plugin.send(customer, "<green>Trade complete:</green> gave <white>" + t.describeNeed()
                + "</white>, received <white>" + t.describePay());
        customer.playSound(customer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);

        Player owner = Bukkit.getPlayer(shop.owner());
        if (owner != null) {
            plugin.send(owner, "<yellow>" + customer.getName() + "</yellow> traded <white>" + t.describeNeed()
                    + "</white> for <white>" + t.describePay() + "</white> at your shop.");
        }
    }

    private void reject(Player player, String message) {
        plugin.send(player, message);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }
}
