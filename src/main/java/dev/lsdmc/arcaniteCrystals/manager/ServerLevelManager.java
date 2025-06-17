package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.RequirementChecker;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Comprehensive server-wide leveling system that can be hooked into by other plugins.
 * Provides a robust API for level management, requirement checking, and progression tracking.
 */
public class ServerLevelManager implements Listener {

    private static final Logger logger = ArcaniteCrystals.getInstance().getLogger();
    private static final Map<UUID, LevelData> playerLevelCache = new ConcurrentHashMap<>();
    private static final Map<Integer, LevelConfiguration> levelConfigs = new HashMap<>();
    private static Economy economy;
    
    // Hook for other plugins to listen to level changes
    private static final Set<LevelChangeListener> levelChangeListeners = ConcurrentHashMap.newKeySet();
    
    static {
        // Initialize economy
        RegisteredServiceProvider<Economy> rsp = ArcaniteCrystals.getInstance()
                .getServer().getServicesManager().getRegistration(Economy.class);
        economy = (rsp != null) ? rsp.getProvider() : null;
        
        loadLevelConfigurations();
    }

    /**
     * Interface for other plugins to listen to level changes
     */
    public interface LevelChangeListener {
        void onLevelUp(Player player, int oldLevel, int newLevel, LevelConfiguration newLevelConfig);
        void onLevelDown(Player player, int oldLevel, int newLevel, LevelConfiguration newLevelConfig);
    }

    /**
     * Comprehensive level data for players
     */
    public static class LevelData {
        private final UUID playerId;
        private int level;
        private long experience;
        private long totalExperience;
        private final Map<String, Double> appliedBuffs;
        private long lastLevelUpTime;
        
        public LevelData(UUID playerId, int level) {
            this.playerId = playerId;
            this.level = level;
            this.experience = 0;
            this.totalExperience = 0;
            this.appliedBuffs = new HashMap<>();
            this.lastLevelUpTime = 0;
        }
        
        // Getters
        public UUID getPlayerId() { return playerId; }
        public int getLevel() { return level; }
        public long getExperience() { return experience; }
        public long getTotalExperience() { return totalExperience; }
        public Map<String, Double> getAppliedBuffs() { return new HashMap<>(appliedBuffs); }
        public long getLastLevelUpTime() { return lastLevelUpTime; }
        
        // Setters (package private)
        void setLevel(int level) { this.level = level; }
        void setExperience(long experience) { this.experience = experience; }
        void addExperience(long experience) { this.experience += experience; this.totalExperience += experience; }
        void setLastLevelUpTime(long time) { this.lastLevelUpTime = time; }
        void addAppliedBuff(String buff, double value) { this.appliedBuffs.put(buff, value); }
        void clearAppliedBuffs() { this.appliedBuffs.clear(); }
    }

    /**
     * Comprehensive level configuration
     */
    public static class LevelConfiguration {
        private final int level;
        private final String tag;
        private final String displayName;
        private final String description;
        private final RequirementSet requirements;
        private final Map<String, Double> buffs;
        private final Set<String> permissions;
        private final Set<String> commands;
        private final Map<String, Object> customData;
        
        public LevelConfiguration(int level, String tag, String displayName, String description,
                                RequirementSet requirements, Map<String, Double> buffs,
                                Set<String> permissions, Set<String> commands, Map<String, Object> customData) {
            this.level = level;
            this.tag = tag;
            this.displayName = displayName;
            this.description = description;
            this.requirements = requirements;
            this.buffs = Collections.unmodifiableMap(buffs);
            this.permissions = Collections.unmodifiableSet(permissions);
            this.commands = Collections.unmodifiableSet(commands);
            this.customData = Collections.unmodifiableMap(customData);
        }
        
        // Getters
        public int getLevel() { return level; }
        public String getTag() { return tag; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public RequirementSet getRequirements() { return requirements; }
        public Map<String, Double> getBuffs() { return buffs; }
        public Set<String> getPermissions() { return permissions; }
        public Set<String> getCommands() { return commands; }
        public Map<String, Object> getCustomData() { return customData; }
        
        // Helper methods
        public boolean hasPermission(String permission) { return permissions.contains(permission); }
        public Object getCustomData(String key) { return customData.get(key); }
        public <T> T getCustomData(String key, Class<T> type) {
            Object value = customData.get(key);
            return type.isInstance(value) ? type.cast(value) : null;
        }
    }

