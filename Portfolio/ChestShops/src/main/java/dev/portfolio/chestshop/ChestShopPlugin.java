package dev.portfolio.chestshop;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ChestShopPlugin extends JavaPlugin implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private ShopRegistry registry;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        registry = new ShopRegistry(this);

        getServer().getPluginManager().registerEvents(new ShopSignMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopTradeListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopProtectionListener(this), this);

        getCommand("shopsign").setExecutor(this);
        getCommand("pshop").setExecutor(this);
    }

    public ShopRegistry registry() {
        return registry;
    }

    public void send(CommandSender to, String miniMessage) {
        to.sendMessage(MM.deserialize(getConfig().getString("prefix", "") + miniMessage));
    }

    /** The sign item type from config, falling back to an oak sign if it's invalid. */
    public Material signMaterial() {
        Material m = Material.matchMaterial(getConfig().getString("sign-material", "OAK_SIGN"));
        if (m == null || !m.name().endsWith("_SIGN") || m.name().contains("HANGING") || m.name().contains("WALL")) {
            return Material.OAK_SIGN;
        }
        return m;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("shopsign")) {
            if (!(sender instanceof Player player)) {
                send(sender, "<red>Only players can create shop signs.");
                return true;
            }
            if (!player.hasPermission("playershops.create")) {
                send(player, "<red>You don't have permission to create shops.");
                return true;
            }
            ShopSignMenu.open(this, player);
            return true;
        }

        // /pshop
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("playershops.admin")) {
                send(sender, "<red>No permission.");
                return true;
            }
            reloadConfig();
            send(sender, "<green>Config reloaded.");
            return true;
        }

        send(sender, "<gold>How to make a shop");
        send(sender, "<white>1.</white> Place a chest and fill it with the items you'll PAY out.");
        send(sender, "<white>2.</white> Run <yellow>/shopsign</yellow>, pick what you NEED and what you PAY, and set the amounts.");
        send(sender, "<white>3.</white> <yellow>Sneak + right-click</yellow> the side of your chest with the sign.");
        send(sender, "<white>4.</white> Customers right-click the sign to trade. Collect what they hand over from the chest.");
        return true;
    }
}
