package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
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
import java.util.Set;
import java.util.UUID;

public class ArcaniteMainMenu implements InventoryHolder, Listener {
    private static final int TALENTS_SLOT = 9;
    private static final int CRYSTALS_SLOT = 10;
    private static final int WORKSHOP_SLOT = 11;
    private static final int PROGRESS_SLOT = 12;
    private static final int STATS_SLOT = 13;
    private static final int HELP_SLOT = 14;
    private static final int ADMIN_SLOT = 15;
    private static final int CLOSE_SLOT = 16;
    
    // Quick status slots
    private static final int LEVEL_INFO_SLOT = 27;
    private static final int ACTIVE_CRYSTAL_SLOT = 28;
    private static final int NEXT_UPGRADE_SLOT = 29;
    private static final int ECONOMY_SLOT = 30;
    private static final int COOLDOWN_SLOT = 31;
    
    private final Player player;
    private final UUID playerId;
    private final Inventory inventory;
    private boolean isOpen = false;
    
    public ArcaniteMainMenu(Player player) {
        this.player = player;
        this.playerId = player.getUniqueId();
        this.inventory = Bukkit.createInventory(this, 54, 
            ChatColor.DARK_PURPLE + "✦ Arcanite Management Center ✦");
        buildMainMenu();
    }
    
    private void buildMainMenu() {
        // Fill borders
        fillBorders();
        
        // Add main features
        addMainFeatures();
        
        // Add quick status
        addQuickStatus();
        
        // Add recent activity
        addRecentActivity();
    }
    
    private void addMainFeatures() {
        // Talents Section
        ItemStack talentsBtn = new ItemStack(Material.NETHER_STAR);
        ItemMeta talentsMeta = talentsBtn.getItemMeta();
        talentsMeta.setDisplayName(ChatColor.GOLD + "Crystal Talents");
        List<String> talentsLore = new ArrayList<>();
        talentsLore.add(ChatColor.GRAY + "Unlock and upgrade crystal effects");
        talentsLore.add("");
        int playerLevel = PlayerDataManager.getLevel(playerId);
        talentsLore.add(ChatColor.BLUE + "Your Level: " + ChatColor.WHITE + playerLevel);
        talentsLore.add(ChatColor.YELLOW + "Click to view talents");
        talentsMeta.setLore(talentsLore);
        talentsBtn.setItemMeta(talentsMeta);
        inventory.setItem(TALENTS_SLOT, talentsBtn);
        
        // Crystal Inventory
        ItemStack crystalsBtn = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta crystalsMeta = crystalsBtn.getItemMeta();
        crystalsMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Crystal Inventory");
        List<String> crystalsLore = new ArrayList<>();
        crystalsLore.add(ChatColor.GRAY + "Manage your crystal collection");
        crystalsLore.add("");
        int crystalCount = countPlayerCrystals(player);
        crystalsLore.add(ChatColor.BLUE + "Crystals: " + ChatColor.WHITE + crystalCount);
        crystalsLore.add(ChatColor.YELLOW + "Click to open inventory");
        crystalsMeta.setLore(crystalsLore);
        crystalsBtn.setItemMeta(crystalsMeta);
        inventory.setItem(CRYSTALS_SLOT, crystalsBtn);
        
        // Crystal Workshop
        ItemStack workshopBtn = new ItemStack(Material.SMITHING_TABLE);
        ItemMeta workshopMeta = workshopBtn.getItemMeta();
        workshopMeta.setDisplayName(ChatColor.AQUA + "Crystal Workshop");
        List<String> workshopLore = new ArrayList<>();
        workshopLore.add(ChatColor.GRAY + "Craft and enhance crystals");
        workshopLore.add("");
        workshopLore.add(ChatColor.BLUE + "Available Recipes:");
        workshopLore.add(ChatColor.GRAY + "• Basic Crystal");
        workshopLore.add(ChatColor.GRAY + "• Crystal Recharge");
        workshopLore.add(ChatColor.YELLOW + "Click to open workshop");
        workshopMeta.setLore(workshopLore);
        workshopBtn.setItemMeta(workshopMeta);
        inventory.setItem(WORKSHOP_SLOT, workshopBtn);
        
        // Progress Dashboard
        ItemStack progressBtn = new ItemStack(Material.BOOK);
        ItemMeta progressMeta = progressBtn.getItemMeta();
        progressMeta.setDisplayName(ChatColor.GREEN + "Progress Dashboard");
        List<String> progressLore = new ArrayList<>();
        progressLore.add(ChatColor.GRAY + "Track your crystal journey");
        progressLore.add("");
        Set<String> upgrades = PlayerDataManager.getUnlockedUpgrades(playerId);
        progressLore.add(ChatColor.BLUE + "Unlocked Effects: " + ChatColor.WHITE + upgrades.size());
        progressLore.add(ChatColor.YELLOW + "Click to view progress");
        progressMeta.setLore(progressLore);
        progressBtn.setItemMeta(progressMeta);
        inventory.setItem(PROGRESS_SLOT, progressBtn);
        
        // Statistics
        ItemStack statsBtn = new ItemStack(Material.COMPASS);
        ItemMeta statsMeta = statsBtn.getItemMeta();
        statsMeta.setDisplayName(ChatColor.YELLOW + "Statistics");
        List<String> statsLore = new ArrayList<>();
        statsLore.add(ChatColor.GRAY + "View your crystal statistics");
        statsLore.add("");
        statsLore.add(ChatColor.BLUE + "Crystals Identified: " + ChatColor.WHITE + getStatistic(playerId, "crystals_identified"));
        statsLore.add(ChatColor.BLUE + "Energy Used: " + ChatColor.WHITE + getStatistic(playerId, "energy_used"));
        statsMeta.setLore(statsLore);
        statsBtn.setItemMeta(statsMeta);
        inventory.setItem(STATS_SLOT, statsBtn);
        
        // Help
        ItemStack helpBtn = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta helpMeta = helpBtn.getItemMeta();
        helpMeta.setDisplayName(ChatColor.AQUA + "Help & Guide");
        List<String> helpLore = new ArrayList<>();
        helpLore.add(ChatColor.GRAY + "Learn about crystal mechanics");
        helpLore.add("");
        helpLore.add(ChatColor.YELLOW + "Click to view guide");
        helpMeta.setLore(helpLore);
        helpBtn.setItemMeta(helpMeta);
        inventory.setItem(HELP_SLOT, helpBtn);
        
        // Admin Panel (only for admins)
        if (player.hasPermission("arcanite.admin")) {
            ItemStack adminBtn = new ItemStack(Material.COMMAND_BLOCK);
            ItemMeta adminMeta = adminBtn.getItemMeta();
            adminMeta.setDisplayName(ChatColor.RED + "Admin Panel");
            List<String> adminLore = new ArrayList<>();
            adminLore.add(ChatColor.GRAY + "Manage crystal system");
            adminLore.add("");
            adminLore.add(ChatColor.YELLOW + "Click to open admin panel");
            adminMeta.setLore(adminLore);
            adminBtn.setItemMeta(adminMeta);
            inventory.setItem(ADMIN_SLOT, adminBtn);
        }
        
        // Close Button
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Close Menu");
        closeBtn.setItemMeta(closeMeta);
        inventory.setItem(CLOSE_SLOT, closeBtn);
    }
    
