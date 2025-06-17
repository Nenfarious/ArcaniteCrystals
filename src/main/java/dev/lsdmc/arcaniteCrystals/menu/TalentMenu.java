package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import dev.lsdmc.arcaniteCrystals.menu.LevelOverviewGUI;
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

/**
 * Redesigned Talent Menu with proper pagination and tier management
 * 
 * Features:
 * 1. Overview Mode: Shows tier summaries and featured upgrades
 * 2. Tier Detail Mode: Shows all upgrades for a specific tier with pagination
 * 3. Proper navigation between modes
 * 4. Handles unlimited upgrades per tier
 */
public class TalentMenu implements InventoryHolder, Listener {
    
    private static final int MENU_SIZE = 54;
    
    // Display modes
    private enum DisplayMode {
        OVERVIEW,    // Show tier summaries
        TIER_DETAIL  // Show specific tier upgrades
    }
    
    // Overview mode layout
    private static final int TIER_1_SLOT = 11;
    private static final int TIER_2_SLOT = 13;
    private static final int TIER_3_SLOT = 15;
    
    // Navigation slots
    private static final int PREV_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int BACK_SLOT = 49;
    private static final int INFO_SLOT = 4;
    private static final int HELP_SLOT = 50;
    
    // Upgrade display area for detail mode (28 slots = 4 rows x 7 columns)
    private static final int[] UPGRADE_SLOTS_DETAIL = {
        10, 11, 12, 13, 14, 15, 16,  // Row 1
        19, 20, 21, 22, 23, 24, 25,  // Row 2  
        28, 29, 30, 31, 32, 33, 34,  // Row 3
        37, 38, 39, 40, 41, 42, 43   // Row 4
    };
    
    // Featured upgrades around tier cards in overview mode
    private static final int[] UPGRADE_SLOTS_OVERVIEW = {
        19, 20, 21, 28, 29, 30, 37, 38, 39  // Around center
    };
    
    private final Player player;
    private final UUID playerId;
    private final Inventory inventory;
    private volatile boolean isOpen = false;
    
    // State management
    private DisplayMode currentMode = DisplayMode.OVERVIEW;
    private int currentTier = 1;
    private int currentPage = 0;
    private final Map<Integer, UpgradeSlot> upgradeSlots = new HashMap<>();
    
    // Cached data
    private Map<Integer, List<String>> upgradesByTier;
    private Set<String> unlockedUpgrades;
    private int maxTier;
    
    public TalentMenu(Player player) {
        this.player = player;
        this.playerId = player.getUniqueId();
        this.inventory = Bukkit.createInventory(this, MENU_SIZE, 
            ChatColor.DARK_PURPLE + "✦ Crystal Talents ✦");
        
        loadData();
        buildDisplay();
    }
    
    /**
     * Load and cache all necessary data
     */
    private void loadData() {
        unlockedUpgrades = PlayerDataManager.getUnlockedUpgrades(playerId);
        maxTier = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getMaxTier(playerId);
        
        // Organize upgrades by tier
        upgradesByTier = new HashMap<>();
        ConfigurationSection upgradesConfig = ConfigManager.getUpgradesConfig()
            .getConfigurationSection("upgrades");
        
        if (upgradesConfig != null) {
            for (String upgradeId : upgradesConfig.getKeys(false)) {
                ConfigurationSection upgrade = upgradesConfig.getConfigurationSection(upgradeId);
                if (upgrade != null) {
                    int tier = upgrade.getInt("tier", 1);
                    upgradesByTier.computeIfAbsent(tier, k -> new ArrayList<>()).add(upgradeId);
                }
            }
            
            // Sort upgrades within each tier
            upgradesByTier.values().forEach(list -> list.sort(String::compareTo));
        }
    }
    
    /**
     * Build the appropriate display based on current mode
     */
    private void buildDisplay() {
        inventory.clear();
        upgradeSlots.clear();
        
        if (currentMode == DisplayMode.OVERVIEW) {
            buildOverviewDisplay();
        } else {
            buildTierDetailDisplay();
        }
        
        addNavigationButtons();
    }
    
