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
 * Click the NEED slot to browse every item. Click items in YOUR inventory to deposit them as payment.
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

    public static final class Holder implements InventoryHolder {
        final ShopSession session;
        Inventory inventory;

        Holder(ShopSession session) {
            this.session = session;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    /** Starts a brand-new session (used by /shopsign). */
    public static void open(ChestShopPlugin plugin, Player player) {
        ShopSession session = plugin.sessions().start(player);
        show(plugin, player, session);
        plugin.send(player, "Click the <yellow>NEED</yellow> slot to browse every item. Click items in "
                + "<yellow>your inventory</yellow> to put them in as <green>PAY</green>.");
    }

    /** Shows the main menu for an existing session. */
    public static void show(ChestShopPlugin plugin, Player player, ShopSession session) {
        Holder holder = new Holder(session);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, MM.deserialize("<dark_gray>Create a Shop Sign"));
        holder.inventory = inventory;
        render(holder);
        player.openInventory(inventory);
    }

    public static void render(Holder h) {
        ShopSession s = h.session;
        Inventory inv = h.inventory;

        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler());

        inv.setItem(NEED_LABEL, named(Material.PAPER, "<yellow><bold>YOU NEED",
                "<gray>What customers hand over"));
        inv.setItem(PAY_LABEL, named(Material.PAPER, "<green><bold>YOU PAY",
                "<gray>What customers receive from the chest",
                "",
                "<gray>Click items in <white>your inventory</white> to put them in.",
                "<gray>Left: whole stack  Right: one  Shift: all of them",
                "<gray>They leave your inventory and go into the chest",
                "<gray>when you place the sign."));

        // NEED slot
        if (s.need == null) {
            inv.setItem(NEED_SLOT, named(Material.BOOK, "<yellow>Choose an item",
                    "<gray>Click to browse every item in the game"));
        } else {
            ItemStack display = s.need.asQuantity(Math.max(1, Math.min(s.needAmount, s.need.getMaxStackSize())));
            addLore(display, "", "<white>Quantity: " + s.needAmount, "<dark_gray>Click to choose a different item");
            inv.setItem(NEED_SLOT, display);
        }

        // PAY slot
        if (s.pay == null) {
            inv.setItem(PAY_SLOT, named(Material.STRUCTURE_VOID, "<red>Nothing deposited yet",
                    "<gray>Click an item in your inventory",
                    "<gray>to put it in as payment."));
        } else {
            ItemStack display = s.pay.asQuantity(Math.max(1, Math.min(s.payAmount, s.pay.getMaxStackSize())));
            addLore(display, "",
                    "<white>Per trade: " + s.payAmount,
                    "<white>Deposited: " + s.payStock + " <dark_gray>(" + (s.payStock / s.payAmount) + " trades)",
                    "<dark_gray>Click: take back a stack, Right: one, Shift: all");
            inv.setItem(PAY_SLOT, display);
        }

        inv.setItem(NEED_MINUS, named(Material.RED_STAINED_GLASS_PANE, "<red>- Quantity",
                "<gray>Left-click: -1", "<gray>Shift-click: -8"));
        inv.setItem(NEED_PLUS, named(Material.LIME_STAINED_GLASS_PANE, "<green>+ Quantity",
                "<gray>Left-click: +1", "<gray>Shift-click: +8"));
        inv.setItem(PAY_MINUS, named(Material.RED_STAINED_GLASS_PANE, "<red>- Per trade",
                "<gray>Left-click: -1", "<gray>Shift-click: -8"));
        inv.setItem(PAY_PLUS, named(Material.LIME_STAINED_GLASS_PANE, "<green>+ Per trade",
                "<gray>Left-click: +1", "<gray>Shift-click: +8"));

        String needText = s.need == null ? "<red>(not chosen)"
                : "<white>" + s.needAmount + "x " + ItemNames.pretty(s.need);
        String payText = s.pay == null ? "<red>(nothing deposited)"
                : "<white>" + s.payAmount + "x " + ItemNames.pretty(s.pay);
        inv.setItem(SUMMARY, named(Material.ARROW, "<white><bold>Each trade",
                "<yellow>Customer hands over: " + needText,
                "<green>Customer receives: " + payText,
                "",
                "<gray>The payment is taken from the chest.",
                "<gray>If it runs out, the sign rejects trades."));

        if (s.complete() && s.payStock >= s.payAmount) {
            inv.setItem(CONFIRM, named(Material.LIME_CONCRETE, "<green><bold>Get shop sign",
                    "<gray>Click to receive your sign"));
        } else if (s.complete()) {
            inv.setItem(CONFIRM, named(Material.GRAY_CONCRETE, "<gray>Deposit at least " + s.payAmount
                    + " items to pay with"));
        } else {
            inv.setItem(CONFIRM, named(Material.GRAY_CONCRETE, "<gray>Choose what you need and deposit your payment"));
        }
        inv.setItem(CANCEL, named(Material.BARRIER, "<red>Cancel", "<gray>Your deposited items are returned"));
    }

    // ---------- item helpers ----------

    static ItemStack filler() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setHideTooltip(true);
        filler.setItemMeta(meta);
        return filler;
    }

    static ItemStack named(Material material, String name, String... lore) {
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

    private static void addLore(ItemStack item, String... extra) {
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        for (String l : extra) lore.add(l.isEmpty() ? Component.empty() : line(l));
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    static Component line(String mini) {
        return MM.deserialize("<!italic>" + mini);
    }
}
