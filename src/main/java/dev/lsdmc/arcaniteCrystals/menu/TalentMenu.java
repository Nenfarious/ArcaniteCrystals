package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.manager.LevelManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simplified Talent Menu with clear tier-based organization
 * 
 * Layout (54 slots = 6 rows × 9 columns):
 * 
 * Row 0: [BORDER] [TIER_1_HEADER] [BORDER] [TIER_2_HEADER] [BORDER] [TIER_3_HEADER] [BORDER] [INFO] [CLOSE]
 * Row 1: [BORDER] [TIER_1_UPG_1] [BORDER] [TIER_2_UPG_1] [BORDER] [TIER_3_UPG_1] [BORDER] [STATS] [HELP]
 * Row 2: [BORDER] [TIER_1_UPG_2] [BORDER] [TIER_2_UPG_2] [BORDER] [TIER_3_UPG_2] [BORDER] [LEVEL] [MONEY]
 * Row 3: [BORDER] [TIER_1_UPG_3] [BORDER] [TIER_2_UPG_3] [BORDER] [TIER_3_UPG_3] [BORDER] [SLOTS] [SHOP]
 * Row 4: [BORDER] [TIER_1_UPG_4] [BORDER] [TIER_2_UPG_4] [BORDER] [TIER_3_UPG_4] [BORDER] [RESET] [EXIT]
 * Row 5: [=================== BOTTOM_BORDER ===================]
 */
public class TalentMenu implements InventoryHolder, Listener {
    
    // Simple, predictable layout
    private static final int MENU_SIZE = 54;
    
    // Tier columns (each tier gets 2 columns including borders)
    private static final int TIER_1_COLUMN = 1;  // Slots: 1, 10, 19, 28, 37
    private static final int TIER_2_COLUMN = 3;  // Slots: 3, 12, 21, 30, 39  
    private static final int TIER_3_COLUMN = 5;  // Slots: 5, 14, 23, 32, 41
    
    // Utility column (right side)
    private static final int UTILITY_COLUMN = 7; // Slots: 7, 16, 25, 34, 43
    private static final int ACTION_COLUMN = 8;  // Slots: 8, 17, 26, 35, 44
    
    // Maximum upgrades per tier that fit in 4 rows
    private static final int MAX_UPGRADES_PER_TIER = 4;
    
    private final Player player;
    private final UUID playerId;
    private final Inventory inventory;
    private volatile boolean isOpen = false;
    
    // Simple tracking - just what we need
    private final Map<Integer, UpgradeSlot> upgradeSlots = new HashMap<>();
    
    public TalentMenu(Player player) {
        this.player = player;
        this.playerId = player.getUniqueId();
        this.inventory = Bukkit.createInventory(this, MENU_SIZE, 
            ChatColor.DARK_PURPLE + "✦ Crystal Talents ✦");
        
        buildSimpleLayout();
    }
    
    /**
     * Build the simplified, intuitive layout
     */
    private void buildSimpleLayout() {
        // 1. Create clean borders
        fillBorders();
        
        // 2. Add tier headers  
        addTierHeaders();
        
        // 3. Add upgrades in organized columns
        addUpgradesByTier();
        
        // 4. Add utility panels
        addUtilityPanels();
        
        // 5. Add action buttons
        addActionButtons();
    }
    
    /**
     * Create clean, simple borders
     */
    private void fillBorders() {
        ItemStack border = createBorderItem();
        
        // Vertical borders between tiers
        for (int row = 0; row < 6; row++) {
            inventory.setItem(row * 9 + 0, border); // Left border
            inventory.setItem(row * 9 + 2, border); // Between tier 1 & 2
            inventory.setItem(row * 9 + 4, border); // Between tier 2 & 3  
            inventory.setItem(row * 9 + 6, border); // Between tier 3 & utility
        }
        
        // Bottom border
        for (int col = 0; col < 9; col++) {
            inventory.setItem(45 + col, border);
        }
    }
    
