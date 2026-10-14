package dev.portfolio.chestshop;

import org.bukkit.inventory.ItemStack;

/**
 * What a shop trades.
 *
 * @param need       the item the shop owner NEEDS (customers hand this over)
 * @param needAmount how many of it per trade
 * @param pay        the item the owner PAYS (customers receive this from the chest)
 * @param payAmount  how many of it per trade
 */
public record ShopTerms(ItemStack need, int needAmount, ItemStack pay, int payAmount) {

    public ItemStack needStack() {
        return need.asQuantity(needAmount);
    }

    public ItemStack payStack() {
        return pay.asQuantity(payAmount);
    }

    public String describeNeed() {
        return needAmount + "x " + ItemNames.pretty(need);
    }

    public String describePay() {
        return payAmount + "x " + ItemNames.pretty(pay);
    }
}
