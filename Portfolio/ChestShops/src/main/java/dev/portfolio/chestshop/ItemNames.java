package dev.portfolio.chestshop;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemNames {

    private ItemNames() {
    }

    /** The custom name if the item has one, otherwise "Diamond Sword" style text. */
    public static String pretty(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
            return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        }
        return pretty(item.getType());
    }

    /** "DIAMOND_SWORD" -> "Diamond Sword" */
    public static String pretty(Material material) {
        StringBuilder out = new StringBuilder();
        for (String word : material.name().toLowerCase().split("_")) {
            if (word.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    public static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