    /**
     * Add clear tier headers showing progression
     */
    private void addTierHeaders() {
        int playerLevel = PlayerDataManager.getLevel(playerId);
        int maxTier = LevelManager.getMaxTier(playerId);
        
        // Tier 1 Header
        ItemStack tier1Header = new ItemStack(Material.IRON_INGOT);
        ItemMeta tier1Meta = tier1Header.getItemMeta();
        tier1Meta.setDisplayName(ChatColor.WHITE + "⬥ Tier I - Basic ⬥");
        List<String> tier1Lore = new ArrayList<>();
        tier1Lore.add(maxTier >= 1 ? ChatColor.GREEN + "✓ UNLOCKED" : ChatColor.RED + "✗ LOCKED");
        tier1Lore.add(ChatColor.GRAY + "Foundation effects");
        tier1Lore.add(ChatColor.YELLOW + "Available from Level 1");
        tier1Meta.setLore(tier1Lore);
        tier1Header.setItemMeta(tier1Meta);
        inventory.setItem(TIER_1_COLUMN, tier1Header);
        
        // Tier 2 Header  
        ItemStack tier2Header = new ItemStack(Material.GOLD_INGOT);
        ItemMeta tier2Meta = tier2Header.getItemMeta();
        tier2Meta.setDisplayName(ChatColor.YELLOW + "⬥ Tier II - Advanced ⬥");
        List<String> tier2Lore = new ArrayList<>();
        tier2Lore.add(maxTier >= 2 ? ChatColor.GREEN + "✓ UNLOCKED" : ChatColor.RED + "✗ LOCKED");
        tier2Lore.add(ChatColor.GRAY + "Enhanced effects");
        tier2Lore.add(ChatColor.YELLOW + "Available from Level 3");
        tier2Meta.setLore(tier2Lore);
        tier2Header.setItemMeta(tier2Meta);
        inventory.setItem(TIER_2_COLUMN, tier2Header);
        
        // Tier 3 Header
        ItemStack tier3Header = new ItemStack(Material.DIAMOND);
        ItemMeta tier3Meta = tier3Header.getItemMeta();
        tier3Meta.setDisplayName(ChatColor.AQUA + "⬥ Tier III - Master ⬥");
        List<String> tier3Lore = new ArrayList<>();
        tier3Lore.add(maxTier >= 3 ? ChatColor.GREEN + "✓ UNLOCKED" : ChatColor.RED + "✗ LOCKED");
        tier3Lore.add(ChatColor.GRAY + "Ultimate effects");
        tier3Lore.add(ChatColor.YELLOW + "Available from Level 7");
        tier3Meta.setLore(tier3Lore);
        tier3Header.setItemMeta(tier3Meta);
        inventory.setItem(TIER_3_COLUMN, tier3Header);
    }
    
    /**
     * Add upgrades organized by tier in clean columns
     */
    private void addUpgradesByTier() {
        ConfigurationSection upgradesConfig = ConfigManager.getUpgradesConfig()
            .getConfigurationSection("upgrades");
        if (upgradesConfig == null) return;
        
        // Organize upgrades by tier
        Map<Integer, List<String>> upgradesByTier = new HashMap<>();
        for (String upgradeId : upgradesConfig.getKeys(false)) {
            ConfigurationSection upgrade = upgradesConfig.getConfigurationSection(upgradeId);
            if (upgrade != null) {
                int tier = upgrade.getInt("tier", 1);
                upgradesByTier.computeIfAbsent(tier, k -> new ArrayList<>()).add(upgradeId);
            }
        }
        
        // Add upgrades to their respective columns
        addTierUpgrades(1, upgradesByTier.getOrDefault(1, new ArrayList<>()), TIER_1_COLUMN);
        addTierUpgrades(2, upgradesByTier.getOrDefault(2, new ArrayList<>()), TIER_2_COLUMN);  
        addTierUpgrades(3, upgradesByTier.getOrDefault(3, new ArrayList<>()), TIER_3_COLUMN);
    }
    