    private void addQuickStatus() {
        // Level Info
        ItemStack levelInfo = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta levelMeta = levelInfo.getItemMeta();
        levelMeta.setDisplayName(ChatColor.GOLD + "Level Information");
        List<String> levelLore = new ArrayList<>();
        int playerLevel = PlayerDataManager.getLevel(playerId);
        levelLore.add(ChatColor.BLUE + "Current Level: " + ChatColor.WHITE + playerLevel);
        levelLore.add(ChatColor.BLUE + "Max Tier: " + ChatColor.WHITE + (1 + (playerLevel / 3)));
        levelLore.add("");
        levelLore.add(ChatColor.YELLOW + "Level up to unlock:");
        levelLore.add(ChatColor.GRAY + "• More crystal effects");
        levelLore.add(ChatColor.GRAY + "• Higher tier crystals");
        levelMeta.setLore(levelLore);
        levelInfo.setItemMeta(levelMeta);
        inventory.setItem(LEVEL_INFO_SLOT, levelInfo);
        
        // Active Crystal
        ItemStack activeCrystal = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta crystalMeta = activeCrystal.getItemMeta();
        crystalMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Active Crystal");
        List<String> crystalLore = new ArrayList<>();
        
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (CrystalManager.isCrystal(offHand)) {
            List<String> effects = CrystalManager.getCrystalEffects(offHand);
            int energy = CrystalManager.getEnergy(offHand);
            int maxEnergy = CrystalManager.getMaxEnergy(offHand);
            
            crystalLore.add(ChatColor.GREEN + "✓ Crystal Active");
            crystalLore.add("");
            crystalLore.add(ChatColor.BLUE + "Energy: " + ChatColor.WHITE + energy + "/" + maxEnergy);
            crystalLore.add("");
            crystalLore.add(ChatColor.LIGHT_PURPLE + "Effects:");
            for (String effect : effects) {
                crystalLore.add(ChatColor.GRAY + "• " + CrystalManager.beautifyEffectName(effect));
            }
        } else {
            crystalLore.add(ChatColor.RED + "✗ No Active Crystal");
            crystalLore.add("");
            crystalLore.add(ChatColor.GRAY + "Place a crystal in your off-hand");
            crystalLore.add(ChatColor.GRAY + "to activate its effects");
        }
        
        crystalMeta.setLore(crystalLore);
        activeCrystal.setItemMeta(crystalMeta);
        inventory.setItem(ACTIVE_CRYSTAL_SLOT, activeCrystal);
        
        // Next Upgrade
        ItemStack nextUpgrade = new ItemStack(Material.NETHER_STAR);
        ItemMeta upgradeMeta = nextUpgrade.getItemMeta();
        upgradeMeta.setDisplayName(ChatColor.GOLD + "Next Upgrade");
        List<String> upgradeLore = new ArrayList<>();
        
        String nextUpgradeName = findNextAffordableUpgrade(player);
        if (nextUpgradeName != null) {
            upgradeLore.add(ChatColor.GREEN + "Available Upgrade:");
            upgradeLore.add(ChatColor.WHITE + nextUpgradeName);
        } else {
            upgradeLore.add(ChatColor.RED + "No upgrades available");
            upgradeLore.add(ChatColor.GRAY + "Level up to unlock more");
        }
        
        upgradeMeta.setLore(upgradeLore);
        nextUpgrade.setItemMeta(upgradeMeta);
        inventory.setItem(NEXT_UPGRADE_SLOT, nextUpgrade);
        
        // Economy Status
        ItemStack economyInfo = new ItemStack(Material.GOLD_INGOT);
        ItemMeta economyMeta = economyInfo.getItemMeta();
        economyMeta.setDisplayName(ChatColor.YELLOW + "Economy Status");
        List<String> economyLore = new ArrayList<>();
        
        if (ArcaniteCrystals.getEconomy() != null) {
            double balance = getPlayerBalance();
            economyLore.add(ChatColor.GREEN + "Balance: $" + String.format("%.2f", balance));
        } else {
            economyLore.add(ChatColor.RED + "Economy not available");
        }
        
        economyMeta.setLore(economyLore);
        economyInfo.setItemMeta(economyMeta);
        inventory.setItem(ECONOMY_SLOT, economyInfo);
        
        // Cooldown Status
        ItemStack cooldownInfo = new ItemStack(Material.CLOCK);
        ItemMeta cooldownMeta = cooldownInfo.getItemMeta();
        cooldownMeta.setDisplayName(ChatColor.AQUA + "Cooldown Status");
        List<String> cooldownLore = new ArrayList<>();
        
        long cooldown = PlayerDataManager.getCooldown(playerId);
        if (cooldown > System.currentTimeMillis()) {
            long remaining = (cooldown - System.currentTimeMillis()) / 1000;
            cooldownLore.add(ChatColor.RED + "✗ On Cooldown");
            cooldownLore.add(ChatColor.GRAY + "Time Remaining: " + formatTime(remaining));
        } else {
            cooldownLore.add(ChatColor.GREEN + "✓ Ready");
            cooldownLore.add(ChatColor.GRAY + "No active cooldowns");
        }
        
        cooldownMeta.setLore(cooldownLore);
        cooldownInfo.setItemMeta(cooldownMeta);
        inventory.setItem(COOLDOWN_SLOT, cooldownInfo);
    }
    