    /**
     * Build overview display showing tier summaries
     */
    private void buildOverviewDisplay() {
        // Fill background
        fillBackground();
        
        // Add tier cards
        addTierCard(1, TIER_1_SLOT);
        addTierCard(2, TIER_2_SLOT);
        addTierCard(3, TIER_3_SLOT);
        
        // Add player info
        addPlayerInfoPanel();
        
        // Show some featured upgrades around the center
        addFeaturedUpgrades();
    }
    
    /**
     * Build detailed view for a specific tier
     */
    private void buildTierDetailDisplay() {
        // Fill background
        fillBackground();
        
        // Add tier header
        addTierHeader();
        
        // Add upgrades for current tier with pagination
        addTierUpgradesDetailed();
        
        // Add pagination info
        addPaginationInfo();
    }
    
    /**
     * Add a tier card showing tier summary
     */
    private void addTierCard(int tier, int slot) {
        List<String> tierUpgrades = upgradesByTier.getOrDefault(tier, new ArrayList<>());
        long unlockedCount = tierUpgrades.stream()
            .filter(unlockedUpgrades::contains)
            .count();
        
        boolean tierUnlocked = maxTier >= tier;
        
        Material material = switch (tier) {
            case 1 -> Material.IRON_INGOT;
            case 2 -> Material.GOLD_INGOT;
            case 3 -> Material.DIAMOND;
            default -> Material.STONE;
        };
        
        ChatColor tierColor = switch (tier) {
            case 1 -> ChatColor.WHITE;
            case 2 -> ChatColor.YELLOW;
            case 3 -> ChatColor.AQUA;
            default -> ChatColor.GRAY;
        };
        
        ItemStack tierCard = new ItemStack(material);
        ItemMeta meta = tierCard.getItemMeta();
        meta.setDisplayName(tierColor + "⬥ Tier " + tier + " ⬥");
        
        List<String> lore = new ArrayList<>();
        lore.add(tierUnlocked ? ChatColor.GREEN + "✓ UNLOCKED" : ChatColor.RED + "✗ LOCKED");
        lore.add("");
        lore.add(ChatColor.GRAY + "Progress: " + ChatColor.AQUA + unlockedCount + 
                ChatColor.GRAY + "/" + tierUpgrades.size());
        
        if (!tierUpgrades.isEmpty()) {
            double progress = (double) unlockedCount / tierUpgrades.size() * 100;
            lore.add(createProgressBar(progress));
        }
        
        lore.add("");
        if (tierUnlocked) {
            lore.add(ChatColor.YELLOW + "Click to view " + tierUpgrades.size() + " upgrades");
        } else {
            String requirement = switch (tier) {
                case 2 -> "Level 3";
                case 3 -> "Level 7";
                default -> "Level 1";
            };
            lore.add(ChatColor.RED + "Requires " + requirement);
        }
        
        meta.setLore(lore);
        tierCard.setItemMeta(meta);
        inventory.setItem(slot, tierCard);
    }
    
    /**
     * Add featured upgrades around the center
     */
    private void addFeaturedUpgrades() {
        List<String> featured = new ArrayList<>();
        
        // Add some unlocked upgrades and available upgrades
        for (int tier = 1; tier <= 3; tier++) {
            if (maxTier >= tier) {
                List<String> tierUpgrades = upgradesByTier.getOrDefault(tier, new ArrayList<>());
                
                // Add unlocked upgrades (limit 2 per tier)
                tierUpgrades.stream()
                    .filter(unlockedUpgrades::contains)
                    .limit(2)
                    .forEach(featured::add);
                
                // Add available upgrades (limit 1 per tier)
                tierUpgrades.stream()
                    .filter(id -> !unlockedUpgrades.contains(id))
                    .limit(1)
                    .forEach(featured::add);
            }
        }
        
        // Display featured upgrades
        for (int i = 0; i < Math.min(featured.size(), UPGRADE_SLOTS_OVERVIEW.length); i++) {
            String upgradeId = featured.get(i);
            int slot = UPGRADE_SLOTS_OVERVIEW[i];
            
            ItemStack upgradeItem = createUpgradeItem(upgradeId, true);
            inventory.setItem(slot, upgradeItem);
            
            ConfigurationSection upgrade = ConfigManager.getUpgradesConfig()
                .getConfigurationSection("upgrades." + upgradeId);
            if (upgrade != null) {
                int tier = upgrade.getInt("tier", 1);
                upgradeSlots.put(slot, new UpgradeSlot(upgradeId, tier));
            }
        }
    }
    