    /**
     * Add upgrades for a specific tier in its column
     */
    private void addTierUpgrades(int tier, List<String> upgrades, int column) {
        // Sort upgrades for consistent display
        upgrades.sort(String::compareTo);
        
        int maxTier = LevelManager.getMaxTier(playerId);
        Set<String> unlockedUpgrades = PlayerDataManager.getUnlockedUpgrades(playerId);
        
        for (int i = 0; i < Math.min(upgrades.size(), MAX_UPGRADES_PER_TIER); i++) {
            String upgradeId = upgrades.get(i);
            int slot = column + (i + 1) * 9; // Skip header row
            
            ItemStack upgradeItem = createUpgradeItem(upgradeId, tier, maxTier, unlockedUpgrades);
            inventory.setItem(slot, upgradeItem);
            
            upgradeSlots.put(slot, new UpgradeSlot(upgradeId, tier));
        }
        
        // If more than 4 upgrades in this tier, show "more" indicator
        if (upgrades.size() > MAX_UPGRADES_PER_TIER) {
            int moreSlot = column + 5 * 9; // Bottom of column
            ItemStack moreItem = createMoreUpgradesItem(tier, upgrades.size() - MAX_UPGRADES_PER_TIER);
            inventory.setItem(moreSlot, moreItem);
        }
    }
    
