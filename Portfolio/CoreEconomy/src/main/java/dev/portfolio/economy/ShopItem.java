package dev.portfolio.economy;

import org.bukkit.Material;

/** One entry in the shop. A negative price means that action is disabled. */
public record ShopItem(Material material, double buy, double sell) {
}
