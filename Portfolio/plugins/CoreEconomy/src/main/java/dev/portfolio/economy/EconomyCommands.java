package dev.portfolio.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EconomyCommands implements CommandExecutor, TabCompleter {

    private final EconomyPlugin plugin;

    public EconomyCommands(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return switch (cmd.getName().toLowerCase()) {
            case "balance" -> balance(sender, args);
            case "pay" -> pay(sender, args);
            case "baltop" -> baltop(sender);
            case "eco" -> eco(sender, args);
            case "shop" -> shop(sender);
            default -> false;
        };
    }

    // ---- /balance [player] ----
    private boolean balance(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                plugin.send(sender, "<red>Console must specify a player.");
                return true;
            }
            plugin.send(p, "Your balance: <green>" + plugin.format(plugin.balances().get(p.getUniqueId())));
            return true;
        }
        if (!sender.hasPermission("economy.balance.others")) {
            plugin.send(sender, "<red>You can't view other players' balances.");
            return true;
        }
        OfflinePlayer target = resolve(args[0]);
        if (target == null) {
            plugin.send(sender, "<red>Unknown player: " + args[0]);
            return true;
        }
        plugin.send(sender, "<yellow>" + target.getName() + "</yellow>'s balance: <green>"
                + plugin.format(plugin.balances().get(target.getUniqueId())));
        return true;
    }

    // ---- /pay <player> <amount> ----
    private boolean pay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            plugin.send(sender, "<red>Only players can pay.");
            return true;
        }
        if (args.length != 2) {
            plugin.send(p, "<red>Usage: /pay <player> <amount>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            plugin.send(p, "<red>That player isn't online.");
            return true;
        }
        if (target.equals(p)) {
            plugin.send(p, "<red>You can't pay yourself.");
            return true;
        }
        Double amount = parseAmount(args[1]);
        if (amount == null || amount <= 0) {
            plugin.send(p, "<red>Enter a positive amount.");
            return true;
        }
        if (!plugin.balances().withdraw(p.getUniqueId(), amount)) {
            plugin.send(p, "<red>You don't have enough money.");
            return true;
        }
        plugin.balances().deposit(target.getUniqueId(), amount);
        plugin.send(p, "Sent <green>" + plugin.format(amount) + "</green> to <yellow>" + target.getName());
        plugin.send(target, "Received <green>" + plugin.format(amount) + "</green> from <yellow>" + p.getName());
        return true;
    }

    // ---- /baltop ----
    private boolean baltop(CommandSender sender) {
        List<Map.Entry<UUID, Double>> top = new ArrayList<>(plugin.balances().all().entrySet());
        top.sort(Map.Entry.<UUID, Double>comparingByValue(Comparator.reverseOrder()));
        plugin.send(sender, "<gold>--- Top Balances ---");
        int rank = 1;
        for (Map.Entry<UUID, Double> entry : top) {
            if (rank > 10) break;
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            plugin.send(sender, "<yellow>#" + rank + " <white>" + (name == null ? "Unknown" : name)
                    + " <gray>- <green>" + plugin.format(entry.getValue()));
            rank++;
        }
        if (top.isEmpty()) plugin.send(sender, "<gray>No balances recorded yet.");
        return true;
    }

    // ---- /eco <give|take|set|reload> ----
    private boolean eco(CommandSender sender, String[] args) {
        if (!sender.hasPermission("economy.admin")) {
            plugin.send(sender, "<red>No permission.");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            plugin.send(sender, "<green>Config reloaded.");
            return true;
        }
        if (args.length != 3) {
            plugin.send(sender, "<red>Usage: /eco <give|take|set> <player> <amount>");
            return true;
        }
        OfflinePlayer target = resolve(args[1]);
        Double amount = parseAmount(args[2]);
        if (target == null || amount == null || amount < 0) {
            plugin.send(sender, "<red>Invalid player or amount.");
            return true;
        }
        UUID id = target.getUniqueId();
        switch (args[0].toLowerCase()) {
            case "give" -> plugin.balances().deposit(id, amount);
            case "take" -> plugin.balances().set(id, plugin.balances().get(id) - amount);
            case "set" -> plugin.balances().set(id, amount);
            default -> {
                plugin.send(sender, "<red>Usage: /eco <give|take|set> <player> <amount>");
                return true;
            }
        }
        plugin.send(sender, "<yellow>" + target.getName() + "</yellow> now has <green>"
                + plugin.format(plugin.balances().get(id)));
        return true;
    }

    // ---- /shop ----
    private boolean shop(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            plugin.send(sender, "<red>Only players can open the shop.");
            return true;
        }
        if (!p.hasPermission("economy.shop")) {
            plugin.send(p, "<red>No permission.");
            return true;
        }
        ShopGUI.open(plugin, p);
        return true;
    }

    // ---- helpers ----
    private OfflinePlayer resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    private Double parseAmount(String raw) {
        try {
            double v = Double.parseDouble(raw);
            if (Double.isNaN(v) || Double.isInfinite(v)) return null;
            return BalanceManager.round(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        String name = cmd.getName().toLowerCase();
        List<String> out = new ArrayList<>();
        if (name.equals("eco")) {
            if (args.length == 1) out.addAll(List.of("give", "take", "set", "reload"));
            else if (args.length == 2) Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        } else if ((name.equals("balance") || name.equals("pay")) && args.length == 1) {
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        }
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        return out.stream().filter(s -> s.toLowerCase().startsWith(last)).toList();
    }
}
