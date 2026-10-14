# ChestShops

Item-for-item chest shops for Paper 1.21+, created through a `/shopsign` menu. No money, no dependencies.

## How it works
1. Place a chest (or barrel) with some free space.
2. Run `/shopsign`. A menu opens:
   - Click the **NEED** slot to open a catalog of **every item in the game** (45 per page, with chat search), and pick what customers must hand over. Set the quantity with `+` / `-`.
   - Click items in **your own inventory** to deposit them as the **PAY**. They really leave your inventory
     (left-click = whole stack, right-click = one, shift = all of them). Click the PAY slot to take them back.
   - Set how many items one trade pays with the per-trade `+` / `-` buttons
   - Click **Get shop sign**
3. You receive a locked shop sign. **Sneak + right-click** the side of your chest to place it. The deposited payment
   moves from the sign into the chest.
4. Customers right-click the sign to trade. What they hand over is stored in the chest.

If the chest doesn't have enough payment, the sign rejects the trade. It also rejects when the
customer lacks the needed items, the chest is full, or the customer's inventory is full.

Closing the menu or choosing Cancel returns everything you deposited.

> The payment you deposit is stored inside the sign item until you place it. Don't lose the sign before placing it.

## Features
- Item catalog of everything obtainable in the game, paginated and searchable by name
- Payment items are physically taken from your inventory (no duplication) and returned if you cancel, close the menu, or disconnect
- The sign text is generated and the sign is **waxed**: it can never be edited
- Shop data lives on the sign (PersistentDataContainer): no database, survives restarts
- Trades are dry-run on copies of both inventories first, so a trade either fully succeeds or changes nothing
- Works with single and double chests, and barrels
- Owner is notified of trades while online; right-click your own sign to see stock and trade count
- Breaking a shop gives the owner a working shop sign back

## Protection
- **Only the player who placed the shop can break the sign or its chest**, or open the chest (admins with `playershops.admin` can bypass)
- A shop can only be put on a chest you placed yourself (`require-placed-by-owner`)
- Hoppers can't drain shop chests; explosions and pistons can't move or destroy shops

## Build
```
mvn clean package
```

## Commands & permissions
| Command | Description |
|---------|-------------|
| `/shopsign` (`/ss`) | Open the shop sign menu |
| `/pshop` | Help; `/pshop reload` reloads the config |

| Node | Default | Description |
|------|---------|-------------|
| playershops.create | true | Use `/shopsign` and place shops |
| playershops.use | true | Trade at shops |
| playershops.admin | op | Bypass protection, reload |
