package dev.portfolio.chestshop;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class InventoryUtil {

    private InventoryUtil() {
    }

    /** A throwaway copy of an inventory's storage slots, used to test an operation before doing it. */
    public static Inventory simulate(Inventory real) {
        ItemStack[] contents = real.getStorageContents();
        Inventory copy = Bukkit.createInventory(null, contents.length);
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) copy.setItem(i, contents[i].clone());
        }
        return copy;
    }

    /** How many items matching the template are in the inventory. */
    public static int count(Inventory inventory, ItemStack template) {
        int total = 0;
        for (ItemStack stack : inventory.getContents()) {
            if (stack != null && stack.isSimilar(template)) total += stack.getAmount();
        }
        return total;
    }

    /** Splits a total amount into legal stacks (e.g. 150 sand -> 64, 64, 22). */
    public static ItemStack[] stacks(ItemStack template, int total) {
        List<ItemStack> list = new ArrayList<>();
        int max = Math.max(1, template.getMaxStackSize());
        int remaining = total;
        while (remaining > 0) {
            int n = Math.min(remaining, max);
            list.add(template.asQuantity(n));
            remaining -= n;
        }
        return list.toArray(new ItemStack[0]);
    }
}
