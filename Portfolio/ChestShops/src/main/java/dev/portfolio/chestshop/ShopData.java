package dev.portfolio.chestshop;

import java.util.UUID;

/** Everything a placed shop sign remembers. Stored on the sign itself (PersistentDataContainer). */
public record ShopData(UUID owner, ShopTerms terms, int sales) {

    public ShopData withSale() {
        return new ShopData(owner, terms, sales + 1);
    }
}
