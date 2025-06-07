// src/main/java/dev/lsdmc/arcaniteCrystals/database/PlayerDataManager.java
package dev.lsdmc.arcaniteCrystals.database;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;

/**
 * Compatibility wrapper for PlayerDataManager that delegates to DatabaseManager.
 * This maintains backward compatibility with existing code while providing
 * proper database abstraction and fallback handling.
 */
public class PlayerDataManager {

    /**
     * Initialize the data manager (delegates to DatabaseManager).
     */
    public static void initialize(JavaPlugin plugin) {
        DatabaseManager.initialize(plugin);
    }

    /**
     * Enable YAML fallback (handled by DatabaseManager).
     */
    public static void enableYamlFallback(JavaPlugin plugin) {
        // Handled by DatabaseManager fallback system
    }

    /**
     * Get player level.
     */
    public static int getLevel(UUID playerId) {
        return DatabaseManager.getLevel(playerId);
    }

    /**
     * Set player level.
     */
    public static void setLevel(UUID playerId, int level) {
        DatabaseManager.setLevel(playerId, level);
    }

    /**
     * Get unlocked upgrades.
     */
    public static Set<String> getUnlockedUpgrades(UUID playerId) {
        return DatabaseManager.getUnlockedUpgrades(playerId);
    }

    /**
     * Unlock an upgrade.
     */
    public static void unlockUpgrade(UUID playerId, String upgradeId) {
        DatabaseManager.unlockUpgrade(playerId, upgradeId);
    }

    /**
     * Revoke an upgrade.
     */
    public static void revokeUpgrade(UUID playerId, String upgradeId) {
        DatabaseManager.revokeUpgrade(playerId, upgradeId);
    }

    /**
     * Get player cooldown.
     */
    public static long getCooldown(UUID playerId) {
        return DatabaseManager.getCooldown(playerId);
    }

    /**
     * Set player cooldown.
     */
    public static void setCooldown(UUID playerId, long timestamp) {
        DatabaseManager.setCooldown(playerId, timestamp);
    }

    /**
     * Save all data.
     */
    public static void saveAll() {
        DatabaseManager.saveAll();
    }

    /**
     * Get cache statistics.
     */
    public static String getCacheStats() {
        return DatabaseManager.getStats();
    }

    /**
     * Clear cache (handled by DatabaseManager).
     */
    public static void clearCache() {
        // Handled by DatabaseManager internally
    }
    
    /**
     * Check if database is healthy.
     */
    public static boolean isHealthy() {
        return DatabaseManager.isHealthy();
    }
    
    /**
     * Check if database is initialized.
     */
    public static boolean isInitialized() {
        return DatabaseManager.isInitialized();
    }
}
