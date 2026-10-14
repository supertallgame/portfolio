package dev.portfolio.chestshop;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SessionManager {

    private final ChestShopPlugin plugin;
    private final Map<UUID, ShopSession> sessions = new HashMap<>();

    public SessionManager(ChestShopPlugin plugin) {
        this.plugin = plugin;
    }

    /** Starts a fresh session, returning any items from an unfinished one first. */
    public ShopSession start(Player player) {
        end(player, true);
        ShopSession session = new ShopSession();
        sessions.put(player.getUniqueId(), session);
        return session;
    }

    public ShopSession get(UUID id) {
        return sessions.get(id);
    }

    /** Ends the session. With refund=true, deposited payment items go back to the player. */
    public void end(Player player, boolean refund) {
        ShopSession session = sessions.remove(player.getUniqueId());
        if (session == null || !refund) return;
        int returned = session.refund(player);
        if (returned > 0) {
            plugin.send(player, "Your <white>" + returned + "</white> payment items were returned to you.");
        }
    }

    /** Called on shutdown so nobody loses items they had deposited. */
    public void endAll() {
        for (UUID id : List.copyOf(sessions.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                sessions.remove(id);
                continue;
            }
            end(player, true);
            InventoryHolder open = player.getOpenInventory().getTopInventory().getHolder();
            if (open instanceof ShopSignMenu.Holder || open instanceof ItemCatalog.Holder) {
                player.closeInventory();
            }
        }
    }
}
