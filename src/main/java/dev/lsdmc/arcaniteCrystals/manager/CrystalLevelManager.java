package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CrystalLevelManager {
    private static final Map<UUID, Integer> playerLevels = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> tierRequirements = new HashMap<>();
    
    static {
        // Initialize tier requirements
        tierRequirements.put(1, 0);   // Tier 1: No requirement
        tierRequirements.put(2, 10);  // Tier 2: Level 10
        tierRequirements.put(3, 25);  // Tier 3: Level 25
        tierRequirements.put(4, 50);  // Tier 4: Level 50
    }
    
    /**
     * Gets the player's current crystal level.
     */
    public static int getPlayerLevel(UUID playerId) {
        return playerLevels.computeIfAbsent(playerId, k -> {
            // Load from database if available
            Integer level = PlayerDataManager.getLevel(playerId);
            return level != null ? level : 0;
        });
    }
    
    /**
     * Sets the player's crystal level.
     */
    public static void setPlayerLevel(UUID playerId, int level) {
        playerLevels.put(playerId, level);
        PlayerDataManager.setLevel(playerId, level);
    }
    
    /**
     * Adds experience to the player's crystal level.
     */
    public static void addExperience(UUID playerId, int experience) {
        int currentLevel = getPlayerLevel(playerId);
        int newLevel = calculateNewLevel(currentLevel, experience);
        
        if (newLevel > currentLevel) {
            setPlayerLevel(playerId, newLevel);
            Player player = ArcaniteCrystals.getInstance().getServer().getPlayer(playerId);
            if (player != null) {
                MessageManager.sendNotification(player, 
                    "Crystal level up! You are now level " + newLevel, 
                    MessageManager.NotificationType.LEVEL_UP);
            }
        }
    }
    
    /**
     * Calculates the new level based on current level and experience gained.
     */
    private static int calculateNewLevel(int currentLevel, int experience) {
        int totalExperience = currentLevel * 100 + experience;
        return totalExperience / 100;
    }
    
    /**
     * Gets the required level for a crystal tier.
     */
    public static int getRequiredLevel(int tier) {
        return tierRequirements.getOrDefault(tier, 0);
    }
    
    /**
     * Checks if a player can use a crystal of the specified tier.
     */
    public static boolean canUseCrystal(Player player, int tier) {
        int requiredLevel = getRequiredLevel(tier);
        int playerLevel = getPlayerLevel(player.getUniqueId());
        return playerLevel >= requiredLevel;
    }
    
    /**
     * Gets the maximum tier a player can use.
     */
    public static int getMaxTier(UUID playerId) {
        int playerLevel = getPlayerLevel(playerId);
        int maxTier = 1;
        
        for (Map.Entry<Integer, Integer> entry : tierRequirements.entrySet()) {
            if (playerLevel >= entry.getValue()) {
                maxTier = entry.getKey();
            }
        }
        
        return maxTier;
    }
    
    /**
     * Gets the number of effect slots available to a player.
     */
    public static int getSlots(UUID playerId) {
        int level = getPlayerLevel(playerId);
        if (level >= 50) return 4;
        if (level >= 25) return 3;
        if (level >= 10) return 2;
        return 1;
    }
    
    /**
     * Validates if a player can use a crystal and handles feedback.
     */
    public static boolean validateCrystalUse(Player player, ItemStack crystal) {
        if (crystal == null || !CrystalManager.isCrystal(crystal)) {
            return false;
        }
        
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Integer tier = container.get(CrystalManager.KEY_TIER, PersistentDataType.INTEGER);
        
        if (tier == null) {
            tier = 1; // Default to tier 1 if not specified
        }
        
        if (!canUseCrystal(player, tier)) {
            int requiredLevel = getRequiredLevel(tier);
            MessageManager.sendNotification(player,
                "You need to be level " + requiredLevel + " to use this crystal!",
                MessageManager.NotificationType.ERROR);
            return false;
        }
        
        return true;
    }
    
    /**
     * Cleans up player data.
     */
    public static void cleanup() {
        playerLevels.clear();
    }
} 