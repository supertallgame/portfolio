package dev.portfolio.chestshop;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

/**
 * Handles the /shopsign menu, the item catalog and the chat search.
 * Every click in these menus is cancelled and handled manually, so nothing can be duplicated.
 */
public class ShopSignMenuListener implements Listener {

    private final ChestShopPlugin plugin;

    public ShopSignMenuListener(ChestShopPlugin plugin) {
        this.plugin = plugin;
    }

    private static ShopSession sessionOf(InventoryHolder holder) {
        if (holder instanceof ShopSignMenu.Holder menu) return menu.session;
        if (holder instanceof ItemCatalog.Holder catalog) return catalog.session;
        return null;
    }

    // ---------- clicks ----------

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (sessionOf(event.getView().getTopInventory().getHolder()) != null) event.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (sessionOf(holder) == null) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;

        if (holder instanceof ShopSignMenu.Holder menu) handleMenu(player, menu, event);
        else if (holder instanceof ItemCatalog.Holder catalog) handleCatalog(player, catalog, event);
    }

    // ---------- main menu ----------

    private void handleMenu(Player player, ShopSignMenu.Holder menu, InventoryClickEvent event) {
        ShopSession s = menu.session;
        Inventory top = event.getView().getTopInventory();

        if (event.getClickedInventory() == top) {
            int step = event.isShiftClick() ? 8 : 1;
            switch (event.getSlot()) {
                case ShopSignMenu.NEED_SLOT -> {
                    s.searchQuery = null;
                    s.catalogPage = 0;
                    ItemCatalog.open(plugin, player, s);
                    return;
                }
                case ShopSignMenu.PAY_SLOT -> withdraw(player, s, event);
                case ShopSignMenu.NEED_MINUS -> s.needAmount = clamp(s.needAmount - step, s.maxNeed());
                case ShopSignMenu.NEED_PLUS -> s.needAmount = clamp(s.needAmount + step, s.maxNeed());
                case ShopSignMenu.PAY_MINUS -> s.payAmount = clamp(s.payAmount - step, s.maxPay());
                case ShopSignMenu.PAY_PLUS -> s.payAmount = clamp(s.payAmount + step, s.maxPay());
                case ShopSignMenu.CONFIRM -> {
                    confirm(player, menu);
                    return;
                }
                case ShopSignMenu.CANCEL -> {
                    plugin.sessions().end(player, true); // returns deposited items
                    player.closeInventory();
                    return;
                }
                default -> {
                    return;
                }
            }
            click(player);
        } else {
            deposit(player, s, event);
        }
        ShopSignMenu.render(menu);
    }

    /** Moves items from the player's inventory into the payment stock. They really leave the inventory. */
    private void deposit(Player player, ShopSession s, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        ItemStack template = clicked.asOne();

        if (s.pay != null && !s.pay.isSimilar(template)) {
            plugin.send(player, "<red>You're already paying with <white>" + ItemNames.pretty(s.pay)
                    + "</white>. Take those back first (click the PAY slot) to change.");
            return;
        }
        if (s.need != null && s.need.isSimilar(template)) {
            same(player);
            return;
        }

        PlayerInventory inv = player.getInventory();
        int moved = 0;
        if (event.isShiftClick()) {
            for (int i = 0; i < 36; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && item.isSimilar(template)) {
                    moved += item.getAmount();
                    inv.setItem(i, null);
                }
            }
        } else if (event.isRightClick()) {
            moved = 1;
            clicked.setAmount(clicked.getAmount() - 1);
            inv.setItem(event.getSlot(), clicked.getAmount() <= 0 ? null : clicked);
        } else {
            moved = clicked.getAmount();
            inv.setItem(event.getSlot(), null);
        }
        if (moved <= 0) return;

        s.pay = template;
        s.payStock += moved;
        s.payAmount = Math.min(s.payAmount, s.maxPay());
        click(player);
        ShopSignMenu.render((ShopSignMenu.Holder) event.getView().getTopInventory().getHolder());
    }

    /** Moves items from the payment stock back into the player's inventory. */
    private void withdraw(Player player, ShopSession s, InventoryClickEvent event) {
        if (s.pay == null || s.payStock <= 0) return;
        int max = Math.max(1, s.pay.getMaxStackSize());
        int want = event.isShiftClick() ? s.payStock
                : event.isRightClick() ? 1
                : Math.min(s.payStock, max);

        int given = 0;
        while (given < want) {
            int n = Math.min(want - given, max);
            Map<Integer, ItemStack> left = player.getInventory().addItem(s.pay.asQuantity(n));
            int notGiven = left.values().stream().mapToInt(ItemStack::getAmount).sum();
            given += n - notGiven;
            if (notGiven > 0) {
                plugin.send(player, "<red>Your inventory is full.");
                break;
            }
        }
        s.payStock -= given;
        if (s.payStock <= 0) {
            s.payStock = 0;
            s.pay = null;
            s.payAmount = 1;
        }
    }

    private void confirm(Player player, ShopSignMenu.Holder menu) {
        ShopSession s = menu.session;
        if (!s.complete()) {
            plugin.send(player, "<red>Choose the item you need and deposit your payment first.");
            return;
        }
        if (s.payStock < s.payAmount) {
            plugin.send(player, "<red>Each trade pays <white>" + s.payAmount + "</white> but you only deposited <white>"
                    + s.payStock + "</white>. Deposit more or lower the per-trade amount.");
            return;
        }

        ShopTerms terms = new ShopTerms(s.need, s.needAmount, s.pay, s.payAmount);
        ItemStack sign = plugin.registry().createSignItem(plugin.signMaterial(), terms, s.payStock);
        if (!player.getInventory().addItem(sign).isEmpty()) {
            plugin.send(player, "<red>Your inventory is full. Make room and click again.");
            return;
        }

        plugin.sessions().end(player, false); // the payment is now stored inside the sign item
        plugin.send(player, "<green>Shop sign created!</green> Needs <white>" + terms.describeNeed()
                + "</white>, pays <white>" + terms.describePay() + "</white>.");
        plugin.send(player, "Your <white>" + s.payStock + "</white> payment items are stored in the sign and go "
                + "into the chest when you place it. <yellow>Sneak + right-click</yellow> the side of a chest you placed.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
        player.closeInventory();
    }

    // ---------- catalog ----------

    private void handleCatalog(Player player, ItemCatalog.Holder catalog, InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (event.getClickedInventory() != top) return;
        ShopSession s = catalog.session;
        int slot = event.getSlot();

        if (slot < ItemCatalog.PAGE_SIZE) {
            int index = s.catalogPage * ItemCatalog.PAGE_SIZE + slot;
            if (index >= catalog.results.size()) return;
            Material chosen = catalog.results.get(index);
            ItemStack template = new ItemStack(chosen);

            if (s.pay != null && s.pay.isSimilar(template)) {
                same(player);
                return;
            }
            s.need = template;
            s.needAmount = Math.min(s.needAmount, s.maxNeed());
            click(player);
            ShopSignMenu.show(plugin, player, s);
            return;
        }

        switch (slot) {
            case ItemCatalog.PREV -> {
                s.catalogPage--;
                ItemCatalog.render(catalog);
            }
            case ItemCatalog.NEXT -> {
                s.catalogPage++;
                ItemCatalog.render(catalog);
            }
            case ItemCatalog.CLEAR -> {
                s.searchQuery = null;
                s.catalogPage = 0;
                catalog.results = ItemCatalog.search(null);
                ItemCatalog.render(catalog);
            }
            case ItemCatalog.BACK -> ShopSignMenu.show(plugin, player, s);
            case ItemCatalog.SEARCH -> startSearch(player, s);
            default -> {
            }
        }
    }

    private void startSearch(Player player, ShopSession s) {
        s.awaitingSearch = true;
        long started = System.currentTimeMillis();
        s.searchStartedAt = started;
        player.closeInventory();
        plugin.send(player, "<aqua>Type part of an item name in chat</aqua> (for example <white>sand</white>), "
                + "or type <white>cancel</white>.");

        // Don't hold someone's deposited items forever if they wander off
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (plugin.sessions().get(player.getUniqueId()) == s && s.awaitingSearch
                    && s.searchStartedAt == started) {
                plugin.send(player, "<red>Search timed out.");
                plugin.sessions().end(player, true);
            }
        }, 20L * 60);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ShopSession s = plugin.sessions().get(player.getUniqueId());
        if (s == null || !s.awaitingSearch) return;

        event.setCancelled(true); // keep the search text out of public chat
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (plugin.sessions().get(player.getUniqueId()) != s || !s.awaitingSearch) return;
            s.awaitingSearch = false;
            if (!text.equalsIgnoreCase("cancel")) {
                if (ItemCatalog.search(text).isEmpty()) {
                    plugin.send(player, "<red>No items match '" + text + "'.");
                    s.searchQuery = null;
                } else {
                    s.searchQuery = text;
                }
                s.catalogPage = 0;
            }
            ItemCatalog.open(plugin, player, s);
        });
    }

    // ---------- leaving the menus ----------

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        ShopSession s = sessionOf(event.getInventory().getHolder());
        if (s == null || !(event.getPlayer() instanceof Player player)) return;

        // Wait a tick: if they just moved to another of our menus (or are typing a search) keep going.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (plugin.sessions().get(player.getUniqueId()) != s || s.awaitingSearch) return;
            if (sessionOf(player.getOpenInventory().getTopInventory().getHolder()) == s) return;
            plugin.sessions().end(player, true); // closed without finishing: give the items back
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.sessions().end(event.getPlayer(), true);
    }

    // ---------- helpers ----------

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
