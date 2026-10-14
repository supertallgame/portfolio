package dev.portfolio.chestshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Finds shops in the world and reads/writes their data.
 * A shop is a waxed wall sign carrying PersistentDataContainer data, attached to a chest or barrel.
 * The same terms are also stored on the sign ITEM that /shopsign hands out.
 */
public class ShopRegistry {

    private static final BlockFace[] SIDES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final NamespacedKey ownerKey;
    private final NamespacedKey salesKey;
    private final NamespacedKey stockKey;
    private final NamespacedKey needItemKey;
    private final NamespacedKey needAmountKey;
    private final NamespacedKey payItemKey;
    private final NamespacedKey payAmountKey;
    private final NamespacedKey placedByKey;

    public ShopRegistry(Plugin plugin) {
        ownerKey = new NamespacedKey(plugin, "owner");
        salesKey = new NamespacedKey(plugin, "sales");
        stockKey = new NamespacedKey(plugin, "stock");
        needItemKey = new NamespacedKey(plugin, "need_item");
        needAmountKey = new NamespacedKey(plugin, "need_amount");
        payItemKey = new NamespacedKey(plugin, "pay_item");
        payAmountKey = new NamespacedKey(plugin, "pay_amount");
        placedByKey = new NamespacedKey(plugin, "placed_by");
    }

    public static boolean isContainer(Block block) {
        Material t = block.getType();
        return t == Material.CHEST || t == Material.TRAPPED_CHEST || t == Material.BARREL;
    }

    // ---------- shop terms (shared by sign items and placed signs) ----------

    private void writeTerms(PersistentDataContainer pdc, ShopTerms terms) {
        pdc.set(needItemKey, PersistentDataType.BYTE_ARRAY, terms.need().asOne().serializeAsBytes());
        pdc.set(needAmountKey, PersistentDataType.INTEGER, terms.needAmount());
        pdc.set(payItemKey, PersistentDataType.BYTE_ARRAY, terms.pay().asOne().serializeAsBytes());
        pdc.set(payAmountKey, PersistentDataType.INTEGER, terms.payAmount());
    }

    private Optional<ShopTerms> readTerms(PersistentDataContainer pdc) {
        byte[] need = pdc.get(needItemKey, PersistentDataType.BYTE_ARRAY);
        Integer needAmount = pdc.get(needAmountKey, PersistentDataType.INTEGER);
        byte[] pay = pdc.get(payItemKey, PersistentDataType.BYTE_ARRAY);
        Integer payAmount = pdc.get(payAmountKey, PersistentDataType.INTEGER);
        if (need == null || needAmount == null || pay == null || payAmount == null) return Optional.empty();
        try {
            return Optional.of(new ShopTerms(
                    ItemStack.deserializeBytes(need), needAmount,
                    ItemStack.deserializeBytes(pay), payAmount));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    // ---------- the shop sign ITEM handed out by /shopsign ----------

    public ItemStack createSignItem(Material material, ShopTerms terms, int stock) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line("<gold><bold>Shop Sign"));

        List<Component> lore = new ArrayList<>();
        lore.add(line("<yellow>Needs: <white>" + terms.describeNeed()));
        lore.add(line("<green>Pays: <white>" + terms.describePay()));
        if (stock > 0) {
            lore.add(line("<aqua>Holds " + stock + "x " + ItemNames.pretty(terms.pay()) + " as payment"));
        }
        lore.add(Component.empty());
        lore.add(line("<gray>Sneak + right-click the side of a"));
        lore.add(line("<gray>chest you placed to set up your shop."));
        if (stock > 0) {
            lore.add(line("<gray>The payment goes into the chest."));
            lore.add(line("<red>Don't lose this sign, it holds your items!"));
        }
        meta.lore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        writeTerms(pdc, terms);
        pdc.set(stockKey, PersistentDataType.INTEGER, stock);
        item.setItemMeta(meta);
        return item;
    }