    /**
     * Level requirement set
     */
    public static class RequirementSet {
        private final double money;
        private final int playerKills;
        private final int mobKills;
        private final long playtimeHours;
        private final long experience;
        private final Map<String, Integer> items;
        private final Map<String, Integer> customRequirements;
        
        public RequirementSet(double money, int playerKills, int mobKills, long playtimeHours, long experience,
                            Map<String, Integer> items, Map<String, Integer> customRequirements) {
            this.money = money;
            this.playerKills = playerKills;
            this.mobKills = mobKills;
            this.playtimeHours = playtimeHours;
            this.experience = experience;
            this.items = Collections.unmodifiableMap(items);
            this.customRequirements = Collections.unmodifiableMap(customRequirements);
        }
        
        // Getters
        public double getMoney() { return money; }
        public int getPlayerKills() { return playerKills; }
        public int getMobKills() { return mobKills; }
        public long getPlaytimeHours() { return playtimeHours; }
        public long getExperience() { return experience; }
        public Map<String, Integer> getItems() { return items; }
        public Map<String, Integer> getCustomRequirements() { return customRequirements; }
    }

    // ===== PUBLIC API METHODS =====

    /**
     * Gets a player's current level (API method for other plugins)
     */
    public static int getPlayerLevel(UUID playerId) {
        LevelData data = playerLevelCache.get(playerId);
        if (data != null) return data.getLevel();
        
        // Load from database
        int level = PlayerDataManager.getLevel(playerId);
        playerLevelCache.put(playerId, new LevelData(playerId, level));
        return level;
    }

    /**
     * Gets a player's level data (API method for other plugins)
     */
    public static LevelData getPlayerLevelData(UUID playerId) {
        return playerLevelCache.computeIfAbsent(playerId, 
            id -> new LevelData(id, PlayerDataManager.getLevel(id)));
    }

    /**
     * Gets level configuration (API method for other plugins)
     */
    public static LevelConfiguration getLevelConfiguration(int level) {
        return levelConfigs.get(level);
    }

    /**
     * Gets all available levels (API method for other plugins)
     */
    public static Set<Integer> getAvailableLevels() {
        return new HashSet<>(levelConfigs.keySet());
    }

    /**
     * Gets max available level (API method for other plugins)
     */
    public static int getMaxLevel() {
        return levelConfigs.keySet().stream().mapToInt(Integer::intValue).max().orElse(10);
    }

    /**
     * Checks if a player can level up (API method for other plugins)
     */
    public static boolean canPlayerLevelUp(UUID playerId) {
        int currentLevel = getPlayerLevel(playerId);
        int nextLevel = currentLevel + 1;
        
        LevelConfiguration nextLevelConfig = getLevelConfiguration(nextLevel);
        if (nextLevelConfig == null) return false;
        
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return false;
        
        return checkRequirements(player, nextLevelConfig.getRequirements()).isEmpty();
    }

    /**
     * Gets missing requirements for next level (API method for other plugins)
     */
    public static List<String> getMissingRequirementsForNextLevel(UUID playerId) {
        int currentLevel = getPlayerLevel(playerId);
        int nextLevel = currentLevel + 1;
        
        LevelConfiguration nextLevelConfig = getLevelConfiguration(nextLevel);
        if (nextLevelConfig == null) return Arrays.asList("Max level reached");
        
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return Arrays.asList("Player not online");
        
        return checkRequirements(player, nextLevelConfig.getRequirements());
    }

