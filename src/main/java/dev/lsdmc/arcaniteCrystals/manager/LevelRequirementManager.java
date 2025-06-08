package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LevelRequirementManager {
    private static final Map<UUID, Integer> playerLevels = new HashMap<>();
    private static final Map<Integer, Integer> tierLevelRequirements = new HashMap<>();
    
    static {
        // Initialize tier level requirements
        tierLevelRequirements.put(1, 0);  // Tier 1: No requirement
        tierLevelRequirements.put(2, 10); // Tier 2: Level 10
        tierLevelRequirements.put(3, 25); // Tier 3: Level 25
        tierLevelRequirements.put(4, 50); // Tier 4: Level 50
    }
    
    private static final ArcaniteCrystals plugin = ArcaniteCrystals.getInstance();
    
    public static boolean checkLevelRequirement(Player player, String action, int requiredLevel) {
        int playerLevel = PlayerDataManager.getPlayerLevel(player.getUniqueId());
        
        if (playerLevel < requiredLevel) {
            String message = MessageManager.colorize(
                "&cYou need to be level " + requiredLevel + " to " + action + "!"
            );
            player.sendMessage(message);
            return false;
        }
        
        return true;
    }
    
    public static boolean checkCrystalLevelRequirement(Player player, int crystalLevel) {
        int playerLevel = PlayerDataManager.getPlayerLevel(player.getUniqueId());
        int maxLevelDifference = ConfigManager.getConfig().getInt("fusion.max-level-difference", 2);
        
        if (Math.abs(playerLevel - crystalLevel) > maxLevelDifference) {
            String message = MessageManager.colorize(
                "&cThis crystal's level is too different from your level!"
            );
            player.sendMessage(message);
            return false;
        }
        
        return true;
    }
    
    public static boolean checkUpgradeLevelRequirement(Player player, String upgradeId) {
        int playerLevel = PlayerDataManager.getPlayerLevel(player.getUniqueId());
        int requiredLevel = ConfigManager.getConfig().getInt("upgrades." + upgradeId + ".required-level", 1);
        
        return checkLevelRequirement(player, "use this upgrade", requiredLevel);
    }
    
    public static boolean checkFusionLevelRequirement(Player player) {
        int playerLevel = PlayerDataManager.getPlayerLevel(player.getUniqueId());
        int requiredLevel = ConfigManager.getConfig().getInt("fusion.required-level", 5);
        
        return checkLevelRequirement(player, "fuse crystals", requiredLevel);
    }
    
    public static boolean checkSocketLevelRequirement(Player player) {
        int playerLevel = PlayerDataManager.getPlayerLevel(player.getUniqueId());
        int requiredLevel = ConfigManager.getConfig().getInt("socket.required-level", 3);
        
        return checkLevelRequirement(player, "socket crystals", requiredLevel);
    }
    
    public static boolean checkIdentificationLevelRequirement(Player player) {
        int playerLevel = PlayerDataManager.getPlayerLevel(player.getUniqueId());
        int requiredLevel = ConfigManager.getConfig().getInt("identification.required-level", 2);
        
        return checkLevelRequirement(player, "identify crystals", requiredLevel);
    }
    
    /**
     * Checks if a player meets the level requirement for a crystal.
     */
    public static boolean checkLevelRequirement(Player player, ItemStack crystal) {
        int tier = CrystalManager.getCrystalTier(crystal);
        int requiredLevel = tierLevelRequirements.getOrDefault(tier, 0);
        int playerLevel = getPlayerLevel(player);
        
        return playerLevel >= requiredLevel;
    }
    
    /**
     * Gets the level requirement for a crystal tier.
     */
    public static int getLevelRequirement(int tier) {
        return tierLevelRequirements.getOrDefault(tier, 0);
    }
    
    /**
     * Gets a player's current level.
     */
    public static int getPlayerLevel(Player player) {
        return playerLevels.getOrDefault(player.getUniqueId(), 0);
    }
    
    /**
     * Sets a player's level.
     */
    public static void setPlayerLevel(Player player, int level) {
        playerLevels.put(player.getUniqueId(), level);
    }
    
    /**
     * Increases a player's level by the specified amount.
     */
    public static void increasePlayerLevel(Player player, int amount) {
        int currentLevel = getPlayerLevel(player);
        setPlayerLevel(player, currentLevel + amount);
        
        // Notify player of level up
        MessageManager.sendNotification(player, 
            "Level Up! You are now level " + (currentLevel + amount) + "!", 
            MessageManager.NotificationType.SUCCESS);
    }
    
    /**
     * Calculates experience required for the next level.
     */
    public static int getExperienceForNextLevel(int currentLevel) {
        return 100 * (currentLevel + 1); // Simple linear progression
    }
    
    /**
     * Adds experience to a player's level progress.
     */
    public static void addExperience(Player player, int amount) {
        int currentLevel = getPlayerLevel(player);
        int currentExp = getPlayerExperience(player);
        int requiredExp = getExperienceForNextLevel(currentLevel);
        
        currentExp += amount;
        
        // Level up if enough experience
        while (currentExp >= requiredExp) {
            currentExp -= requiredExp;
            increasePlayerLevel(player, 1);
            requiredExp = getExperienceForNextLevel(getPlayerLevel(player));
        }
        
        setPlayerExperience(player, currentExp);
    }
    
    /**
     * Gets a player's current experience.
     */
    public static int getPlayerExperience(Player player) {
        return ArcaniteCrystals.getInstance().getConfig()
            .getInt("players." + player.getUniqueId() + ".experience", 0);
    }
    
    /**
     * Sets a player's current experience.
     */
    public static void setPlayerExperience(Player player, int experience) {
        ArcaniteCrystals.getInstance().getConfig()
            .set("players." + player.getUniqueId() + ".experience", experience);
        ArcaniteCrystals.getInstance().saveConfig();
    }
    
    /**
     * Saves all player levels to the configuration.
     */
    public static void savePlayerLevels() {
        for (Map.Entry<UUID, Integer> entry : playerLevels.entrySet()) {
            ArcaniteCrystals.getInstance().getConfig()
                .set("players." + entry.getKey() + ".level", entry.getValue());
        }
        ArcaniteCrystals.getInstance().saveConfig();
    }
    
    /**
     * Loads all player levels from the configuration.
     */
    public static void loadPlayerLevels() {
        playerLevels.clear();
        if (ArcaniteCrystals.getInstance().getConfig().contains("players")) {
            for (String uuid : ArcaniteCrystals.getInstance().getConfig()
                    .getConfigurationSection("players").getKeys(false)) {
                int level = ArcaniteCrystals.getInstance().getConfig()
                    .getInt("players." + uuid + ".level", 0);
                playerLevels.put(UUID.fromString(uuid), level);
            }
        }
    }
} 