package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.database.DatabaseManager;
import dev.lsdmc.arcaniteCrystals.util.GUIUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Simple GUI that shows current database mode, health and stats.
 */
public class DatabaseStatusGUI implements InventoryHolder, Listener {

    private final Player viewer;
    private final Inventory inv;
    private boolean open = false;

    public DatabaseStatusGUI(Player viewer) {
        this.viewer = viewer;
        this.inv = Bukkit.createInventory(this, 27, ChatColor.AQUA + "✦ Database Status ✦");
        build();
    }

    private void build() {
        GUIUtils.fillInventory(inv, Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");

        // Mode item
        ItemStack mode = GUIUtils.createNavIcon(Material.COMPARATOR, ChatColor.GOLD + "Current Mode");
        ItemMeta mm = mode.getItemMeta();
        mm.setLore(java.util.List.of(ChatColor.YELLOW + DatabaseManager.getCurrentMode()));
        mode.setItemMeta(mm);
        inv.setItem(10, mode);

        // Health item
        ItemStack health = GUIUtils.createNavIcon(Material.LIME_DYE, ChatColor.GOLD + "Health");
        ItemMeta hm = health.getItemMeta();
        hm.setLore(java.util.List.of(DatabaseManager.isHealthy() ? ChatColor.GREEN + "Healthy" : ChatColor.RED + "Unhealthy"));
        health.setItemMeta(hm);
        inv.setItem(12, health);

        // Stats item
        ItemStack stats = GUIUtils.createNavIcon(Material.BOOK, ChatColor.GOLD + "Stats");
        ItemMeta sm = stats.getItemMeta();
        sm.setLore(java.util.List.of(ChatColor.GRAY + DatabaseManager.getStats()));
        stats.setItemMeta(sm);
        inv.setItem(14, stats);

        // Close
        inv.setItem(26, GUIUtils.createNavIcon(Material.BARRIER, ChatColor.RED + "Close"));
    }

    public void open() {
        if (!open) {
            viewer.openInventory(inv);
            open = true;
            Bukkit.getPluginManager().registerEvents(this, ArcaniteCrystals.getInstance());
        }
    }

    @EventHandler
    public void click(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;
        e.setCancelled(true);
        if (e.getRawSlot() == 26) {
            e.getWhoClicked().closeInventory();
        }
    }

    @EventHandler
    public void close(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() == this && open) {
            open = false;
            HandlerList.unregisterAll(this);
        }
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
} 