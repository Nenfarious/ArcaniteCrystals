package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.manager.LevelManager;
import dev.lsdmc.arcaniteCrystals.util.GUIUtils;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced GUI menu for purchasing crystal upgrades with dynamic layout, 
 * proper pagination, improved user experience, and memory leak prevention.
 */
public class TalentMenu implements InventoryHolder, Listener {
    
    private static final int SIZE = 54;
    private static final int UPGRADES_PER_PAGE = 15; // 5 rows x 3 columns for clean spacing
    
    // Dynamic layout - will be calculated based on content
    private static final int CONTENT_START_ROW = 1;
    private static final int UPGRADES_PER_ROW = 7;
    
    // Navigation slots
    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 53;
    private static final int PROGRESS_SLOT = 4; // Top center for progress indicator
    
    // Transaction locking to prevent duplication exploits
    private static final Set<UUID> activePurchases = ConcurrentHashMap.newKeySet();
    
    private final Player player;
    private final int page;
    private final Economy economy;
    private final Inventory inventory;
    private final FileConfiguration upgradesConfig;
    private final List<UpgradeInfo> allUpgrades;
    private boolean isRegistered = false; // Track listener registration
    
    // Track which slots contain which upgrades for reliable click handling
    private final Map<Integer, String> slotToUpgrade = new HashMap<>();
    
    // Track which tiers are present on this page for dynamic headers
    private final Set<Integer> tiersOnPage = new HashSet<>();
    
    public TalentMenu(Player player) {
        this(player, 0);
    }
    
    public TalentMenu(Player player, int page) {
        this.player = player;
        this.page = Math.max(0, page);
        this.upgradesConfig = ConfigManager.getUpgradesConfig();
        
        // Get economy provider
        RegisteredServiceProvider<Economy> rsp = ArcaniteCrystals.getInstance()
                .getServer().getServicesManager().getRegistration(Economy.class);
        this.economy = (rsp != null) ? rsp.getProvider() : null;
        
        // Load all upgrades
        this.allUpgrades = loadAllUpgrades();
        
        // Create inventory with enhanced title
        String title = ChatColor.translateAlternateColorCodes('&', 
            ConfigManager.getMessagesConfig().getString("gui.talents.title", "&5&lCrystal Upgrades"));
        if (getTotalPages() > 1) {
            title += ChatColor.GRAY.toString() + " - Page " + (page + 1);
        }
        this.inventory = Bukkit.createInventory(this, SIZE, title);
        
        // Register listener with proper tracking
        registerListener();
        
        // Build menu
        buildMenu();
    }
    
    /**
     * Properly register event listener with tracking
     */
    private void registerListener() {
        if (!isRegistered) {
            Bukkit.getPluginManager().registerEvents(this, ArcaniteCrystals.getInstance());
            isRegistered = true;
        }
    }
    
