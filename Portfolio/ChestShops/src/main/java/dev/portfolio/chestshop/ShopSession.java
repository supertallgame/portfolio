package dev.portfolio.chestshop;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * A player's work-in-progress shop sign. It lives for as long as they are using the
 * /shopsign menus (including the item catalog and chat search).
 * Deposited payment items are held here and are returned if the session ends without a sign.
 */
public final class ShopSession {

    public ItemStack need;          // plain item the owner needs (chosen from the catalog)
    public int needAmount = 1;
    public ItemStack pay;           // payment item (taken from the owner's inventory)
    public int payAmount = 1;       // items given per trade
    public int payStock = 0;        // items deposited so far

    public boolean awaitingSearch = false;
    public long searchStartedAt = 0;
    public String searchQuery = null;
    public int catalogPage = 0;

    public int maxNeed() {
        return need == null ? 64 : need.getMaxStackSize();
    }

    public int maxPay() {
        return pay == null ? 64 : pay.getMaxStackSize();
    }

    public boolean complete() {
        return need != null && pay != null;
    }

    /** Gives every deposited payment item back (dropping whatever doesn't fit). Returns how many. */
    public int refund(Player player) {
        if (pay == null || payStock <= 0) {
            payStock = 0;
            return 0;
        }
        int total = payStock;
        for (ItemStack stack : InventoryUtil.stacks(pay, payStock)) {
            player.getInventory().addItem(stack).values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
        payStock = 0;
        pay = null;
        return total;
    }
}
