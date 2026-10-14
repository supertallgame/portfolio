package dev.portfolio.chestshop;

/**
 * What a shop sign ITEM carries: the trade, plus the payment stock the owner deposited
 * in the /shopsign menu. The stock is moved into the chest when the sign is placed.
 */
public record SignItemData(ShopTerms terms, int stock) {
}
