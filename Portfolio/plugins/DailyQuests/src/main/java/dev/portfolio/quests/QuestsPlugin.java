package dev.portfolio.quests;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class QuestsPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private QuestManager quests;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        quests = new QuestManager(this);
        quests.loadDefinitions();
        quests.loadData();

        getServer().getPluginManager().registerEvents(new QuestListener(this), this);
        getCommand("quests").setExecutor(this);
        getCommand("quests").setTabCompleter(this);

        // Autosave every 5 minutes
        getServer().getScheduler().runTaskTimer(this, quests::saveAll, 6000L, 6000L);
    }

    @Override
    public void onDisable() {
        if (quests != null) quests.saveAll();
    }

    public QuestManager quests() {
        return quests;
    }

    public static MiniMessage mm() {
        return MM;
    }

    public void send(CommandSender to, String miniMessage) {
        to.sendMessage(MM.deserialize(getConfig().getString("prefix", "") + miniMessage));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("dailyquests.admin")) {
                send(sender, "<red>No permission.");
                return true;
            }
            reloadConfig();
            quests.loadDefinitions();
            send(sender, "<green>Config reloaded.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reroll")) {
            if (!sender.hasPermission("dailyquests.admin")) {
                send(sender, "<red>No permission.");
                return true;
            }
            Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1])
                    : (sender instanceof Player p ? p : null);
            if (target == null) {
                send(sender, "<red>Player not found.");
                return true;
            }
            quests.reroll(quests.get(target));
            send(sender, "Rerolled quests for <yellow>" + target.getName() + "</yellow>.");
            send(target, "Your daily quests were rerolled!");
            return true;
        }

        if (!(sender instanceof Player player)) {
            send(sender, "<red>Only players can view quests.");
            return true;
        }
        if (!player.hasPermission("dailyquests.use")) {
            send(player, "<red>No permission.");
            return true;
        }
        QuestGUI.open(this, player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("dailyquests.admin")) return out;
        if (args.length == 1) out.addAll(List.of("reroll", "reload"));
        else if (args.length == 2 && args[0].equalsIgnoreCase("reroll")) {
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        }
        String last = args[args.length - 1].toLowerCase();
        return out.stream().filter(s -> s.toLowerCase().startsWith(last)).toList();
    }
}