    /**
     * Properly unregister event listener to prevent memory leaks
     */
    private void unregisterListener() {
        if (isRegistered) {
            HandlerList.unregisterAll(this);
            isRegistered = false;
        }
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Opens the menu for the player.
     */
    public void open() {
        player.openInventory(inventory);
        SoundManager.playMenuSound(player, SoundManager.MenuSoundType.OPEN);
    }
    
    /**
     * Loads all available upgrades in a simple, sorted order.
     */
    private List<UpgradeInfo> loadAllUpgrades() {
        List<UpgradeInfo> upgrades = new ArrayList<>();
        ConfigurationSection upgradesSection = upgradesConfig.getConfigurationSection("upgrades");
        
        if (upgradesSection == null) {
            ArcaniteCrystals.getInstance().getLogger().warning("No upgrades section found in upgrades.yml");
            return upgrades;
        }
        
        for (String upgradeId : upgradesSection.getKeys(false)) {
            ConfigurationSection upgrade = upgradesSection.getConfigurationSection(upgradeId);
            if (upgrade != null) {
                int tier = upgrade.getInt("tier", 1);
                String effect = upgrade.getString("effect", upgradeId);
                upgrades.add(new UpgradeInfo(upgradeId, tier, effect));
            }
        }
        
        // Sort by tier first, then by name for consistent ordering
        upgrades.sort((a, b) -> {
            int tierCompare = Integer.compare(a.tier, b.tier);
            if (tierCompare != 0) return tierCompare;
            return a.upgradeId.compareTo(b.upgradeId);
        });
        
        return upgrades;
    }
    
    /**
     * Builds the complete menu layout with dynamic organization.
     */
    private void buildMenu() {
        // Clear mappings
        slotToUpgrade.clear();
        tiersOnPage.clear();
        
        // Fill background
        GUIUtils.fillInventory(inventory, Material.BLACK_STAINED_GLASS_PANE, " ");
        
        // Add progress indicator first
        addProgressIndicator();
        
        // Add upgrade items and determine which tiers are present
        addUpgradeItems();
        
        // Add dynamic tier separators based on what's actually on the page
        addDynamicTierSeparators();
        
        // Add navigation
        addNavigation();
        
        // Add decorative borders
        addDecorativeBorders();
    }
    
    /**
     * Adds a progress indicator showing player's advancement.
     */
    private void addProgressIndicator() {
        ItemStack progress = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = progress.getItemMeta();
        
        int playerLevel = PlayerDataManager.getLevel(player.getUniqueId());
        int maxTier = LevelManager.getMaxTier(player.getUniqueId());
        Set<String> unlockedUpgrades = PlayerDataManager.getUnlockedUpgrades(player.getUniqueId());
        
        meta.setDisplayName(ChatColor.GOLD.toString() + "✦ " + ChatColor.BOLD + "Your Progress " + ChatColor.GOLD.toString() + "✦");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.BLUE.toString() + "Level: " + ChatColor.WHITE.toString() + playerLevel + "/10");
        lore.add(ChatColor.AQUA.toString() + "Max Tier: " + ChatColor.WHITE.toString() + maxTier + "/3");
        lore.add(ChatColor.GREEN.toString() + "Upgrades: " + ChatColor.WHITE.toString() + unlockedUpgrades.size() + "/" + allUpgrades.size());
        lore.add("");
        
        // Progress bar
        double progressPercent = (double) unlockedUpgrades.size() / allUpgrades.size() * 100;
        String progressBar = createProgressBar(progressPercent);
        lore.add(ChatColor.YELLOW.toString() + "Completion: " + progressBar);
        lore.add(ChatColor.GRAY.toString() + String.format("%.1f%% Complete", progressPercent));
        
        meta.setLore(lore);
        progress.setItemMeta(meta);
        inventory.setItem(PROGRESS_SLOT, progress);
    }
    
