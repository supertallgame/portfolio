package dev.portfolio.chestshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The /shopsign menu.
 *
 * <pre>
 *  .  .  NEED  .  .  .  PAY  .  .
 *  . [-] [item] [+] (arrow) [-] [item] [+] .
 *  .  .  .  .  .  .  .  .  .
 *  .  .  .  OK  .  X  .  .  .
 * </pre>
 */
public final class ShopSignMenu {

    public static final int SIZE = 36;
    public static final int NEED_LABEL = 2;
    public static final int PAY_LABEL = 6;
    public static final int NEED_MINUS = 10;
    public static final int NEED_SLOT = 11;
    public static final int NEED_PLUS = 12;
    public static final int SUMMARY = 13;
    public static final int PAY_MINUS = 14;
    public static final int PAY_SLOT = 15;
    public static final int PAY_PLUS = 16;
    public static final int CONFIRM = 30;
    public static final int CANCEL = 32;

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private ShopSignMenu() {
    }

    /** Holds the menu's state while a player is choosing. Nothing is taken from the player. */
    public static final class Holder implements InventoryHolder {
        ItemStack need;
        ItemStack pay;
        int needAmount = 1;
        int payAmount = 1;
        Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }

        boolean complete() {
            return need != null && pay != null;
        }

        int maxNeed() {
            return need == null ? 64 : need.getMaxStackSize();
        }

        int maxPay() {
            return pay == null ? 64 : pay.getMaxStackSize();
        }
    }

    public static void open(ChestShopPlugin plugin, Player player) {
        Holder holder = new Holder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE, MM.deserialize("<dark_gray>Create a Shop Sign"));
        holder.inventory = inventory;
        render(holder);
        player.openInventory(inventory);
        plugin.send(player, "Click an item in <yellow>your inventory</yellow> to choose what you <yellow>NEED</yellow>, "
                + "then the item you <green>PAY</green>.");
    }

    public static void render(Holder h) {
        Inventory inv = h.inventory;

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setHideTooltip(true);
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler);

        inv.setItem(NEED_LABEL, named(Material.PAPER, "<yellow><bold>YOU NEED",
                "<gray>What customers hand over"));
        inv.setItem(PAY_LABEL, named(Material.PAPER, "<green><bold>YOU PAY",
                "<gray>What customers get from the chest"));

        inv.setItem(NEED_SLOT, slotItem(h.need, h.needAmount));
        inv.setItem(PAY_SLOT, slotItem(h.pay, h.payAmount));

        inv.setItem(NEED_MINUS, named(Material.RED_STAINED_GLASS_PANE, "<red>- Quantity",
                "<gray>Left-click: -1", "<gray>Shift-click: -8"));
        inv.setItem(NEED_PLUS, named(Material.LIME_STAINED_GLASS_PANE, "<green>+ Quantity",
                "<gray>Left-click: +1", "<gray>Shift-click: +8"));
        inv.setItem(PAY_MINUS, named(Material.RED_STAINED_GLASS_PANE, "<red>- Quantity",
                "<gray>Left-click: -1", "<gray>Shift-click: -8"));
        inv.setItem(PAY_PLUS, named(Material.LIME_STAINED_GLASS_PANE, "<green>+ Quantity",
                "<gray>Left-click: +1", "<gray>Shift-click: +8"));

        String needText = h.need == null ? "<red>(not chosen)" : "<white>" + h.needAmount + "x "
                + ItemNames.pretty(h.need);
        String payText = h.pay == null ? "<red>(not chosen)" : "<white>" + h.payAmount + "x "
                + ItemNames.pretty(h.pay);
        inv.setItem(SUMMARY, named(Material.ARROW, "<white><bold>Each trade",
                "<yellow>Customer hands over: " + needText,
                "<green>Customer receives: " + payText,
                "",
                "<gray>The payment is taken from the chest.",
                "<gray>If it runs out, the sign rejects trades."));

        if (h.complete()) {
            inv.setItem(CONFIRM, named(Material.LIME_CONCRETE, "<green><bold>Get shop sign",
                    "<gray>Click to receive your sign"));
        } else {
            inv.setItem(CONFIRM, named(Material.GRAY_CONCRETE, "<gray>Choose both items first"));
        }
        inv.setItem(CANCEL, named(Material.BARRIER, "<red>Cancel"));
    }

    private static ItemStack slotItem(ItemStack chosen, int amount) {
        if (chosen == null) {
            return named(Material.STRUCTURE_VOID, "<red>Empty",
                    "<gray>Click an item in your inventory,",
                    "<gray>or click here with one on your cursor.");
        }
        ItemStack display = chosen.asQuantity(Math.max(1, Math.min(amount, chosen.getMaxStackSize())));
        ItemMeta meta = display.getItemMeta();
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.add(Component.empty());
        lore.add(line("<white>Quantity: " + amount));
        lore.add(line("<dark_gray>Click to clear"));
        meta.lore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private static ItemStack named(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line(name));
        if (lore.length > 0) {
            List<Component> lines = new ArrayList<>();
            for (String l : lore) lines.add(line(l));
            meta.lore(lines);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static Component line(String mini) {
        return MM.deserialize("<!italic>" + mini);
    }
}
