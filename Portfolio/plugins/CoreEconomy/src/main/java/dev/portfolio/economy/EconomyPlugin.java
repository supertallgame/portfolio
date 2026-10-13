package dev.portfolio.economy;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EconomyPlugin extends JavaPlugin {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private BalanceManager balances;
    private final List<ShopItem> shopItems = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        balances = new BalanceManager(this);
        balances.load();
        loadShop();

        EconomyCommands commands = new EconomyCommands(this);
        for (String name : List.of("balance", "pay", "baltop", "eco", "shop")) {
            PluginCommand cmd = getCommand(name);
            if (cmd == null) continue;
            cmd.setExecutor(commands);
            cmd.setTabCompleter(commands);
        }

        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
        getServer().getPluginManager().registerEvents(new MobRewardListener(this), this);

        long ticks = getConfig().getLong("autosave-minutes", 5) * 60L * 20L;
        getServer().getScheduler().runTaskTimer(this, balances::save, ticks, ticks);

        getLogger().info("CoreEconomy enabled with " + shopItems.size() + " shop items.");
    }

    @Override
    public void onDisable() {
        if (balances != null) balances.save();
    }

    public void reload() {
        reloadConfig();
        loadShop();
    }

    private void loadShop() {
        shopItems.clear();
        for (Map<?, ?> raw : getConfig().getMapList("shop.items")) {
            Material material = Material.matchMaterial(String.valueOf(raw.get("material")));
            if (material == null || !material.isItem()) {
                getLogger().warning("Unknown shop material: " + raw.get("material"));
                continue;
            }
            double buy = raw.containsKey("buy") ? ((Number) raw.get("buy")).doubleValue() : -1;
            double sell = raw.containsKey("sell") ? ((Number) raw.get("sell")).doubleValue() : -1;
            shopItems.add(new ShopItem(material, buy, sell));
        }
    }

    public BalanceManager balances() {
        return balances;
    }

    public List<ShopItem> shopItems() {
        return shopItems;
    }

    /** Formats an amount using the configured currency symbol, e.g. $1,250.50 */
    public String format(double amount) {
        return getConfig().getString("currency-symbol", "$") + String.format("%,.2f", amount);
    }

    /** Sends a MiniMessage-formatted, prefixed message. */
    public void send(CommandSender to, String miniMessage) {
        to.sendMessage(MM.deserialize(getConfig().getString("prefix", "") + miniMessage));
    }

    public static MiniMessage mm() {
        return MM;
    }
}