    /** If this item is a shop sign made by /shopsign, returns its terms and payment stock. */
    public Optional<SignItemData> readItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return Optional.empty();
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return readTerms(pdc).map(terms ->
                new SignItemData(terms, pdc.getOrDefault(stockKey, PersistentDataType.INTEGER, 0)));
    }

    private static Component line(String mini) {
        return MM.deserialize("<!italic>" + mini);
    }

    // ---------- the placed shop sign ----------

    /** Cheap check: the owner of a shop sign, or null if this block isn't one. */
    public UUID ownerOf(Block block) {
        if (!Tag.WALL_SIGNS.isTagged(block.getType())) return null;
        if (!(block.getState() instanceof Sign sign)) return null;
        String raw = sign.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Optional<ShopData> read(Block block) {
        if (!Tag.WALL_SIGNS.isTagged(block.getType())) return Optional.empty();
        if (!(block.getState() instanceof Sign sign)) return Optional.empty();

        PersistentDataContainer pdc = sign.getPersistentDataContainer();
        String owner = pdc.get(ownerKey, PersistentDataType.STRING);
        if (owner == null) return Optional.empty();
        Optional<ShopTerms> terms = readTerms(pdc);
        if (terms.isEmpty()) return Optional.empty();
        try {
            return Optional.of(new ShopData(UUID.fromString(owner), terms.get(),
                    pdc.getOrDefault(salesKey, PersistentDataType.INTEGER, 0)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Writes shop data onto a sign state. The caller must call sign.update(). */
    public void write(Sign sign, ShopData data) {
        PersistentDataContainer pdc = sign.getPersistentDataContainer();
        pdc.set(ownerKey, PersistentDataType.STRING, data.owner().toString());
        pdc.set(salesKey, PersistentDataType.INTEGER, data.sales());
        writeTerms(pdc, data.terms());
    }

    // ---------- finding shops around a chest ----------

    /** The chest itself plus the other half if it is a double chest. */
    public List<Block> halves(Block container) {
        List<Block> result = new ArrayList<>();
        result.add(container);
        if (container.getState() instanceof Chest chest
                && chest.getInventory().getHolder() instanceof DoubleChest dc) {
            for (InventoryHolder side : new InventoryHolder[]{dc.getLeftSide(), dc.getRightSide()}) {
                if (side instanceof Chest c && !c.getBlock().equals(container)) result.add(c.getBlock());
            }
        }
        return result;
    }

    /** Shop signs attached directly to this one block (cheap; no double-chest lookup). */
    public List<Block> directShopSigns(Block container) {
        List<Block> signs = new ArrayList<>();
        for (BlockFace face : SIDES) {
            Block neighbour = container.getRelative(face);
            if (neighbour.getBlockData() instanceof WallSign wall
                    && wall.getFacing() == face
                    && ownerOf(neighbour) != null) {
                signs.add(neighbour);
            }
        }
        return signs;
    }

    /** Shop signs attached to this container, including either half of a double chest. */
    public List<Block> signsFor(Block container) {
        List<Block> signs = new ArrayList<>();
        for (Block half : halves(container)) signs.addAll(directShopSigns(half));
        return signs;
    }

    /** Used by the hopper check, so it avoids the heavier double-chest lookup. */
    public boolean holderHasShop(InventoryHolder holder) {
        if (holder instanceof Container c) return !directShopSigns(c.getBlock()).isEmpty();
        if (holder instanceof DoubleChest dc) {
            return holderHasShop(dc.getLeftSide()) || holderHasShop(dc.getRightSide());
        }
        return false;
    }

    /** True for shop signs and for any container that has a shop on it. */
    public boolean isProtected(Block block) {
        if (ownerOf(block) != null) return true;
        return isContainer(block) && !signsFor(block).isEmpty();
    }

    /** May this player open/break this container? Only the shop owner (or an admin) may. */
    public boolean canAccess(Player player, Block container) {
        if (player.hasPermission("playershops.admin")) return true;
        for (Block sign : signsFor(container)) {
            UUID owner = ownerOf(sign);
            if (owner != null && !owner.equals(player.getUniqueId())) return false;
        }
        return true;
    }

    // ---------- who placed a chest (anti-grief) ----------

    public void tagPlacedBy(Block container, UUID placer) {
        if (container.getState() instanceof TileState tile) {
            tile.getPersistentDataContainer().set(placedByKey, PersistentDataType.STRING, placer.toString());
            tile.update();
        }
    }

    /** Everyone recorded as having placed this chest (both halves of a double chest). */
    public Set<UUID> placers(Block container) {
        Set<UUID> result = new HashSet<>();
        for (Block half : halves(container)) {
            if (half.getState() instanceof TileState tile) {
                String raw = tile.getPersistentDataContainer().get(placedByKey, PersistentDataType.STRING);
                if (raw == null) continue;
                try {
                    result.add(UUID.fromString(raw));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return result;
    }
}