    /**
     * Create a clear, informative upgrade item
     */
    private ItemStack createUpgradeItem(String upgradeId, int tier, int maxTier, Set<String> unlocked) {
        ConfigurationSection upgrade = ConfigManager.getUpgradesConfig()
            .getConfigurationSection("upgrades." + upgradeId);
        if (upgrade == null) return createErrorItem(upgradeId);
        
        boolean isUnlocked = unlocked.contains(upgradeId);
        boolean canPurchase = tier <= maxTier && !isUnlocked;
        
        // Choose material based on state
        Material material;
        ChatColor nameColor;
        String statusIcon;
        
        if (isUnlocked) {
            material = Material.EMERALD;
            nameColor = ChatColor.GREEN;
            statusIcon = "✓";
        } else if (canPurchase) {
            material = getUpgradeMaterial(upgrade.getString("effect", ""));
            nameColor = ChatColor.YELLOW;
            statusIcon = "◆";
        } else {
            material = Material.BARRIER;
            nameColor = ChatColor.RED;
            statusIcon = "✗";
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Clean, readable name
        String effectName = beautifyEffectName(upgrade.getString("effect", upgradeId));
        meta.setDisplayName(nameColor.toString() + statusIcon + " " + effectName);
        
        // Simple, informative lore
        List<String> lore = new ArrayList<>();
        
        // Status line
        if (isUnlocked) {
            lore.add(ChatColor.GREEN.toString() + "✓ PURCHASED");
        } else if (canPurchase) {
            lore.add(ChatColor.YELLOW.toString() + "◆ AVAILABLE");
        } else {
            lore.add(ChatColor.RED.toString() + "✗ LOCKED");
            lore.add(ChatColor.DARK_RED.toString() + "Requires Tier " + tier);
        }
        
        lore.add("");
        
        // Effect description
        String effect = upgrade.getString("effect", "");
        int amplifier = upgrade.getInt("amplifier", 0);
        lore.add(ChatColor.BLUE.toString() + "Effect: " + ChatColor.WHITE.toString() + beautifyEffectName(effect));
        lore.add(ChatColor.BLUE.toString() + "Level: " + ChatColor.WHITE.toString() + (amplifier + 1));
        
        // Purchase info
        if (canPurchase) {
            lore.add("");
            ConfigurationSection buySection = upgrade.getConfigurationSection("buy");
            if (buySection != null) {
                String cost = formatCost(buySection);
                boolean canAfford = canPlayerAfford(buySection);
                
                lore.add(ChatColor.GOLD.toString() + "Cost: " + cost);
                lore.add(canAfford ? 
                    ChatColor.GREEN.toString() + "Click to purchase!" : 
                    ChatColor.RED.toString() + "Cannot afford");
            }
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Add utility panels showing player info
     */
    private void addUtilityPanels() {
        int playerLevel = PlayerDataManager.getLevel(playerId);
        int maxTier = LevelManager.getMaxTier(playerId);
        int slots = LevelManager.getSlots(playerId);
        Set<String> upgrades = PlayerDataManager.getUnlockedUpgrades(playerId);
        
        // Player Info Panel
        ItemStack infoPanel = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta infoMeta = infoPanel.getItemMeta();
        infoMeta.setDisplayName(ChatColor.GOLD + "Player Information");
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.BLUE + "Level: " + ChatColor.WHITE + playerLevel + "/10");
        infoLore.add(ChatColor.BLUE + "Max Tier: " + ChatColor.WHITE + maxTier + "/3");
        infoLore.add(ChatColor.BLUE + "Crystal Slots: " + ChatColor.WHITE + slots);
        infoLore.add(ChatColor.BLUE + "Upgrades: " + ChatColor.WHITE + upgrades.size());
        infoMeta.setLore(infoLore);
        infoPanel.setItemMeta(infoMeta);
        inventory.setItem(UTILITY_COLUMN, infoPanel);
        
        // Progress Panel
        ItemStack progressPanel = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta progressMeta = progressPanel.getItemMeta();
        progressMeta.setDisplayName(ChatColor.GREEN + "Progress");
        List<String> progressLore = new ArrayList<>();
        
        double progressPercent = (double) upgrades.size() / getTotalUpgradeCount() * 100;
        progressLore.add(ChatColor.YELLOW + "Completion: " + String.format("%.1f%%", progressPercent));
        progressLore.add(createProgressBar(progressPercent));
        
        if (playerLevel < 10) {
            progressLore.add("");
            progressLore.add(ChatColor.GRAY + "Next level unlocks:");
            progressLore.add(ChatColor.AQUA + "• Higher tier upgrades");
            progressLore.add(ChatColor.AQUA + "• More crystal slots");
        }
        
        progressMeta.setLore(progressLore);
        progressPanel.setItemMeta(progressMeta);
        inventory.setItem(UTILITY_COLUMN + 9, progressPanel);
        
        // Economy Panel
        ItemStack economyPanel = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta economyMeta = economyPanel.getItemMeta();
        economyMeta.setDisplayName(ChatColor.YELLOW + "Economy");
        List<String> economyLore = new ArrayList<>();
        
        // Show player's current resources
        if (hasEconomy()) {
            double balance = getPlayerBalance();
            economyLore.add(ChatColor.GREEN + "Balance: $" + String.format("%.2f", balance));
        }
        
        int playerExp = player.getTotalExperience();
        economyLore.add(ChatColor.BLUE + "Experience: " + playerExp);
        
        economyMeta.setLore(economyLore);
        economyPanel.setItemMeta(economyMeta);
        inventory.setItem(UTILITY_COLUMN + 18, economyPanel);
    }
    
    /**
     * Add simple action buttons
     */
    private void addActionButtons() {
        // Level Up Button
        ItemStack levelUpBtn = new ItemStack(Material.NETHER_STAR);
        ItemMeta levelUpMeta = levelUpBtn.getItemMeta();
        levelUpMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Level Up");
        
        List<String> levelUpLore = new ArrayList<>();
        int currentLevel = PlayerDataManager.getLevel(playerId);
        if (currentLevel >= 10) {
            levelUpLore.add(ChatColor.GOLD + "MAX LEVEL REACHED");
        } else {
            LevelManager.LevelConfig nextLevelConfig = LevelManager.getConfigForLevel(currentLevel + 1);
            if (nextLevelConfig != null) {
                RequirementChecker checker = new RequirementChecker(player, nextLevelConfig);
                List<String> missing = checker.getMissing();
                
                if (missing.isEmpty()) {
                    levelUpLore.add(ChatColor.GREEN + "Ready to level up!");
                    levelUpLore.add(ChatColor.YELLOW + "Click to advance!");
            } else {
                    levelUpLore.add(ChatColor.RED + "Requirements not met:");
                    for (String req : missing) {
                        levelUpLore.add(ChatColor.GRAY + "• " + req);
                    }
                }
            }
        }
        
        levelUpMeta.setLore(levelUpLore);
        levelUpBtn.setItemMeta(levelUpMeta);
        inventory.setItem(ACTION_COLUMN, levelUpBtn);
        
        // Help Button
        ItemStack helpBtn = new ItemStack(Material.BOOK);
        ItemMeta helpMeta = helpBtn.getItemMeta();
        helpMeta.setDisplayName(ChatColor.AQUA + "Help");
        List<String> helpLore = new ArrayList<>();
        helpLore.add(ChatColor.GRAY + "How to use this menu:");
        helpLore.add(ChatColor.YELLOW + "• Click upgrades to purchase");
        helpLore.add(ChatColor.YELLOW + "• Level up to unlock tiers");
        helpLore.add(ChatColor.YELLOW + "• Higher tiers = better effects");
        helpMeta.setLore(helpLore);
        helpBtn.setItemMeta(helpMeta);
        inventory.setItem(ACTION_COLUMN + 9, helpBtn);
        
        // Close Button
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Close Menu");
        closeBtn.setItemMeta(closeMeta);
        inventory.setItem(ACTION_COLUMN + 27, closeBtn);
    }
    
    /**
     * Handle inventory clicks with simple logic
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player clicker = (Player) event.getWhoClicked();
        
        int slot = event.getRawSlot();
        
        // Handle upgrade purchases
        UpgradeSlot upgradeSlot = upgradeSlots.get(slot);
        if (upgradeSlot != null) {
            handleUpgradePurchase(clicker, upgradeSlot);
            return;
        }
        
        // Handle action buttons
        if (slot == ACTION_COLUMN) { // Level up button
            clicker.performCommand("levelup");
            // Refresh menu after level up
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (clicker.isOnline()) {
                        new TalentMenu(clicker).open();
                    }
                }
            }.runTaskLater(ArcaniteCrystals.getInstance(), 5L);
        } else if (slot == ACTION_COLUMN + 9) { // Help button
            sendHelpMessage(clicker);
        } else if (slot == ACTION_COLUMN + 27) { // Close button
            clicker.closeInventory();
        }
    }
    
    /**
     * Simplified purchase handling
     */
    private void handleUpgradePurchase(Player purchaser, UpgradeSlot upgradeSlot) {
        String upgradeId = upgradeSlot.upgradeId;
        
        // Simple validation
        Set<String> unlocked = PlayerDataManager.getUnlockedUpgrades(purchaser.getUniqueId());
        if (unlocked.contains(upgradeId)) {
            purchaser.sendMessage(ChatColor.YELLOW + "You already have this upgrade!");
            return;
        }
        
        int maxTier = LevelManager.getMaxTier(purchaser.getUniqueId());
        if (upgradeSlot.tier > maxTier) {
            purchaser.sendMessage(ChatColor.RED + "You need to reach Tier " + upgradeSlot.tier + " first!");
            return;
        }
        
        // Process purchase
        ConfigurationSection upgrade = ConfigManager.getUpgradesConfig()
            .getConfigurationSection("upgrades." + upgradeId);
        if (upgrade == null) {
            purchaser.sendMessage(ChatColor.RED + "Upgrade configuration error!");
            return;
        }
        
        ConfigurationSection buySection = upgrade.getConfigurationSection("buy");
        if (buySection == null) {
            purchaser.sendMessage(ChatColor.RED + "Purchase configuration missing!");
            return;
        }
        
        if (processPurchase(purchaser, buySection)) {
            // Purchase successful
            PlayerDataManager.unlockUpgrade(purchaser.getUniqueId(), upgradeId);
            
            // Success feedback
            String effectName = beautifyEffectName(upgrade.getString("effect", upgradeId));
            MessageManager.sendUpgradeUnlocked(purchaser, effectName, upgradeSlot.tier);
            SoundManager.playUpgradeUnlockSound(purchaser);
            ParticleManager.playUpgradeEffect(purchaser);
            
            // Refresh menu
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (purchaser.isOnline()) {
                        new TalentMenu(purchaser).open();
                    }
                }
            }.runTaskLater(ArcaniteCrystals.getInstance(), 10L);
        }
    }
    
