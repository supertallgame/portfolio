# ChestShops

Item-for-item chest shops for Paper 1.21+, created through a `/shopsign` menu. No money, no dependencies.

## How it works
1. Place a chest (or barrel) and fill it with the items you will **pay** out.
2. Run `/shopsign`. A menu opens:
   - Click an item in your inventory to choose what you **NEED** (what customers hand over)
   - Click another to choose what you **PAY** (what customers receive from the chest)
   - Set the quantity of each with the `+` / `-` buttons
   - Click **Get shop sign**
3. You receive a locked shop sign. **Sneak + right-click** the side of your chest to place it.
4. Customers right-click the sign to trade. What they hand over is stored in the chest.

If the chest doesn't have enough payment, the sign rejects the trade. It also rejects when the
customer lacks the needed items, the chest is full, or the customer's inventory is full.

## Features
- Sells **any item**, including enchanted, renamed and potion items (stored as serialized item bytes)
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