    /**
     * Attempts to level up a player (API method for other plugins)
     */
    public static boolean levelUpPlayer(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return false;
        
        int currentLevel = getPlayerLevel(playerId);
        int nextLevel = currentLevel + 1;
        
        LevelConfiguration nextLevelConfig = getLevelConfiguration(nextLevel);
        if (nextLevelConfig == null) return false;
        
        // Check requirements
        List<String> missing = checkRequirements(player, nextLevelConfig.getRequirements());
        if (!missing.isEmpty()) return false;
        
        // Consume requirements
        if (!consumeRequirements(player, nextLevelConfig.getRequirements())) {
            return false;
        }
        
        // Update level
        LevelData levelData = getPlayerLevelData(playerId);
        levelData.setLevel(nextLevel);
        levelData.setLastLevelUpTime(System.currentTimeMillis());
        PlayerDataManager.setLevel(playerId, nextLevel);
        
        // Apply buffs
        applyLevelBuffs(player, nextLevelConfig);
        
        // Grant permissions
        grantLevelPermissions(player, nextLevelConfig);
        
        // Execute commands
        executeLevelCommands(player, nextLevelConfig);
        
        // Notify listeners
        notifyLevelChange(player, currentLevel, nextLevel, nextLevelConfig);
        
        // Send feedback
        sendLevelUpFeedback(player, nextLevelConfig);
        
        logger.info("Player " + player.getName() + " leveled up to level " + nextLevel);
        return true;
    }

    /**
     * Registers a level change listener (API method for other plugins)
     */
    public static void registerLevelChangeListener(LevelChangeListener listener) {
        levelChangeListeners.add(listener);
    }

    /**
     * Unregisters a level change listener (API method for other plugins)
     */
    public static void unregisterLevelChangeListener(LevelChangeListener listener) {
        levelChangeListeners.remove(listener);
    }

    // ===== CRYSTAL SYSTEM INTEGRATION METHODS =====
    
    /**
     * Gets the maximum effect tier allowed by the player's level (API method for other plugins)
     */
    public static int getMaxTier(UUID playerId) {
        int level = getPlayerLevel(playerId);
        LevelConfiguration config = getLevelConfiguration(level);
        if (config != null) {
            // Get tier from level configuration custom data or calculate based on level
            Integer tier = config.getCustomData("tier", Integer.class);
            if (tier != null) return tier;
            
            // Check levels.yml directly for tier info
            FileConfiguration levelsConfig = ConfigManager.getLevelsConfig();
            ConfigurationSection levelSection = levelsConfig.getConfigurationSection("level-" + level);
            if (levelSection != null) {
                return levelSection.getInt("tier", 1);
            }
        }
        
        // Fallback calculation based on level ranges
        if (level >= 7) return 3;      // Levels 7-10: Tier 3
        if (level >= 3) return 2;      // Levels 3-6: Tier 2
        return 1;                      // Levels 1-2: Tier 1
    }
    
    /**
     * Gets the number of crystal slots for the player's level (API method for other plugins)
     */
    public static int getSlots(UUID playerId) {
        int level = getPlayerLevel(playerId);
        LevelConfiguration config = getLevelConfiguration(level);
        if (config != null) {
            // Get slots from level configuration custom data or calculate based on level
            Integer slots = config.getCustomData("crystal-slots", Integer.class);
            if (slots != null) return slots;
            
            // Check levels.yml directly for slots info
            FileConfiguration levelsConfig = ConfigManager.getLevelsConfig();
            ConfigurationSection levelSection = levelsConfig.getConfigurationSection("level-" + level);
            if (levelSection != null) {
                return levelSection.getInt("crystal-slots", 1);
            }
        }
        
        // Fallback calculation based on level ranges
        if (level >= 9) return 5;      // Levels 9-10: 5 slots
        if (level >= 7) return 4;      // Levels 7-8: 4 slots
        if (level >= 5) return 3;      // Levels 5-6: 3 slots
        if (level >= 3) return 2;      // Levels 3-4: 2 slots
        return 1;                      // Levels 1-2: 1 slot
    }
    
    /**
     * Gets the set of upgrade IDs the player has unlocked (API method for other plugins)
     */
    public static Set<String> getAllowedUpgrades(UUID playerId) {
        return PlayerDataManager.getUnlockedUpgrades(playerId);
    }
    
    /**
     * Applies permanent attribute buffs to the player based on their level (API method for other plugins)
     */
    public static void applyBuffs(Player player, int level) {
        LevelConfiguration config = getLevelConfiguration(level);
        if (config != null) {
            applyLevelBuffs(player, config);
        }
    }
    
