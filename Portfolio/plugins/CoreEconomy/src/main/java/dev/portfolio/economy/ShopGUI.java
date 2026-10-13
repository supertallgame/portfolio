package dev.portfolio.economy;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ShopGUI {

    private ShopGUI() {
    }

    /** Marker holder so the listener can recognise our inventory. */
    public static final class Holder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public static void open(EconomyPlugin plugin, Player player) {
        List<ShopItem> items = plugin.shopItems();
        int rows = Math.max(1, Math.min(6, (int) Math.ceil(items.size() / 9.0)));

        Holder holder = new Holder();
        Component title = EconomyPlugin.mm().deserialize(plugin.getConfig().getString("shop.title", "Shop"));
        Inventory inv = Bukkit.createInventory(holder, rows * 9, title);
        holder.inventory = inv;

        for (int i = 0; i < Math.min(items.size(), rows * 9); i++) {
            ShopItem item = items.get(i);
            ItemStack stack = new ItemStack(item.material());
            ItemMeta meta = stack.getItemMeta();

            List<Component> lore = new ArrayList<>();
            if (item.buy() >= 0) {
                lore.add(line("<green>Buy: " + plugin.format(item.buy()) + " <gray>(Left-click, Shift = x16)"));
            }
            if (item.sell() >= 0) {
                lore.add(line("<gold>Sell: " + plugin.format(item.sell()) + " <gray>(Right-click, Shift = x16)"));
            }
            meta.lore(lore);
            stack.setItemMeta(meta);
            inv.setItem(i, stack);
        }
        player.openInventory(inv);
    }

    private static Component line(String mini) {
        return EconomyPlugin.mm().deserialize("<!italic>" + mini);
    }
}
