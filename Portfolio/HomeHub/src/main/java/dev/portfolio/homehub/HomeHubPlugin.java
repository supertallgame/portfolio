package dev.portfolio.homehub;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class HomeHubPlugin extends JavaPlugin {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String LIMIT_PREFIX = "homes.limit.";

    private LocationStore homes;
    private LocationStore warps;
    private TeleportService teleports;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        homes = new LocationStore(this, "homes.yml");
        warps = new LocationStore(this, "warps.yml");
        teleports = new TeleportService(this);
        getServer().getPluginManager().registerEvents(teleports, this);

        HomeHubCommands commands = new HomeHubCommands(this);
        for (String name : List.of("sethome", "home", "delhome", "homes", "setwarp", "delwarp", "warp", "warps")) {
            PluginCommand cmd = getCommand(name);
            if (cmd == null) continue;
            cmd.setExecutor(commands);
            cmd.setTabCompleter(commands);
        }
    }

    /** Highest home limit granted by a homes.limit.<n> permission, or the config default. */
    public int getHomeLimit(Player player) {
        int limit = getConfig().getInt("default-home-limit", 2);
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) continue;
            String perm = info.getPermission().toLowerCase();
            if (!perm.startsWith(LIMIT_PREFIX)) continue;
            String suffix = perm.substring(LIMIT_PREFIX.length());
            if (suffix.equals("unlimited")) return Integer.MAX_VALUE;
            try {
                limit = Math.max(limit, Integer.parseInt(suffix));
            } catch (NumberFormatException ignored) {
            }
        }
        return limit;
    }

    public LocationStore homes() {
        return homes;
    }

    public LocationStore warps() {
        return warps;
    }

    public TeleportService teleports() {
        return teleports;
    }

    public void send(CommandSender to, String miniMessage) {
        to.sendMessage(MM.deserialize(getConfig().getString("prefix", "") + miniMessage));
    }
}
