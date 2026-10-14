package dev.portfolio.chestshop;

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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** A paginated, searchable list of every item in the game. Used to pick what the shop NEEDS. */
public final class ItemCatalog {

    public static final int PAGE_SIZE = 45;
    public static final int PREV = 45;
    public static final int BACK = 47;
    public static final int SEARCH = 48;
    public static final int INFO = 49;
    public static final int CLEAR = 50;
    public static final int NEXT = 53;

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private record Entry(Material material, String pretty, String key) {
    }

    private static List<Entry> all;

    private ItemCatalog() {
    }

    private static synchronized List<Entry> all() {
        if (all == null) {
            List<Entry> list = new ArrayList<>();
            for (Material m : Material.values()) {
                if (m.isLegacy() || m.isAir() || !m.isItem()) continue;
                String pretty = ItemNames.pretty(m);
                list.add(new Entry(m, pretty, pretty.toLowerCase(Locale.ROOT)));
            }
            list.sort(Comparator.comparing(Entry::pretty));
            all = list;
        }
        return all;
    }

    /** Every item whose name contains the query (case-insensitive). A null/blank query returns everything. */
    public static List<Material> search(String query) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        List<Material> out = new ArrayList<>();
        for (Entry e : all()) {
            if (q.isEmpty() || e.key().contains(q) || e.material().name().toLowerCase(Locale.ROOT)
                    .contains(q.replace(' ', '_'))) {
                out.add(e.material());
            }
        }
        return out;
    }

    public static final class Holder implements InventoryHolder {
        final ShopSession session;
        List<Material> results;
        Inventory inventory;

        Holder(ShopSession session) {
            this.session = session;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public static void open(ChestShopPlugin plugin, Player player, ShopSession session) {
        Holder holder = new Holder(session);
        holder.results = search(session.searchQuery);
        Inventory inventory = Bukkit.createInventory(holder, 54, MM.deserialize("<dark_gray>Choose the item you need"));
        holder.inventory = inventory;
        render(holder);
        player.openInventory(inventory);
    }

    public static int pageCount(Holder h) {
        return Math.max(1, (int) Math.ceil(h.results.size() / (double) PAGE_SIZE));
    }

    public static void render(Holder h) {
        ShopSession s = h.session;
        Inventory inv = h.inventory;
        int pages = pageCount(h);
        s.catalogPage = Math.max(0, Math.min(pages - 1, s.catalogPage));

        inv.clear();
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int index = s.catalogPage * PAGE_SIZE + slot;
            if (index >= h.results.size()) break;
            inv.setItem(slot, icon(h.results.get(index)));
        }

        for (int slot = 45; slot < 54; slot++) inv.setItem(slot, ShopSignMenu.filler());
        if (s.catalogPage > 0) {
            inv.setItem(PREV, ShopSignMenu.named(Material.ARROW, "<yellow>Previous page"));
        }
        if (s.catalogPage < pages - 1) {
            inv.setItem(NEXT, ShopSignMenu.named(Material.ARROW, "<yellow>Next page"));
        }
        inv.setItem(BACK, ShopSignMenu.named(Material.BARRIER, "<red>Back", "<gray>Return to the shop menu"));
        inv.setItem(SEARCH, ShopSignMenu.named(Material.COMPASS, "<aqua>Search",
                "<gray>Click, then type part of an", "<gray>item name in chat"));
        inv.setItem(INFO, ShopSignMenu.named(Material.PAPER,
                "<white>Page " + (s.catalogPage + 1) + " / " + pages,
                "<gray>" + h.results.size() + " items"
                        + (s.searchQuery == null ? "" : " matching '" + s.searchQuery + "'")));
        if (s.searchQuery != null) {
            inv.setItem(CLEAR, ShopSignMenu.named(Material.MILK_BUCKET, "<yellow>Clear search",
                    "<gray>Show every item again"));
        }
    }

    private static ItemStack icon(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.lore(List.of(ShopSignMenu.line("<gray>Click to choose")));
            item.setItemMeta(meta);
        }
        return item;
    }
}