    /**
     * Add tier header for detail view
     */
    private void addTierHeader() {
        List<String> tierUpgrades = upgradesByTier.getOrDefault(currentTier, new ArrayList<>());
        long unlockedCount = tierUpgrades.stream()
            .filter(unlockedUpgrades::contains)
            .count();
        
        Material material = switch (currentTier) {
            case 1 -> Material.IRON_BLOCK;
            case 2 -> Material.GOLD_BLOCK;
            case 3 -> Material.DIAMOND_BLOCK;
            default -> Material.STONE;
        };
        
        ChatColor tierColor = switch (currentTier) {
            case 1 -> ChatColor.WHITE;
            case 2 -> ChatColor.YELLOW;
            case 3 -> ChatColor.AQUA;
            default -> ChatColor.GRAY;
        };
        
        ItemStack header = new ItemStack(material);
        ItemMeta meta = header.getItemMeta();
        meta.setDisplayName(tierColor + "⬥ Tier " + currentTier + " Upgrades ⬥");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Progress: " + ChatColor.AQUA + unlockedCount + 
                ChatColor.GRAY + "/" + tierUpgrades.size());
        
        if (!tierUpgrades.isEmpty()) {
            double progress = (double) unlockedCount / tierUpgrades.size() * 100;
            lore.add(createProgressBar(progress));
        }
        
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click upgrades to purchase");
        
