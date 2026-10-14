package dev.portfolio.chestshop;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemNames {

    private ItemNames() {
    }

    /** "DIAMOND_SWORD" -> "Diamond Sword", or the custom name if the item has one. */
    public static String pretty(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
            return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        }
        StringBuilder out = new StringBuilder();
        for (String word : item.getType().name().toLowerCase().split("_")) {
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
