# CoreEconomy

A standalone economy plugin for Paper 1.21+ (no Vault required).

## Features
- Persistent balances (`balances.yml`) with autosave
- `/balance`, `/pay`, `/baltop`, and admin `/eco give|take|set|reload`
- Config-driven GUI shop: left-click buys, right-click sells, Shift = stack of 16
- Mob-kill rewards defined per entity type in `config.yml`
- Inventory-full refunds, money rounding, and click/drag cancellation in the GUI

## Build
```
mvn clean package
```
The JAR appears in `target/`. Drop it into your server's `plugins/` folder.

## Permissions
| Node | Default | Description |
|------|---------|-------------|
| economy.balance / .others | true | Check balances |
| economy.pay | true | Pay players |
| economy.baltop | true | View leaderboard |
| economy.shop | true | Open shop |
| economy.admin | op | `/eco` |