    /**
     * Creates a visual progress bar.
     */
    private String createProgressBar(double percent) {
        int bars = 20;
        int filled = (int) (percent / 100.0 * bars);
        
        StringBuilder bar = new StringBuilder();
        bar.append(ChatColor.GRAY.toString()).append("[");
        
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                bar.append(ChatColor.GREEN.toString()).append("█");
            } else {
                bar.append(ChatColor.DARK_GRAY.toString()).append("░");
            }
        }
        
        bar.append(ChatColor.GRAY.toString()).append("]");
        return bar.toString();
    }
    
    /**
     * Adds upgrade items in a flowing layout.
     */
    private void addUpgradeItems() {
        int startIndex = page * UPGRADES_PER_PAGE;
        int endIndex = Math.min(startIndex + UPGRADES_PER_PAGE, allUpgrades.size());
        
        if (startIndex >= allUpgrades.size()) {
            createNoUpgradesDisplay();
            return;
        }
        
        // Place upgrades in a flowing grid, tracking tiers
        int currentSlot = CONTENT_START_ROW * 9 + 1; // Start of content area
        
        for (int i = startIndex; i < endIndex; i++) {
            // Skip navigation and decoration slots
            while (isReservedSlot(currentSlot)) {
                currentSlot++;
            }
            
            if (currentSlot >= 45) break; // Don't go into navigation area
            
            UpgradeInfo upgrade = allUpgrades.get(i);
            tiersOnPage.add(upgrade.tier); // Track which tiers are on this page
            
            boolean unlocked = PlayerDataManager.getUnlockedUpgrades(player.getUniqueId())
                    .contains(upgrade.upgradeId);
            
            ItemStack item = createUpgradeItem(upgrade, unlocked);
            inventory.setItem(currentSlot, item);
            slotToUpgrade.put(currentSlot, upgrade.upgradeId);
            
            currentSlot++;
        }
    }
    
    /**
     * Checks if a slot is reserved for navigation or decoration.
     */
    private boolean isReservedSlot(int slot) {
        // Reserve top row (except center), navigation row, and specific decoration slots
        if (slot >= 0 && slot <= 8 && slot != PROGRESS_SLOT) return true; // Top row
        if (slot >= 45 && slot <= 53) return true; // Bottom row (navigation)
        if (slot % 9 == 0 || slot % 9 == 8) return true; // Side borders
        return false;
    }
    
    /**
     * Adds dynamic tier separators only for tiers that are actually present.
     */
    private void addDynamicTierSeparators() {
        if (tiersOnPage.isEmpty()) return;
        
        String[] tierNames = {"Tier I", "Tier II", "Tier III"};
        Material[] tierMaterials = {Material.IRON_BLOCK, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK};
        ChatColor[] tierColors = {ChatColor.WHITE, ChatColor.YELLOW, ChatColor.AQUA};
        
        // Create separators for tiers actually present on this page
        List<Integer> sortedTiers = new ArrayList<>(tiersOnPage);
        Collections.sort(sortedTiers);
        
        // Place tier indicators in available top slots
        int[] indicatorSlots = {1, 3, 5, 7}; // Spread across top row
        int slotIndex = 0;
        
        for (int tier : sortedTiers) {
            if (slotIndex >= indicatorSlots.length) break;
            
            ItemStack indicator = new ItemStack(tierMaterials[tier - 1]);
            ItemMeta meta = indicator.getItemMeta();
            meta.setDisplayName(tierColors[tier - 1].toString() + ChatColor.BOLD + tierNames[tier - 1]);
            
            List<String> lore = new ArrayList<>();
            int maxTier = LevelManager.getMaxTier(player.getUniqueId());
            
            if (tier <= maxTier) {
                lore.add(ChatColor.GREEN.toString() + "✓ UNLOCKED");
                lore.add(ChatColor.GRAY.toString() + "Available for purchase");
            } else {
                lore.add(ChatColor.RED.toString() + "✗ LOCKED");
                lore.add(ChatColor.GRAY.toString() + "Level up to unlock");
                lore.add(ChatColor.GOLD.toString() + "Requires: Level " + getMinLevelForTier(tier));
            }
            
            meta.setLore(lore);
            indicator.setItemMeta(meta);
            inventory.setItem(indicatorSlots[slotIndex], indicator);
            
            slotIndex++;
        }
    }
    
    /**
     * Gets minimum level required for a tier.
     */
    private int getMinLevelForTier(int tier) {
        for (int level = 1; level <= 10; level++) {
            var config = LevelManager.getConfigForLevel(level);
            if (config != null && config.getTier() >= tier) {
                return level;
            }
        }
        return tier; // Fallback
    }
    
    /**
     * Adds subtle decorative borders.
     */
    private void addDecorativeBorders() {
        // Side borders with gradient effect
        for (int row = 1; row < 5; row++) {
            int leftSlot = row * 9;
            int rightSlot = row * 9 + 8;
            
            Material borderMaterial = row % 2 == 0 ? Material.GRAY_STAINED_GLASS_PANE : Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            
            inventory.setItem(leftSlot, GUIUtils.createNavIcon(borderMaterial, " "));
            inventory.setItem(rightSlot, GUIUtils.createNavIcon(borderMaterial, " "));
        }
        
        // Corner accents
        inventory.setItem(0, GUIUtils.createNavIcon(Material.BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(8, GUIUtils.createNavIcon(Material.BLUE_STAINED_GLASS_PANE, " "));
    }
    
    /**
     * Creates upgrade items with enhanced visual design.
     */
    private ItemStack createUpgradeItem(UpgradeInfo upgrade, boolean unlocked) {
        ConfigurationSection upgradeSection = upgradesConfig
                .getConfigurationSection("upgrades." + upgrade.upgradeId);
        
        if (upgradeSection == null) {
            return createErrorItem(upgrade.upgradeId);
        }
        
        // Enhanced material selection with more variety
        Material material;
        ChatColor nameColor;
        String statusSymbol;
        
        if (unlocked) {
            material = Material.EMERALD;
            nameColor = ChatColor.GREEN;
            statusSymbol = "✓";
        } else {
            int playerMaxTier = LevelManager.getMaxTier(player.getUniqueId());
            if (upgrade.tier <= playerMaxTier) {
                material = switch (upgrade.tier) {
                    case 1 -> Material.IRON_INGOT;
                    case 2 -> Material.GOLD_INGOT;
                    case 3 -> Material.DIAMOND;
                    default -> Material.COAL;
                };
                nameColor = ChatColor.YELLOW;
                statusSymbol = "◆";
            } else {
                material = Material.BARRIER;
                nameColor = ChatColor.RED;
                statusSymbol = "✗";
            }
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Enhanced display name with better formatting
        String effectName = beautifyEffectName(upgrade.effect);
        String tierRoman = getRomanNumeral(upgrade.tier);
        
        meta.setDisplayName(nameColor.toString() + "" + ChatColor.BOLD + statusSymbol + " " + effectName + " " + 
                           ChatColor.GRAY.toString() + "[" + tierRoman + "]");
        
        // Enhanced lore with better organization
        List<String> lore = createEnhancedUpgradeLore(upgradeSection, upgrade, unlocked);
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Creates enhanced upgrade lore with better organization.
     */
    private List<String> createEnhancedUpgradeLore(ConfigurationSection upgradeSection, 
                                                  UpgradeInfo upgrade, boolean unlocked) {
        List<String> lore = new ArrayList<>();
        
        // Status section with better formatting
        if (unlocked) {
            lore.add(ChatColor.GREEN.toString() + "✓ " + ChatColor.BOLD + "UNLOCKED");
            lore.add(ChatColor.DARK_GREEN.toString() + "This upgrade is active");
        } else {
            int playerMaxTier = LevelManager.getMaxTier(player.getUniqueId());
            if (upgrade.tier > playerMaxTier) {
                lore.add(ChatColor.RED.toString() + "✗ " + ChatColor.BOLD + "LOCKED");
                lore.add(ChatColor.DARK_RED.toString() + "Requires Tier " + upgrade.tier);
                lore.add(ChatColor.GOLD.toString() + "Level up to unlock");
            } else {
                lore.add(ChatColor.YELLOW.toString() + "◆ " + ChatColor.BOLD + "AVAILABLE");
                lore.add(ChatColor.GRAY.toString() + "Click to purchase");
            }
        }
        
        lore.add("");
        
        // Effect information with better details
        int amplifier = upgradeSection.getInt("amplifier", 0);
        String effectName = beautifyEffectName(upgrade.effect);
        String amplifierText = amplifier > 0 ? " " + getRomanNumeral(amplifier + 1) : "";
        
        lore.add(ChatColor.BLUE.toString() + "⚡ Effect: " + ChatColor.WHITE.toString() + effectName + amplifierText);
        lore.add(ChatColor.LIGHT_PURPLE.toString() + "⟐ Tier: " + ChatColor.WHITE.toString() + upgrade.tier);
        
        // Purchase information with better formatting
        if (!unlocked) {
            ConfigurationSection buySection = upgradeSection.getConfigurationSection("buy");
            if (buySection != null) {
                lore.add("");
                String cost = getCostString(buySection);
                boolean canAfford = canPlayerAfford(buySection);
                
                lore.add(ChatColor.GOLD.toString() + "💰 Cost: " + cost);
                
                if (upgrade.tier <= LevelManager.getMaxTier(player.getUniqueId())) {
                    if (canAfford) {
                        lore.add(ChatColor.GREEN.toString() + "✦ Click to purchase! ✦");
                    } else {
                        lore.add(ChatColor.RED.toString() + "❌ Insufficient resources");
                    }
                } else {
                    lore.add(ChatColor.DARK_RED.toString() + "🔒 Tier locked");
                }
            }
        } else {
            lore.add("");
            lore.add(ChatColor.GREEN.toString() + "✨ Upgrade Active ✨");
        }
        
        return lore;
    }
    
    /**
     * Creates a display for when no upgrades are available.
     */
    private void createNoUpgradesDisplay() {
        ItemStack noUpgrades = new ItemStack(Material.GLASS);
        ItemMeta meta = noUpgrades.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY.toString() + "No upgrades on this page");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY.toString() + "Use navigation arrows to browse");
        lore.add(ChatColor.YELLOW.toString() + "Or return to previous pages");
        meta.setLore(lore);
        noUpgrades.setItemMeta(meta);
        inventory.setItem(22, noUpgrades); // Center slot
    }
    
    /**
     * Creates error item for missing upgrade configurations.
     */
    private ItemStack createErrorItem(String upgradeId) {
        ItemStack errorItem = new ItemStack(Material.BARRIER);
        ItemMeta meta = errorItem.getItemMeta();
        meta.setDisplayName(ChatColor.RED.toString() + "Configuration Error");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY.toString() + "Missing config for: " + upgradeId);
        lore.add(ChatColor.YELLOW.toString() + "Contact an administrator");
        meta.setLore(lore);
        errorItem.setItemMeta(meta);
        return errorItem;
    }
    
    /**
     * Gets cost string with enhanced formatting.
     */
    private String getCostString(ConfigurationSection buySection) {
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
     * Checks if player can afford the upgrade.
     */
    private boolean canPlayerAfford(ConfigurationSection buySection) {
        String type = buySection.getString("type", "money");
        int amount = buySection.getInt("amount", 0);
        
        return switch (type.toLowerCase()) {
            case "money" -> economy != null && economy.has(player, amount);
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
     * Beautifies effect names with better formatting.
     */
    private String beautifyEffectName(String effect) {
        return switch (effect.toLowerCase()) {
            case "speed" -> "Swift Movement";
            case "regeneration" -> "Life Regeneration";
            case "jump_boost" -> "Enhanced Jumping";
            case "haste" -> "Mining Acceleration";
            case "strength" -> "Combat Prowess";
            case "damage_resistance" -> "Damage Resistance";
            case "poison" -> "Poison Immunity";
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
    
    /**
     * Converts numbers to Roman numerals.
     */
    private String getRomanNumeral(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";  
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(number);
        };
    }
    
    /**
     * Adds enhanced navigation with better visual design.
     */
    private void addNavigation() {
        // Previous button
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.SPECTRAL_ARROW);
            ItemMeta meta = prev.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN.toString() + "◀ " + ChatColor.BOLD + "Previous Page");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY.toString() + "Go to page " + page);
            lore.add(ChatColor.YELLOW.toString() + "Click to navigate");
            meta.setLore(lore);
            prev.setItemMeta(meta);
            inventory.setItem(PREV_SLOT, prev);
        }
        
        // Next button
        if (hasNextPage()) {
            ItemStack next = new ItemStack(Material.SPECTRAL_ARROW);
            ItemMeta meta = next.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN.toString() + "" + ChatColor.BOLD + "Next Page " + ChatColor.GREEN.toString() + "▶");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY.toString() + "Go to page " + (page + 2));
            lore.add(ChatColor.YELLOW.toString() + "Click to navigate");
            meta.setLore(lore);
            next.setItemMeta(meta);
            inventory.setItem(NEXT_SLOT, next);
        }
        
        // Enhanced info/close button
        ItemStack info = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ChatColor.BLUE.toString() + "📖 " + ChatColor.BOLD + "Menu Info");
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.AQUA.toString() + "Page: " + ChatColor.WHITE.toString() + (page + 1) + "/" + getTotalPages());
        infoLore.add(ChatColor.AQUA.toString() + "Total upgrades: " + ChatColor.WHITE.toString() + allUpgrades.size());
        
        // Show which tiers are on this page
        if (!tiersOnPage.isEmpty()) {
            List<String> tierNames = tiersOnPage.stream()
                    .sorted()
                    .map(tier -> "Tier " + getRomanNumeral(tier))
                    .toList();
            infoLore.add(ChatColor.AQUA.toString() + "Showing: " + ChatColor.WHITE.toString() + String.join(", ", tierNames));
        }
        
        infoLore.add("");
        infoLore.add(ChatColor.YELLOW.toString() + "Right-click to close menu");
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        inventory.setItem(INFO_SLOT, info);
    }
    
    /**
     * Checks if there's a next page.
     */
    private boolean hasNextPage() {
        return (page + 1) * UPGRADES_PER_PAGE < allUpgrades.size();
    }
    
    /**
     * Gets total number of pages.
     */
    private int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) allUpgrades.size() / UPGRADES_PER_PAGE));
    }
    
    /**
     * Handles inventory clicks with enhanced interaction.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player clicker = (Player) event.getWhoClicked();
        
        int slot = event.getRawSlot();
        
        // Handle navigation clicks
        if (slot == PREV_SLOT && page > 0) {
            SoundManager.playMenuSound(clicker, SoundManager.MenuSoundType.CLICK);
            new TalentMenu(clicker, page - 1).open();
            return;
        }
        
        if (slot == NEXT_SLOT && hasNextPage()) {
            SoundManager.playMenuSound(clicker, SoundManager.MenuSoundType.CLICK);
            new TalentMenu(clicker, page + 1).open();
            return;
        }
        
        if (slot == INFO_SLOT && event.isRightClick()) {
            clicker.closeInventory();
            SoundManager.playMenuSound(clicker, SoundManager.MenuSoundType.CLOSE);
            return;
        }
        
        // Handle upgrade clicks
        String upgradeId = slotToUpgrade.get(slot);
        if (upgradeId != null) {
            handleUpgradePurchase(clicker, upgradeId);
        }
    }
    
    /**
     * Handles upgrade purchase with enhanced feedback and transaction locking.
     */
    private void handleUpgradePurchase(Player purchaser, String upgradeId) {
        UUID playerId = purchaser.getUniqueId();
        
        // Check for active purchase to prevent duplication
        if (activePurchases.contains(playerId)) {
            purchaser.sendMessage(ChatColor.YELLOW + "Please wait, processing your previous purchase...");
            return;
        }
        
        // Lock transaction
        activePurchases.add(playerId);
        
        try {
            ConfigurationSection upgradeSection = upgradesConfig
                    .getConfigurationSection("upgrades." + upgradeId);
            
            if (upgradeSection == null) {
                purchaser.sendMessage(ChatColor.RED.toString() + "Error: Upgrade not found!");
                SoundManager.playMenuSound(purchaser, SoundManager.MenuSoundType.ERROR);
                return;
            }

            // Check if already unlocked (double-check under lock)
            if (PlayerDataManager.getUnlockedUpgrades(purchaser.getUniqueId()).contains(upgradeId)) {
                purchaser.sendMessage(ChatColor.YELLOW.toString() + "You already have this upgrade!");
                SoundManager.playMenuSound(purchaser, SoundManager.MenuSoundType.ERROR);
                return;
            }
            
            // Check tier requirement
            int tier = upgradeSection.getInt("tier", 1);
            int maxTier = LevelManager.getMaxTier(purchaser.getUniqueId());
            if (tier > maxTier) {
                purchaser.sendMessage(ChatColor.RED.toString() + "You need to reach Tier " + tier + " to purchase this upgrade!");
                purchaser.sendMessage(ChatColor.YELLOW.toString() + "Use /levelup to advance to higher tiers");
                SoundManager.playMenuSound(purchaser, SoundManager.MenuSoundType.ERROR);
                return;
            }
            
            // Process purchase
            ConfigurationSection buySection = upgradeSection.getConfigurationSection("buy");
            if (buySection == null) {
                purchaser.sendMessage(ChatColor.RED.toString() + "Error: Purchase info missing!");
                return;
            }
            
            if (processPurchase(purchaser, buySection)) {
                // Purchase successful
                PlayerDataManager.unlockUpgrade(purchaser.getUniqueId(), upgradeId);
                
                // Enhanced success feedback
                String effectName = beautifyEffectName(upgradeSection.getString("effect", upgradeId));
                MessageManager.sendUpgradeUnlocked(purchaser, effectName, tier);
                SoundManager.playUpgradeUnlockSound(purchaser);
                ParticleManager.playUpgradeEffect(purchaser);
                
                // Refresh menu with slight delay for effect
                ArcaniteCrystals.getInstance().getServer().getScheduler().runTaskLater(
                    ArcaniteCrystals.getInstance(), () -> {
                    new TalentMenu(purchaser, page).open();
                }, 20L);
            }
        } finally {
            // Always release lock
            activePurchases.remove(playerId);
        }
    }
    
    /**
     * Processes the purchase transaction with enhanced feedback.
     */
    private boolean processPurchase(Player purchaser, ConfigurationSection buySection) {
        String type = buySection.getString("type", "money");
        int amount = buySection.getInt("amount", 0);
        
        return switch (type.toLowerCase()) {
            case "money" -> {
                if (economy == null) {
                    purchaser.sendMessage(ChatColor.RED.toString() + "Economy system not available!");
                    yield false;
                }
                if (!economy.has(purchaser, amount)) {
                    purchaser.sendMessage(ChatColor.RED.toString() + "You need " + ChatColor.GOLD.toString() + "$" + 
                                        String.format("%,d", amount) + ChatColor.RED.toString() + "!");
                    SoundManager.playMenuSound(purchaser, SoundManager.MenuSoundType.ERROR);
                    yield false;
                }
                economy.withdrawPlayer(purchaser, amount);
                yield true;
            }
            case "exp" -> {
                if (purchaser.getTotalExperience() < amount) {
                    purchaser.sendMessage(ChatColor.RED.toString() + "You need " + ChatColor.BLUE.toString() + 
                                        String.format("%,d", amount) + " XP" + ChatColor.RED.toString() + "!");
                    SoundManager.playMenuSound(purchaser, SoundManager.MenuSoundType.ERROR);
                    yield false;
                }
                purchaser.giveExp(-amount);
                yield true;
            }
            case "item" -> {
                String itemId = buySection.getString("id", "DIAMOND");
                Material material = Material.matchMaterial(itemId);
                if (material == null) {
                    purchaser.sendMessage(ChatColor.RED.toString() + "Invalid item configuration!");
                    yield false;
                }
                ItemStack required = new ItemStack(material, amount);
                if (!purchaser.getInventory().containsAtLeast(required, amount)) {
                    String itemName = itemId.replace("_", " ");
                    itemName = itemName.substring(0, 1).toUpperCase() + itemName.substring(1).toLowerCase();
                    purchaser.sendMessage(ChatColor.RED.toString() + "You need " + ChatColor.YELLOW.toString() + 
                                        amount + "x " + itemName + ChatColor.RED.toString() + "!");
                    SoundManager.playMenuSound(purchaser, SoundManager.MenuSoundType.ERROR);
                    yield false;
                }
                purchaser.getInventory().removeItem(required);
                yield true;
            }
            default -> {
                purchaser.sendMessage(ChatColor.RED.toString() + "Invalid purchase configuration!");
                yield false;
            }
        };
    }
    
    /**
     * Handle inventory close to clean up listeners
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            // Clean up listener when menu is closed
            unregisterListener();
            
            // Remove any active purchase locks for this player
            activePurchases.remove(player.getUniqueId());
        }
    }
    
    /**
     * Simple upgrade info holder.
     */
    private static class UpgradeInfo {
        final String upgradeId;
        final int tier;
        final String effect;
        
        UpgradeInfo(String upgradeId, int tier, String effect) {
            this.upgradeId = upgradeId;
            this.tier = tier;
            this.effect = effect;
        }
    }
}