    /**
     * Determines the tier number for a given upgrade ID by reading from upgrades.yml (API method for other plugins)
     */
    public static int getTier(String upgradeId) {
        FileConfiguration upgradesConfig = ConfigManager.getUpgradesConfig();
        ConfigurationSection upgradesSection = upgradesConfig.getConfigurationSection("upgrades");
        if (upgradesSection == null) return 1;
        
        ConfigurationSection upgradeSection = upgradesSection.getConfigurationSection(upgradeId);
        if (upgradeSection != null) {
            return upgradeSection.getInt("tier", 1);
        }
        
        // Fallback: parse tier from the upgrade ID suffix (e.g., "speed_II" -> tier 2)
        String[] parts = upgradeId.split("_");
        if (parts.length > 1) {
            String tierStr = parts[parts.length - 1];
            switch (tierStr) {
                case "I": return 1;
                case "II": return 2;
                case "III": return 3;
                default: return 1;
            }
        }
        return 1;
    }
    
    /**
     * Gets all available upgrades for a given tier from upgrades.yml (API method for other plugins)
     */
    public static Set<String> getUpgradesForTier(int tier) {
        Set<String> upgrades = new HashSet<>();
        FileConfiguration upgradesConfig = ConfigManager.getUpgradesConfig();
        ConfigurationSection upgradesSection = upgradesConfig.getConfigurationSection("upgrades");
        if (upgradesSection == null) return upgrades;
        
        for (String upgradeId : upgradesSection.getKeys(false)) {
            ConfigurationSection upgradeSection = upgradesSection.getConfigurationSection(upgradeId);
            if (upgradeSection != null && upgradeSection.getInt("tier", 1) == tier) {
                upgrades.add(upgradeId);
            }
        }
        return upgrades;
    }
    
    /**
     * Gets all available upgrades up to the specified maximum tier (API method for other plugins)
     */
    public static Set<String> getUpgradesUpToTier(int maxTier) {
        Set<String> upgrades = new HashSet<>();
        for (int tier = 1; tier <= maxTier; tier++) {
            upgrades.addAll(getUpgradesForTier(tier));
        }
        return upgrades;
    }
    
    /**
     * Checks whether the specified level exists in levels.yml (API method for other plugins)
     */
    public static boolean isValidLevel(int level) {
        return levelConfigs.containsKey(level);
    }

    // ===== INTERNAL METHODS =====

    private static List<String> checkRequirements(Player player, RequirementSet requirements) {
        List<String> missing = new ArrayList<>();
        
        // Money requirement
        if (requirements.getMoney() > 0) {
            if (economy == null) {
                missing.add("Economy system not available");
            } else if (!economy.has(player, requirements.getMoney())) {
                missing.add(String.format("$%.2f required", requirements.getMoney()));
            }
        }
        
        // Player kills requirement
        if (requirements.getPlayerKills() > 0) {
            int currentKills = player.getStatistic(Statistic.PLAYER_KILLS);
            if (currentKills < requirements.getPlayerKills()) {
                missing.add(String.format("%d player kills required", requirements.getPlayerKills()));
            }
        }
        
        // Mob kills requirement
        if (requirements.getMobKills() > 0) {
            int currentKills = player.getStatistic(Statistic.MOB_KILLS);
            if (currentKills < requirements.getMobKills()) {
                missing.add(String.format("%d mob kills required", requirements.getMobKills()));
            }
        }
        
        // Playtime requirement
        if (requirements.getPlaytimeHours() > 0) {
            long currentTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
            long currentHours = (currentTicks * 50L) / 3_600_000L; // Convert ticks to hours
            if (currentHours < requirements.getPlaytimeHours()) {
                missing.add(String.format("%d hours playtime required", requirements.getPlaytimeHours()));
            }
        }
        
        // Experience requirement
        if (requirements.getExperience() > 0) {
            if (player.getTotalExperience() < requirements.getExperience()) {
                missing.add(String.format("%d experience required", requirements.getExperience()));
            }
        }
        
        // Item requirements
        for (Map.Entry<String, Integer> entry : requirements.getItems().entrySet()) {
            // Implementation would check inventory for specific items
            // This is a placeholder for item checking logic
        }
        
        return missing;
    }

