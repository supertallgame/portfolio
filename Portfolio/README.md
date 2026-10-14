# Minecraft Plugin Portfolio

Three Paper 1.21+ plugins, each a standalone Maven project (Java 21).

| Plugin | What it shows off |
|--------|-------------------|
| [CoreEconomy](CoreEconomy) | Persistence, commands, GUI inventories, config-driven data |
| [HomeHub](HomeHub) | Permissions, scheduler, cooldowns, event cancellation, async teleports |
| [ChestShops](ChestShops) | Custom GUI menus, PersistentDataContainer, item/inventory simulation, anti-grief protection |

## Downloading the jars
Every push is built automatically by GitHub Actions. Open the **Actions** tab, pick the latest run,
and download each plugin's jar from the **Artifacts** section.

## Building locally
```
mvn clean package --file CoreEconomy/pom.xml
```
The jar appears in that folder's `target/`. Drop it into your server's `plugins/` folder.