        meta.setLore(lore);
        header.setItemMeta(meta);
        inventory.setItem(INFO_SLOT, header);
    }
    
    /**
     * Add upgrades for current tier with pagination
     */
    private void addTierUpgradesDetailed() {
        List<String> tierUpgrades = upgradesByTier.getOrDefault(currentTier, new ArrayList<>());
        
        int upgradesPerPage = UPGRADE_SLOTS_DETAIL.length;
        int startIndex = currentPage * upgradesPerPage;
        int endIndex = Math.min(startIndex + upgradesPerPage, tierUpgrades.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            String upgradeId = tierUpgrades.get(i);
            int slotIndex = i - startIndex;
            int slot = UPGRADE_SLOTS_DETAIL[slotIndex];
            
            ItemStack upgradeItem = createUpgradeItem(upgradeId, false);
            inventory.setItem(slot, upgradeItem);
            
            ConfigurationSection upgrade = ConfigManager.getUpgradesConfig()
                .getConfigurationSection("upgrades." + upgradeId);
            if (upgrade != null) {
                int tier = upgrade.getInt("tier", 1);
                upgradeSlots.put(slot, new UpgradeSlot(upgradeId, tier));
            }
        }
    }
    
    /**
     * Add pagination information
     */
    private void addPaginationInfo() {
        List<String> tierUpgrades = upgradesByTier.getOrDefault(currentTier, new ArrayList<>());
        int upgradesPerPage = UPGRADE_SLOTS_DETAIL.length;
        int totalPages = (int) Math.ceil((double) tierUpgrades.size() / upgradesPerPage);
        
        if (totalPages > 1) {
            ItemStack pageInfo = new ItemStack(Material.BOOK);
            ItemMeta meta = pageInfo.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Page " + (currentPage + 1) + "/" + totalPages);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Showing " + tierUpgrades.size() + " upgrades");
            lore.add(ChatColor.YELLOW + "Use arrows to navigate");
            meta.setLore(lore);
            pageInfo.setItemMeta(meta);
            inventory.setItem(22, pageInfo);
        }
    }
    
    /**
     * Add player information panel
     */
    private void addPlayerInfoPanel() {
        int playerLevel = PlayerDataManager.getLevel(playerId);
        
        ItemStack info = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = info.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + player.getName());
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.BLUE + "Level: " + ChatColor.WHITE + playerLevel + "/10");
        lore.add(ChatColor.BLUE + "Max Tier: " + ChatColor.WHITE + maxTier + "/3");
        lore.add(ChatColor.BLUE + "Total Upgrades: " + ChatColor.WHITE + unlockedUpgrades.size());
        
        if (hasEconomy()) {
            double balance = getPlayerBalance();
            lore.add(ChatColor.GREEN + "Balance: $" + String.format("%.2f", balance));
        }
        
        meta.setLore(lore);
        info.setItemMeta(meta);
        inventory.setItem(INFO_SLOT, info);
    }
    
    /**
     * Add navigation buttons
     */
    private void addNavigationButtons() {
        // Back button (always present)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (currentMode == DisplayMode.OVERVIEW) {
            backMeta.setDisplayName(ChatColor.YELLOW + "← Back to Main Menu");
        } else {
            backMeta.setDisplayName(ChatColor.YELLOW + "← Back to Overview");
        }
        back.setItemMeta(backMeta);
        inventory.setItem(BACK_SLOT, back);
        
        // Help button
        ItemStack help = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta helpMeta = help.getItemMeta();
        helpMeta.setDisplayName(ChatColor.AQUA + "Help");
        List<String> helpLore = new ArrayList<>();
        helpLore.add(ChatColor.GRAY + "• Click tier cards to view upgrades");
        helpLore.add(ChatColor.GRAY + "• Click upgrades to purchase them");
        helpLore.add(ChatColor.GRAY + "• Level up to unlock higher tiers");
        helpMeta.setLore(helpLore);
        help.setItemMeta(helpMeta);
        inventory.setItem(HELP_SLOT, help);
        
        // Pagination buttons (only in detail mode)
        if (currentMode == DisplayMode.TIER_DETAIL) {
            List<String> tierUpgrades = upgradesByTier.getOrDefault(currentTier, new ArrayList<>());
            int upgradesPerPage = UPGRADE_SLOTS_DETAIL.length;
            int totalPages = (int) Math.ceil((double) tierUpgrades.size() / upgradesPerPage);
            
            if (totalPages > 1) {
                // Previous page
                if (currentPage > 0) {
                    ItemStack prev = new ItemStack(Material.SPECTRAL_ARROW);
                    ItemMeta prevMeta = prev.getItemMeta();
                    prevMeta.setDisplayName(ChatColor.YELLOW + "← Previous Page");
                    prev.setItemMeta(prevMeta);
                    inventory.setItem(PREV_PAGE_SLOT, prev);
                }
                
                // Next page
                if (currentPage < totalPages - 1) {
                    ItemStack next = new ItemStack(Material.SPECTRAL_ARROW);
                    ItemMeta nextMeta = next.getItemMeta();
                    nextMeta.setDisplayName(ChatColor.YELLOW + "Next Page →");
                    next.setItemMeta(nextMeta);
                    inventory.setItem(NEXT_PAGE_SLOT, next);
                }
            }
        }
    }
    
    /**
     * Create an upgrade item with full details
     */
    private ItemStack createUpgradeItem(String upgradeId, boolean compact) {
        ConfigurationSection upgrade = ConfigManager.getUpgradesConfig()
            .getConfigurationSection("upgrades." + upgradeId);
        if (upgrade == null) return createErrorItem(upgradeId);
        
        int tier = upgrade.getInt("tier", 1);
        boolean isUnlocked = unlockedUpgrades.contains(upgradeId);
        boolean canPurchase = tier <= maxTier && !isUnlocked;
        
        // Choose material and styling based on state
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
        
        // Create display name
        String effectName = beautifyEffectName(upgrade.getString("effect", upgradeId));
        meta.setDisplayName(nameColor.toString() + statusIcon + " " + effectName);
        
        // Create lore
        List<String> lore = new ArrayList<>();
        
        // Status
        if (isUnlocked) {
            lore.add(ChatColor.GREEN + "✓ PURCHASED");
        } else if (canPurchase) {
            lore.add(ChatColor.YELLOW + "◆ AVAILABLE");
        } else {
            lore.add(ChatColor.RED + "✗ LOCKED");
            lore.add(ChatColor.DARK_RED + "Requires Tier " + tier);
        }
        
        if (!compact) {
            lore.add("");
            
            // Effect details
            String effect = upgrade.getString("effect", "");
            int amplifier = upgrade.getInt("amplifier", 0);
            lore.add(ChatColor.BLUE + "Effect: " + ChatColor.WHITE + beautifyEffectName(effect));
            lore.add(ChatColor.BLUE + "Level: " + ChatColor.WHITE + (amplifier + 1));
            
            // Purchase information
            if (canPurchase) {
                lore.add("");
                ConfigurationSection buySection = upgrade.getConfigurationSection("buy");
                if (buySection != null) {
                    String cost = formatCost(buySection);
                    boolean canAfford = canPlayerAfford(buySection);
                    
                    lore.add(ChatColor.GOLD + "Cost: " + cost);
                    lore.add(canAfford ? 
                        ChatColor.GREEN + "Click to purchase!" : 
                        ChatColor.RED + "Cannot afford");
                }
            }
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Fill background with decorative items
     */
    private void fillBackground() {
        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = bg.getItemMeta();
        meta.setDisplayName(" ");
        bg.setItemMeta(meta);
        
        // Fill empty slots
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, bg);
            }
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player clicker = (Player) event.getWhoClicked();
        
        int slot = event.getRawSlot();
        
        // Handle navigation
        if (slot == BACK_SLOT) {
            if (currentMode == DisplayMode.OVERVIEW) {
                clicker.closeInventory();
                new ArcaniteMainMenu(clicker).open();
            } else {
                currentMode = DisplayMode.OVERVIEW;
                currentPage = 0;
                buildDisplay();
            }
            return;
        }
        
        if (slot == HELP_SLOT) {
            sendHelpMessage(clicker);
            return;
        }
        
        if (slot == PREV_PAGE_SLOT && currentPage > 0) {
            currentPage--;
            buildDisplay();
            SoundManager.playMenuSound(clicker, SoundManager.MenuSoundType.CLICK);
            return;
        }
        
        if (slot == NEXT_PAGE_SLOT) {
            List<String> tierUpgrades = upgradesByTier.getOrDefault(currentTier, new ArrayList<>());
            int upgradesPerPage = UPGRADE_SLOTS_DETAIL.length;
            int totalPages = (int) Math.ceil((double) tierUpgrades.size() / upgradesPerPage);
            if (currentPage < totalPages - 1) {
                currentPage++;
                buildDisplay();
                SoundManager.playMenuSound(clicker, SoundManager.MenuSoundType.CLICK);
            }
            return;
        }
        
        // Handle tier selection in overview mode
        if (currentMode == DisplayMode.OVERVIEW) {
            if (slot == TIER_1_SLOT && maxTier >= 1) {
                switchToTierDetail(1);
                return;
            }
            if (slot == TIER_2_SLOT && maxTier >= 2) {
                switchToTierDetail(2);
                return;
            }
            if (slot == TIER_3_SLOT && maxTier >= 3) {
                switchToTierDetail(3);
                return;
            }
            
            // Show locked tier message
            if (slot == TIER_2_SLOT && maxTier < 2) {
                clicker.sendMessage(ChatColor.RED + "You need to reach Level 3 to unlock Tier 2!");
                return;
            }
            if (slot == TIER_3_SLOT && maxTier < 3) {
                clicker.sendMessage(ChatColor.RED + "You need to reach Level 7 to unlock Tier 3!");
                return;
            }
        }
        
        // Handle upgrade purchases
        UpgradeSlot upgradeSlot = upgradeSlots.get(slot);
        if (upgradeSlot != null) {
            handleUpgradePurchase(clicker, upgradeSlot);
        }
    }
    
    /**
     * Switch to tier detail view
     */
    private void switchToTierDetail(int tier) {
        currentMode = DisplayMode.TIER_DETAIL;
        currentTier = tier;
        currentPage = 0;
        buildDisplay();
        SoundManager.playMenuSound(player, SoundManager.MenuSoundType.CLICK);
    }
    
    /**
     * Handle upgrade purchase
     */
    private void handleUpgradePurchase(Player purchaser, UpgradeSlot upgradeSlot) {
        String upgradeId = upgradeSlot.upgradeId;
        
        // Validation
        if (unlockedUpgrades.contains(upgradeId)) {
            purchaser.sendMessage(ChatColor.YELLOW + "You already have this upgrade!");
            return;
        }
        
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
            
            // Update cached data
            unlockedUpgrades.add(upgradeId);
            
            // Success feedback
            String effectName = beautifyEffectName(upgrade.getString("effect", upgradeId));
            MessageManager.sendUpgradeUnlocked(purchaser, effectName, upgradeSlot.tier);
            SoundManager.playUpgradeUnlockSound(purchaser);
            ParticleManager.playUpgradeEffect(purchaser);
            
            // Refresh display
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (purchaser.isOnline()) {
                        buildDisplay();
                    }
                }
            }.runTaskLater(ArcaniteCrystals.getInstance(), 5L);
        }
    }
    
    // Helper classes and utility methods
    private static class UpgradeSlot {
        final String upgradeId;
        final int tier;
        
        UpgradeSlot(String upgradeId, int tier) {
            this.upgradeId = upgradeId;
            this.tier = tier;
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
    private String beautifyEffectName(String effect) {
        return switch (effect.toLowerCase()) {
            case "speed" -> "Swift Movement";
            case "regeneration" -> "Life Regeneration";
            case "jump_boost" -> "Enhanced Jumping";
            case "haste" -> "Mining Speed";
            case "strength" -> "Combat Power";
            case "damage_resistance" -> "Damage Resistance";
            case "night_vision" -> "Night Vision";
            case "water_breathing" -> "Aquatic Breathing";
            case "fire_resistance" -> "Fire Immunity";
            default -> {
                String[] parts = effect.split("_");
                StringBuilder result = new StringBuilder();
                for (String part : parts) {
                    if (result.length() > 0) result.append(" ");
                    result.append(part.substring(0, 1).toUpperCase())
                          .append(part.substring(1).toLowerCase());
                }
                yield result.toString();
            }
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
            case "night_vision" -> Material.GOLDEN_CARROT;
            case "water_breathing" -> Material.PUFFERFISH;
            case "fire_resistance" -> Material.MAGMA_CREAM;
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
    
    private boolean hasEconomy() {
        return ArcaniteCrystals.getInstance().getServer().getServicesManager()
            .getRegistration(Economy.class) != null;
    }
    
    private double getPlayerBalance() {
        if (!hasEconomy()) return 0;
        Economy economy = ArcaniteCrystals.getInstance().getServer().getServicesManager()
            .getRegistration(Economy.class).getProvider();
        return economy.getBalance(player);
    }
    
    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Crystal Talents Help ===");
        player.sendMessage(ChatColor.YELLOW + "• Click tier cards to view upgrades");
        player.sendMessage(ChatColor.YELLOW + "• Click upgrades to purchase them");
        player.sendMessage(ChatColor.YELLOW + "• Use arrows to navigate pages");
        player.sendMessage(ChatColor.YELLOW + "• Level up to unlock higher tiers");
        player.sendMessage(ChatColor.YELLOW + "• Each tier can have unlimited upgrades");
    }
    
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
} 