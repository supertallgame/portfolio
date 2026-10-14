package dev.portfolio.chestshop;

import io.papermc.paper.event.player.PlayerOpenSignEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.Inventory;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Turns a shop sign item into a shop when it's placed, moves the stored payment into the chest,
 * and records who placed each chest. Shop signs can never be edited.
 */
public class ShopPlaceListener implements Listener {

    private final ChestShopPlugin plugin;

    public ShopPlaceListener(ChestShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        Player player = event.getPlayer();

        Optional<SignItemData> item = plugin.registry().readItem(event.getItemInHand());
        if (item.isPresent()) {
            placeShop(event, placed, player, item.get());
            return;
        }

        // Remember who placed every chest so strangers can't claim someone else's chest
        if (ShopRegistry.isContainer(placed)) {
            UUID placer = player.getUniqueId();
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.registry().tagPlacedBy(placed, placer));
        }
    }

    private void placeShop(BlockPlaceEvent event, Block sign, Player player, SignItemData data) {
        ShopRegistry registry = plugin.registry();
        ShopTerms terms = data.terms();
        int stock = data.stock();

        if (!player.hasPermission("playershops.create")) {
            deny(event, "<red>You don't have permission to create shops.");
            return;
        }
        if (!(sign.getBlockData() instanceof WallSign wall)) {
            deny(event, "<red>Shop signs must go on the <yellow>side</yellow> of a chest or barrel.");
            return;
        }
        Block chest = sign.getRelative(wall.getFacing().getOppositeFace());
        if (!ShopRegistry.isContainer(chest)) {
            deny(event, "<red>Shop signs must go on the side of a chest or barrel.");
            return;
        }
        if (!registry.signsFor(chest).isEmpty()) {
            deny(event, "<red>This chest already has a shop.");
            return;
        }

        boolean admin = player.hasPermission("playershops.admin");
        if (plugin.getConfig().getBoolean("require-placed-by-owner", true) && !admin) {
            Set<UUID> placers = registry.placers(chest);
            if (placers.size() != 1 || !placers.contains(player.getUniqueId())) {
                deny(event, "<red>You can only open a shop on a chest that you placed yourself.");
                return;
            }
        }

        // The payment stored in the sign must fit in the chest
        if (stock > 0) {
            Inventory sim = InventoryUtil.simulate(((Container) chest.getState()).getInventory());
            if (!sim.addItem(InventoryUtil.stacks(terms.pay(), stock)).isEmpty()) {
                deny(event, "<red>This chest doesn't have room for the <white>" + stock + "x "
                        + ItemNames.pretty(terms.pay()) + "</white> stored in your sign. "
                        + "Free up some space or use a bigger chest.");
                return;
            }
        }

        // Valid. The placer becomes the owner. Finish the sign next tick, once the block entity exists.
        ShopData shop = new ShopData(player.getUniqueId(), terms, 0);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (sign.getState() instanceof Sign state) {
                registry.write(state, shop);

                SignSide front = state.getSide(Side.FRONT);
                front.line(0, Component.text(plugin.getConfig().getString("sign-title", "[Shop]"),
                        NamedTextColor.DARK_BLUE));
                front.line(1, Component.text(ItemNames.truncate("Need:" + terms.needAmount() + " "
                        + ItemNames.pretty(terms.need()), 15), NamedTextColor.DARK_RED));
                front.line(2, Component.text(ItemNames.truncate("Pay: " + terms.payAmount() + " "
                        + ItemNames.pretty(terms.pay()), 15), NamedTextColor.DARK_GREEN));
                front.line(3, Component.text(ItemNames.truncate(player.getName(), 15), NamedTextColor.BLACK));

                state.setWaxed(true); // locked: the text can never be edited or changed
                state.update();
            }

            if (stock > 0 && chest.getState() instanceof Container container) {
                container.getInventory().addItem(InventoryUtil.stacks(terms.pay(), stock)).values()
                        .forEach(left -> chest.getWorld().dropItemNaturally(
                                chest.getLocation().add(0.5, 1, 0.5), left));
            }
        });

        plugin.send(player, "<green>Shop created!</green> Customers hand over <white>" + terms.describeNeed()
                + "</white> and receive <white>" + terms.describePay() + "</white> from the chest.");
        if (stock > 0) {
            plugin.send(player, "Your <white>" + stock + "x " + ItemNames.pretty(terms.pay())
                    + "</white> payment was put into the chest.");
        }
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 1.5f);
    }

    private void deny(BlockPlaceEvent event, String message) {
        event.setCancelled(true); // the sign item is not used up
        plugin.send(event.getPlayer(), message);
        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    // ---------- make sure the sign editor never opens ----------

    /** Placing a sign normally opens the text editor; for shop signs it must not. */
    @EventHandler
    public void onOpenEditor(PlayerOpenSignEvent event) {
        if (plugin.registry().ownerOf(event.getSign().getBlock()) != null) {
            event.setCancelled(true);
            return;
        }
        if (event.getCause() == PlayerOpenSignEvent.Cause.PLACE) {
            Player player = event.getPlayer();
            if (plugin.registry().readItem(player.getInventory().getItemInMainHand()).isPresent()
                    || plugin.registry().readItem(player.getInventory().getItemInOffHand()).isPresent()) {
                event.setCancelled(true);
            }
        }
    }

    /** Belt and braces: even if an edit packet arrives, shop signs can't be changed. */
    @EventHandler
    public void onSignEdit(SignChangeEvent event) {
        if (plugin.registry().ownerOf(event.getBlock()) != null) event.setCancelled(true);
    }
}
