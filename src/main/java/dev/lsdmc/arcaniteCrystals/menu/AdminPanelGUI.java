package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.database.DatabaseManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.menu.PlayerManagementGUI;
import dev.lsdmc.arcaniteCrystals.menu.DatabaseStatusGUI;
import dev.lsdmc.arcaniteCrystals.menu.MaintenanceGUI;
import dev.lsdmc.arcaniteCrystals.menu.SystemStatsGUI;
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

import java.util.ArrayList;
import java.util.List;

public class AdminPanelGUI implements InventoryHolder, Listener {
    private static final int DATABASE_SLOT = 11;
    private static final int PLAYER_MANAGE_SLOT = 13;
    private static final int CONFIG_SLOT = 15;
    private static final int MAINTENANCE_SLOT = 21;
    private static final int STATS_SLOT = 23;
    private static final int RELOAD_SLOT = 25;
    private static final int CLOSE_SLOT = 49;
    
    private final Player player;
    private final Inventory inventory;
    private boolean isOpen = false;

    public AdminPanelGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, 
            ChatColor.DARK_RED + "✦ Admin Panel ✦");
        buildAdminPanel();
    }

    private void buildAdminPanel() {
        // Database management
        ItemStack dbItem = new ItemStack(Material.COMPARATOR);
        ItemMeta dbMeta = dbItem.getItemMeta();
        dbMeta.setDisplayName(ChatColor.AQUA + "Database Management");
        List<String> dbLore = new ArrayList<>();
        dbLore.add(ChatColor.GRAY + "Current Mode: " + ChatColor.YELLOW + DatabaseManager.getCurrentMode());
        dbLore.add(ChatColor.GRAY + "Status: " + (DatabaseManager.isHealthy() ? 
            ChatColor.GREEN + "Healthy" : ChatColor.RED + "Unhealthy"));
        dbMeta.setLore(dbLore);
        dbItem.setItemMeta(dbMeta);
        inventory.setItem(DATABASE_SLOT, dbItem);
        
        // Player management
        ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta playerMeta = playerItem.getItemMeta();
        playerMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Player Management");
        List<String> playerLore = new ArrayList<>();
        playerLore.add(ChatColor.GRAY + "Manage player data");
        playerLore.add(ChatColor.GRAY + "Reset progress, levels, etc.");
        playerMeta.setLore(playerLore);
        playerItem.setItemMeta(playerMeta);
        inventory.setItem(PLAYER_MANAGE_SLOT, playerItem);
        
        // Configuration
        ItemStack configItem = new ItemStack(Material.REDSTONE);
        ItemMeta configMeta = configItem.getItemMeta();
        configMeta.setDisplayName(ChatColor.GOLD + "Configuration");
        List<String> configLore = new ArrayList<>();
        configLore.add(ChatColor.GRAY + "Edit plugin settings");
        configLore.add(ChatColor.GRAY + "Modify crystal properties");
        configMeta.setLore(configLore);
        configItem.setItemMeta(configMeta);
        inventory.setItem(CONFIG_SLOT, configItem);
        
        // Maintenance
        ItemStack maintenanceItem = new ItemStack(Material.ANVIL);
        ItemMeta maintenanceMeta = maintenanceItem.getItemMeta();
        maintenanceMeta.setDisplayName(ChatColor.RED + "Maintenance");
        List<String> maintenanceLore = new ArrayList<>();
        maintenanceLore.add(ChatColor.GRAY + "System maintenance tools");
        maintenanceLore.add(ChatColor.GRAY + "Cache clearing, data repair");
        maintenanceMeta.setLore(maintenanceLore);
        maintenanceItem.setItemMeta(maintenanceMeta);
        inventory.setItem(MAINTENANCE_SLOT, maintenanceItem);
        
        // Statistics
        ItemStack statsItem = new ItemStack(Material.BOOK);
        ItemMeta statsMeta = statsItem.getItemMeta();
        statsMeta.setDisplayName(ChatColor.GREEN + "Statistics");
        List<String> statsLore = new ArrayList<>();
        statsLore.add(ChatColor.GRAY + "View system statistics");
        statsLore.add(ChatColor.GRAY + "Performance metrics");
        statsMeta.setLore(statsLore);
        statsItem.setItemMeta(statsMeta);
        inventory.setItem(STATS_SLOT, statsItem);
        
        // Reload
        ItemStack reloadItem = new ItemStack(Material.LEVER);
        ItemMeta reloadMeta = reloadItem.getItemMeta();
        reloadMeta.setDisplayName(ChatColor.YELLOW + "Reload Plugin");
        List<String> reloadLore = new ArrayList<>();
        reloadLore.add(ChatColor.GRAY + "Reload all configurations");
        reloadLore.add(ChatColor.GRAY + "Refresh plugin state");
        reloadMeta.setLore(reloadLore);
        reloadItem.setItemMeta(reloadMeta);
        inventory.setItem(RELOAD_SLOT, reloadItem);
        
        // Close button
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Close");
        closeBtn.setItemMeta(closeMeta);
        inventory.setItem(CLOSE_SLOT, closeBtn);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);
        
        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        switch (slot) {
            case PLAYER_MANAGE_SLOT -> {
                clicker.closeInventory();
                new PlayerManagementGUI(clicker).open();
            }
            case DATABASE_SLOT -> {
                clicker.closeInventory();
                new DatabaseStatusGUI(clicker).open();
            }
            case CONFIG_SLOT -> {
                clicker.closeInventory();
                clicker.performCommand("arcanite admin config");
            }
            case MAINTENANCE_SLOT -> {
                clicker.closeInventory();
                new MaintenanceGUI(clicker).open();
            }
            case STATS_SLOT -> {
                clicker.closeInventory();
                new SystemStatsGUI(clicker).open();
            }
            case RELOAD_SLOT -> {
                clicker.closeInventory();
                clicker.getServer().reload();
                clicker.sendMessage(ChatColor.GREEN + "Plugin & configuration reloaded.");
            }
            case CLOSE_SLOT -> clicker.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this && isOpen) {
            isOpen = false;
            HandlerList.unregisterAll(this);
        }
    }

    public void open() {
        if (!isOpen) {
            player.openInventory(inventory);
            isOpen = true;
            Bukkit.getPluginManager().registerEvents(this, ArcaniteCrystals.getInstance());
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
} 