    private static boolean consumeRequirements(Player player, RequirementSet requirements) {
        try {
            // Consume money
            if (requirements.getMoney() > 0 && economy != null) {
                if (!economy.withdrawPlayer(player, requirements.getMoney()).transactionSuccess()) {
                    return false;
                }
            }
            
            // Consume experience
            if (requirements.getExperience() > 0) {
                int currentExp = player.getTotalExperience();
                if (currentExp >= requirements.getExperience()) {
                    player.giveExp(-((int) requirements.getExperience()));
                } else {
                    return false;
                }
            }
            
            // Consume items (implementation would go here)
            
            return true;
        } catch (Exception e) {
            logger.warning("Error consuming requirements for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private static void applyLevelBuffs(Player player, LevelConfiguration config) {
        try {
            LevelData levelData = getPlayerLevelData(player.getUniqueId());
            levelData.clearAppliedBuffs();
            
            for (Map.Entry<String, Double> buff : config.getBuffs().entrySet()) {
                applyBuff(player, buff.getKey(), buff.getValue());
                levelData.addAppliedBuff(buff.getKey(), buff.getValue());
            }
        } catch (Exception e) {
            logger.warning("Error applying buffs to " + player.getName() + ": " + e.getMessage());
        }
    }

    private static void applyBuff(Player player, String buffType, double value) {
        try {
            Attribute attribute = null;
            AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_NUMBER;
            
            switch (buffType.toLowerCase()) {
                case "max_health":
                    attribute = Attribute.MAX_HEALTH;
                    break;
                case "movement_speed":
                case "walk_speed":
                    attribute = Attribute.MOVEMENT_SPEED;
                    operation = AttributeModifier.Operation.ADD_SCALAR;
                    break;
                case "attack_damage":
                    attribute = Attribute.ATTACK_DAMAGE;
                    operation = AttributeModifier.Operation.ADD_SCALAR;
                    break;
                case "knockback_resistance":
                    attribute = Attribute.KNOCKBACK_RESISTANCE;
                    operation = AttributeModifier.Operation.ADD_SCALAR;
                    break;
                default:
                    logger.warning("Unknown buff type: " + buffType);
                    return;
            }
            
            if (attribute != null) {
                AttributeInstance instance = player.getAttribute(attribute);
                if (instance != null) {
                    // Remove old modifier if it exists
                    String modifierName = "server_level_" + buffType;
                    instance.getModifiers().stream()
                        .filter(mod -> mod.getName().equals(modifierName))
                        .findFirst()
                        .ifPresent(instance::removeModifier);
                    
                    // Add new modifier
                    UUID modifierUUID = UUID.nameUUIDFromBytes(
                        (player.getUniqueId().toString() + "_" + buffType).getBytes()
                    );
                    
                    AttributeModifier modifier = new AttributeModifier(
                        modifierUUID, modifierName, value, operation
                    );
                    
                    instance.addModifier(modifier);
                }
            }
        } catch (Exception e) {
            logger.warning("Error applying buff " + buffType + " to " + player.getName() + ": " + e.getMessage());
        }
    }

    private static void grantLevelPermissions(Player player, LevelConfiguration config) {
        // Implementation would grant permissions based on permission plugin
        // This is a placeholder for permission granting logic
    }

    private static void executeLevelCommands(Player player, LevelConfiguration config) {
        for (String command : config.getCommands()) {
            try {
                String processedCommand = command.replace("{player}", player.getName())
                                                .replace("{level}", String.valueOf(config.getLevel()));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
            } catch (Exception e) {
                logger.warning("Error executing level command for " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    private static void notifyLevelChange(Player player, int oldLevel, int newLevel, LevelConfiguration config) {
        for (LevelChangeListener listener : levelChangeListeners) {
            try {
                listener.onLevelUp(player, oldLevel, newLevel, config);
            } catch (Exception e) {
                logger.warning("Error notifying level change listener: " + e.getMessage());
            }
        }
    }

    private static void sendLevelUpFeedback(Player player, LevelConfiguration config) {
        try {
            MessageManager.sendLevelUp(player, config.getLevel(), config.getTag());
            
            // Send detailed level info
            StringBuilder message = new StringBuilder();
            message.append(MessageManager.ACCENT).append("═══ ").append(MessageManager.SUCCESS)
                   .append("LEVEL ").append(config.getLevel()).append(" ACHIEVED").append(MessageManager.ACCENT).append(" ═══\n");
            message.append(MessageManager.PRIMARY).append("Title: ").append(config.getDisplayName()).append("\n");
            
            if (!config.getDescription().isEmpty()) {
                message.append(MessageManager.MUTED).append(config.getDescription()).append("\n");
            }
            
            if (!config.getBuffs().isEmpty()) {
                message.append(MessageManager.HIGHLIGHT).append("New Abilities:\n");
                for (Map.Entry<String, Double> buff : config.getBuffs().entrySet()) {
                    message.append(MessageManager.ACCENT).append("▸ ").append(MessageManager.PRIMARY)
                           .append(formatBuffName(buff.getKey())).append(": ").append(MessageManager.SUCCESS)
                           .append("+").append(formatBuffValue(buff.getValue())).append("\n");
                }
            }
            
            player.sendMessage(message.toString());
            
        } catch (Exception e) {
            logger.warning("Error sending level up feedback to " + player.getName() + ": " + e.getMessage());
        }
    }

    private static String formatBuffName(String buffKey) {
        return switch (buffKey.toLowerCase()) {
            case "max_health" -> "Max Health";
            case "movement_speed", "walk_speed" -> "Movement Speed";
            case "attack_damage" -> "Attack Damage";
            case "knockback_resistance" -> "Knockback Resistance";
            default -> buffKey.replace("_", " ");
        };
    }

    private static String formatBuffValue(double value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        } else {
            return String.format("%.2f", value);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Load player's level data
        LevelData levelData = getPlayerLevelData(playerId);
        
        // Apply current level buffs
        applyBuffs(player, levelData.getLevel());
    }

    private static void loadLevelConfigurations() {
        try {
            FileConfiguration levelsConfig = ConfigManager.getLevelsConfig();
            levelConfigs.clear();
            
            for (int i = 1; i <= 50; i++) { // Support up to level 50
                ConfigurationSection levelSection = levelsConfig.getConfigurationSection("level-" + i);
                if (levelSection == null) continue;
                
                // Basic info
                String tag = levelSection.getString("tag", "&7[" + i + "]");
                String displayName = levelSection.getString("display-name", "Level " + i + " Player");
                String description = levelSection.getString("description", "A dedicated server member");
                
                // Requirements
                ConfigurationSection reqSection = levelSection.getConfigurationSection("requirements");
                RequirementSet requirements = new RequirementSet(
                    reqSection != null ? reqSection.getDouble("money", 0) : 0,
                    reqSection != null ? reqSection.getInt("kills", 0) : 0, // player kills
                    reqSection != null ? reqSection.getInt("mob-kills", 0) : 0,
                    reqSection != null ? reqSection.getLong("time", 0) : 0,
                    reqSection != null ? reqSection.getLong("experience", 0) : 0,
                    new HashMap<>(), // items - placeholder
                    new HashMap<>()  // custom - placeholder
                );
                
                // Buffs
                Map<String, Double> buffs = new HashMap<>();
                ConfigurationSection buffsSection = levelSection.getConfigurationSection("buffs");
                if (buffsSection != null) {
                    for (String buff : buffsSection.getKeys(false)) {
                        buffs.put(buff, buffsSection.getDouble(buff));
                    }
                }
                
                // Permissions, commands, custom data - placeholders
                Set<String> permissions = new HashSet<>();
                Set<String> commands = new HashSet<>();
                Map<String, Object> customData = new HashMap<>();
                
                levelConfigs.put(i, new LevelConfiguration(i, tag, displayName, description, 
                    requirements, buffs, permissions, commands, customData));
            }
            
            logger.info("Loaded " + levelConfigs.size() + " level configurations");
        } catch (Exception e) {
            logger.severe("Error loading level configurations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Cleanup method
     */
    public static void cleanup() {
        playerLevelCache.clear();
        levelChangeListeners.clear();
    }
} 