    // Helper classes and methods
    private static class UpgradeSlot {
        final String upgradeId;
        final int tier;
        
        UpgradeSlot(String upgradeId, int tier) {
            this.upgradeId = upgradeId;
            this.tier = tier;
        }
    }
    
    /**
     * Open the simplified menu
     */
    public void open() {
        if (!isOpen) {
            player.openInventory(inventory);
            isOpen = true;
            Bukkit.getPluginManager().registerEvents(this, ArcaniteCrystals.getInstance());
        }
    }
    
    /**
     * Clean up when menu closes
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this && isOpen) {
            isOpen = false;
            HandlerList.unregisterAll(this);
        }
    }
    
    // Utility methods for formatting and display
    private String beautifyEffectName(String effect) {
        return switch (effect.toLowerCase()) {
            case "speed" -> "Swift Movement";
            case "regeneration" -> "Life Regeneration";
            case "jump_boost" -> "Enhanced Jumping";
            case "haste" -> "Mining Speed";
            case "strength" -> "Combat Power";
            case "damage_resistance" -> "Damage Resistance";
            default -> effect.replace("_", " ");
        };
    }
    
    private Material getUpgradeMaterial(String effect) {
        return switch (effect.toLowerCase()) {
            case "speed" -> Material.SUGAR;
            case "regeneration" -> Material.GOLDEN_APPLE;
            case "jump_boost" -> Material.RABBIT_FOOT;
            case "haste" -> Material.GOLDEN_PICKAXE;
            case "strength" -> Material.IRON_SWORD;
            case "damage_resistance" -> Material.IRON_CHESTPLATE;
            default -> Material.POTION;
        };
    }
    
    private String createProgressBar(double percent) {
        int bars = 20;
        int filled = (int) (percent / 100.0 * bars);
        
        StringBuilder bar = new StringBuilder();
        bar.append(ChatColor.GRAY).append("[");
        
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                bar.append(ChatColor.GREEN).append("█");
            } else {
                bar.append(ChatColor.DARK_GRAY).append("░");
            }
        }
        
        bar.append(ChatColor.GRAY).append("]");
        return bar.toString();
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Create a simple border item
     */
    private ItemStack createBorderItem() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName(" ");
        border.setItemMeta(meta);
        return border;
    }

    /**
     * Create an item showing more upgrades are available
     */
    private ItemStack createMoreUpgradesItem(int tier, int remaining) {
        ItemStack more = new ItemStack(Material.ARROW);
        ItemMeta meta = more.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "More Tier " + tier + " Upgrades");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "There are " + remaining + " more upgrades");
        lore.add(ChatColor.GRAY + "in this tier");
        meta.setLore(lore);
        more.setItemMeta(meta);
        return more;
    }
    
    /**
     * Create an error item for missing configurations
     */
    private ItemStack createErrorItem(String upgradeId) {
        ItemStack error = new ItemStack(Material.BARRIER);
        ItemMeta meta = error.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Error");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Missing configuration for:");
        lore.add(ChatColor.RED + upgradeId);
        meta.setLore(lore);
        error.setItemMeta(meta);
        return error;
    }
    
    /**
     * Format the cost of an upgrade
     */
    private String formatCost(ConfigurationSection buySection) {
        String type = buySection.getString("type", "money");
        int amount = buySection.getInt("amount", 0);
        
        return switch (type.toLowerCase()) {
            case "money" -> ChatColor.GREEN.toString() + "$" + String.format("%,d", amount);
            case "exp" -> ChatColor.BLUE.toString() + String.format("%,d", amount) + " XP";
            case "item" -> {
                String itemId = buySection.getString("id", "DIAMOND");
                String itemName = itemId.replace("_", " ");
                itemName = itemName.substring(0, 1).toUpperCase() + itemName.substring(1).toLowerCase();
                yield ChatColor.YELLOW.toString() + amount + "x " + itemName;
            }
            default -> amount + " " + type;
        };
    }
    
    /**
     * Check if player can afford an upgrade
     */
    private boolean canPlayerAfford(ConfigurationSection buySection) {
        String type = buySection.getString("type", "money");
        int amount = buySection.getInt("amount", 0);
        
        return switch (type.toLowerCase()) {
            case "money" -> hasEconomy() && getPlayerBalance() >= amount;
            case "exp" -> player.getTotalExperience() >= amount;
            case "item" -> {
                String itemId = buySection.getString("id", "DIAMOND");
                Material material = Material.matchMaterial(itemId);
                if (material != null) {
                    yield player.getInventory().containsAtLeast(new ItemStack(material), amount);
                }
                yield false;
            }
            default -> false;
        };
    }
    
    /**
     * Get total number of available upgrades
     */
    private int getTotalUpgradeCount() {
        ConfigurationSection upgradesConfig = ConfigManager.getUpgradesConfig()
            .getConfigurationSection("upgrades");
        return upgradesConfig != null ? upgradesConfig.getKeys(false).size() : 0;
    }

    /**
     * Check if economy is available
     */
    private boolean hasEconomy() {
        return ArcaniteCrystals.getInstance().getServer().getServicesManager()
            .getRegistration(Economy.class) != null;
    }

    /**
     * Get player's current balance
     */
    private double getPlayerBalance() {
        if (!hasEconomy()) return 0;
        Economy economy = ArcaniteCrystals.getInstance().getServer().getServicesManager()
            .getRegistration(Economy.class).getProvider();
        return economy.getBalance(player);
    }

    /**
     * Send help message to player
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Crystal Talents Help ===");
        player.sendMessage(ChatColor.YELLOW + "• Click upgrades to purchase them");
        player.sendMessage(ChatColor.YELLOW + "• Level up to unlock higher tiers");
        player.sendMessage(ChatColor.YELLOW + "• Higher tiers have better effects");
        player.sendMessage(ChatColor.YELLOW + "• Use /levelup to see requirements");
    }

    /**
     * Process the purchase transaction
     */
    private boolean processPurchase(Player purchaser, ConfigurationSection buySection) {
        String type = buySection.getString("type", "money");
        int amount = buySection.getInt("amount", 0);
        
        return switch (type.toLowerCase()) {
            case "money" -> {
                if (!hasEconomy()) {
                    purchaser.sendMessage(ChatColor.RED + "Economy system not available!");
                    yield false;
                }
                Economy economy = ArcaniteCrystals.getInstance().getServer().getServicesManager()
                    .getRegistration(Economy.class).getProvider();
                if (!economy.has(purchaser, amount)) {
                    purchaser.sendMessage(ChatColor.RED + "You need " + ChatColor.GOLD + "$" + 
                        String.format("%,d", amount) + ChatColor.RED + "!");
                    yield false;
                }
                economy.withdrawPlayer(purchaser, amount);
                yield true;
            }
            case "exp" -> {
                if (purchaser.getTotalExperience() < amount) {
                    purchaser.sendMessage(ChatColor.RED + "You need " + ChatColor.BLUE + 
                        String.format("%,d", amount) + " XP" + ChatColor.RED + "!");
                    yield false;
                }
                purchaser.giveExp(-amount);
                yield true;
            }
            case "item" -> {
                String itemId = buySection.getString("id", "DIAMOND");
                Material material = Material.matchMaterial(itemId);
                if (material == null) {
                    purchaser.sendMessage(ChatColor.RED + "Invalid item configuration!");
                    yield false;
                }
                ItemStack required = new ItemStack(material, amount);
                if (!purchaser.getInventory().containsAtLeast(required, amount)) {
                    String itemName = itemId.replace("_", " ");
                    itemName = itemName.substring(0, 1).toUpperCase() + itemName.substring(1).toLowerCase();
                    purchaser.sendMessage(ChatColor.RED + "You need " + ChatColor.YELLOW + 
                        amount + "x " + itemName + ChatColor.RED + "!");
                    yield false;
                }
                purchaser.getInventory().removeItem(required);
                yield true;
            }
            default -> {
                purchaser.sendMessage(ChatColor.RED + "Invalid purchase configuration!");
                yield false;
            }
        };
    }
    
    /**
     * Helper class to check level up requirements
     */
    private static class RequirementChecker {
        private final Player player;
        private final LevelManager.LevelConfig config;

        RequirementChecker(Player player, LevelManager.LevelConfig config) {
            this.player = player;
            this.config = config;
        }

        List<String> getMissing() {
            List<String> missing = new ArrayList<>();
            
            // Check money requirement
            int moneyRequired = config.getMoneyRequirement();
            if (moneyRequired > 0) {
                Economy economy = ArcaniteCrystals.getInstance().getServer().getServicesManager()
                    .getRegistration(Economy.class).getProvider();
                if (economy != null && economy.getBalance(player) < moneyRequired) {
                    missing.add("Need $" + String.format("%,d", moneyRequired));
                }
            }
            
            // Check experience requirement
            int expRequired = config.getKillsRequirement(); // Using kills as exp requirement
            if (expRequired > 0 && player.getTotalExperience() < expRequired) {
                missing.add("Need " + String.format("%,d", expRequired) + " XP");
            }
            
            // Check time requirement
            long timeRequired = config.getTimeRequirementMs();
            if (timeRequired > 0) {
                long hours = timeRequired / 3_600_000L;
                missing.add("Need " + hours + " hours of playtime");
            }
            
            return missing;
        }
    }
}


