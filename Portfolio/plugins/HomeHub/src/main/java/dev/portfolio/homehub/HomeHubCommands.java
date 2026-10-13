package dev.portfolio.homehub;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class HomeHubCommands implements CommandExecutor, TabCompleter {

    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_-]{1,16}");
    private static final String WARP_SECTION = "warps";

    private final HomeHubPlugin plugin;

    public HomeHubCommands(HomeHubPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.send(sender, "<red>Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("homehub.use")) {
            plugin.send(player, "<red>No permission.");
            return true;
        }
        return switch (cmd.getName().toLowerCase()) {
            case "sethome" -> setHome(player, args);
            case "home" -> home(player, args);
            case "delhome" -> delHome(player, args);
            case "homes" -> listHomes(player);
            case "setwarp" -> setWarp(player, args);
            case "delwarp" -> delWarp(player, args);
            case "warp" -> warp(player, args);
            case "warps" -> listWarps(player);
            default -> false;
        };
    }

    // ---------- homes ----------

    private boolean setHome(Player p, String[] args) {
        String name = args.length > 0 ? args[0].toLowerCase() : "home";
        if (!VALID_NAME.matcher(name).matches()) {
            plugin.send(p, "<red>Names may use a-z, 0-9, _ and - (max 16 characters).");
            return true;
        }
        String section = p.getUniqueId().toString();
        boolean replacing = plugin.homes().exists(section, name);
        int limit = plugin.getHomeLimit(p);

        if (!replacing && plugin.homes().names(section).size() >= limit) {
            plugin.send(p, "<red>You've reached your limit of <yellow>" + limit + "</yellow> homes.");
            return true;
        }
        plugin.homes().set(section, name, p.getLocation());
        plugin.send(p, (replacing ? "Updated" : "Set") + " home <yellow>" + name + "</yellow>.");
        return true;
    }

    private boolean home(Player p, String[] args) {
        String section = p.getUniqueId().toString();
        Set<String> names = plugin.homes().names(section);
        if (names.isEmpty()) {
            plugin.send(p, "<red>You have no homes. Use <yellow>/sethome</yellow>.");
            return true;
        }

        String name;
        if (args.length > 0) name = args[0].toLowerCase();
        else if (names.contains("home")) name = "home";
        else if (names.size() == 1) name = names.iterator().next();
        else {
            plugin.send(p, "Pick a home: <yellow>" + String.join("</yellow>, <yellow>", names));
            return true;
        }

        Location loc = plugin.homes().get(section, name);
        if (loc == null || loc.getWorld() == null) {
            plugin.send(p, "<red>Home not found (or its world is unloaded).");
            return true;
        }
        plugin.teleports().teleport(p, loc, "home " + name);
        return true;
    }

    private boolean delHome(Player p, String[] args) {
        if (args.length != 1) {
            plugin.send(p, "<red>Usage: /delhome <name>");
            return true;
        }
        String section = p.getUniqueId().toString();
        String name = args[0].toLowerCase();
        if (!plugin.homes().exists(section, name)) {
            plugin.send(p, "<red>You don't have a home called " + name + ".");
            return true;
        }
        plugin.homes().delete(section, name);
        plugin.send(p, "Deleted home <yellow>" + name + "</yellow>.");
        return true;
    }

    private boolean listHomes(Player p) {
        Set<String> names = plugin.homes().names(p.getUniqueId().toString());
        int limit = plugin.getHomeLimit(p);
        String cap = limit == Integer.MAX_VALUE ? "unlimited" : String.valueOf(limit);
        if (names.isEmpty()) {
            plugin.send(p, "You have no homes. (limit: " + cap + ")");
        } else {
            plugin.send(p, "Homes (" + names.size() + "/" + cap + "): <yellow>"
                    + String.join("</yellow>, <yellow>", names));
        }
        return true;
    }

    // ---------- warps ----------

    private boolean setWarp(Player p, String[] args) {
        if (!p.hasPermission("homehub.admin")) {
            plugin.send(p, "<red>No permission.");
            return true;
        }
        if (args.length != 1 || !VALID_NAME.matcher(args[0].toLowerCase()).matches()) {
            plugin.send(p, "<red>Usage: /setwarp <name>  (a-z, 0-9, _ and -)");
            return true;
        }
        plugin.warps().set(WARP_SECTION, args[0].toLowerCase(), p.getLocation());
        plugin.send(p, "Warp <yellow>" + args[0].toLowerCase() + "</yellow> saved.");
        return true;
    }

    private boolean delWarp(Player p, String[] args) {
        if (!p.hasPermission("homehub.admin")) {
            plugin.send(p, "<red>No permission.");
            return true;
        }
        if (args.length != 1 || !plugin.warps().exists(WARP_SECTION, args[0].toLowerCase())) {
            plugin.send(p, "<red>That warp doesn't exist.");
            return true;
        }
        plugin.warps().delete(WARP_SECTION, args[0].toLowerCase());
        plugin.send(p, "Warp deleted.");
        return true;
    }

    private boolean warp(Player p, String[] args) {
        if (args.length != 1) return listWarps(p);
        String name = args[0].toLowerCase();
        Location loc = plugin.warps().get(WARP_SECTION, name);
        if (loc == null || loc.getWorld() == null) {
            plugin.send(p, "<red>That warp doesn't exist.");
            return true;
        }
        plugin.teleports().teleport(p, loc, "warp " + name);
        return true;
    }

    private boolean listWarps(Player p) {
        Set<String> names = plugin.warps().names(WARP_SECTION);
        if (names.isEmpty()) plugin.send(p, "There are no warps yet.");
        else plugin.send(p, "Warps: <yellow>" + String.join("</yellow>, <yellow>", names));
        return true;
    }

    // ---------- tab completion ----------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player p) || args.length != 1) return List.of();
        Set<String> options = switch (cmd.getName().toLowerCase()) {
            case "home", "delhome" -> plugin.homes().names(p.getUniqueId().toString());
            case "warp", "delwarp" -> plugin.warps().names(WARP_SECTION);
            default -> Set.of();
        };
        String prefix = args[0].toLowerCase();
        return new ArrayList<>(options).stream().filter(s -> s.startsWith(prefix)).toList();
    }
}
