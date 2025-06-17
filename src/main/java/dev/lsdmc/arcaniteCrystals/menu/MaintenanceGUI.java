package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
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

/**
 * Simple maintenance tasks GUI (save cache, backup config, clear cache).
 */
public class MaintenanceGUI implements InventoryHolder, Listener {

    private final Player admin;
    private final Inventory inv;
    private boolean open = false;

    private static final int SAVE_SLOT = 11;
    private static final int BACKUP_SLOT = 13;
    private static final int CLEAR_SLOT = 15;
    private static final int CLOSE_SLOT = 26;

    public MaintenanceGUI(Player admin) {
        this.admin = admin;
        this.inv = Bukkit.createInventory(this, 27, ChatColor.RED + "✦ Maintenance ✦");
        build();
    }

    private void build() {
        inv.setItem(SAVE_SLOT, dev.lsdmc.arcaniteCrystals.util.GUIUtils.createNavIcon(Material.WRITABLE_BOOK, ChatColor.YELLOW + "Save Cache"));
        inv.setItem(BACKUP_SLOT, dev.lsdmc.arcaniteCrystals.util.GUIUtils.createNavIcon(Material.CHEST, ChatColor.YELLOW + "Backup Config"));
        inv.setItem(CLEAR_SLOT, dev.lsdmc.arcaniteCrystals.util.GUIUtils.createNavIcon(Material.HOPPER, ChatColor.YELLOW + "Clear Cache"));
        inv.setItem(CLOSE_SLOT, dev.lsdmc.arcaniteCrystals.util.GUIUtils.createNavIcon(Material.BARRIER, ChatColor.RED + "Close"));
    }

    public void open() {
        if (!open) {
            admin.openInventory(inv);
            open = true;
            Bukkit.getPluginManager().registerEvents(this, ArcaniteCrystals.getInstance());
        }
    }

    @EventHandler
    public void click(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        switch (slot) {
            case SAVE_SLOT -> {
                PlayerDataManager.saveAll();
                admin.sendMessage(ChatColor.GREEN + "All cached data saved.");
            }
            case BACKUP_SLOT -> {
                String path = ConfigManager.backupConfig();
                admin.sendMessage(path != null ? ChatColor.GREEN + "Backup created at " + path : ChatColor.RED + "Backup failed!");
            }
            case CLEAR_SLOT -> {
                PlayerDataManager.clearCache();
                admin.sendMessage(ChatColor.GREEN + "Cache cleared.");
            }
            case CLOSE_SLOT -> admin.closeInventory();
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