    private void addRecentActivity() {
        // TODO: Implement recent activity tracking
    }
    
    private void fillBorders() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName(" ");
        border.setItemMeta(meta);
        
        // Fill top and bottom rows
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(45 + i, border);
        }
        
        // Fill sides
        for (int i = 0; i < 5; i++) {
            inventory.setItem(i * 9, border);
            inventory.setItem(i * 9 + 8, border);
        }
    }
    
    private ItemStack createHeaderItem(String title) {
        ItemStack header = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = header.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_PURPLE + title);
        header.setItemMeta(meta);
        return header;
    }
    
    private ItemStack createSeparatorItem(String title) {
        ItemStack separator = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = separator.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + title);
        separator.setItemMeta(meta);
        return separator;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);
        
        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        switch (slot) {
            case TALENTS_SLOT -> new TalentMenu(clicker).open();
            case CRYSTALS_SLOT -> new CrystalInventoryGUI(clicker).open();
            case WORKSHOP_SLOT -> new EnhancementWorkshopGUI(clicker).open();
            case PROGRESS_SLOT -> new ProgressDashboardGUI(clicker).open();
            case STATS_SLOT -> StatisticsMenu.openMenu(clicker);
            case HELP_SLOT -> new HelpSystemGUI(clicker).open();
            case ADMIN_SLOT -> {
                if (clicker.hasPermission("arcanite.admin")) {
                    new AdminPanelGUI(clicker).open();
                }
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
    
    // Utility methods
    private int getTotalUpgradeCount() {
        // TODO: Implement upgrade count
        return 0;
    }
    
    private int countPlayerCrystals(Player player) {
        // TODO: Implement crystal counting
        return 0;
    }
    
    private String getAvailableEnhancements(Player player) {
        int level = PlayerDataManager.getLevel(player.getUniqueId());
        Set<String> unlocked = PlayerDataManager.getUnlockedUpgrades(player.getUniqueId());
        
        if (level < 3) {
            return ChatColor.RED + "Requires Level 3";
        }
        
        if (unlocked.isEmpty()) {
            return ChatColor.RED + "No upgrades unlocked";
        }
        
        return ChatColor.GREEN.toString() + unlocked.size() + " upgrades available";
    }
    
    private String getStatistic(UUID playerId, String stat) {
        // TODO: Implement statistics
        return "0";
    }
    
    private String findNextAffordableUpgrade(Player player) {
        // TODO: Implement upgrade finding
        return null;
    }
    
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    private double getPlayerBalance() {
        // TODO: Implement balance retrieval
        return 0.0;
    }
} 