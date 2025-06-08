// src/main/java/dev/lsdmc/arcaniteCrystals/database/PlayerDataManager.java
package dev.lsdmc.arcaniteCrystals.database;

import org.bukkit.plugin.java.JavaPlugin;
import dev.lsdmc.arcaniteCrystals.manager.PlayerStatisticsManager;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;

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
        return DatabaseManager.getLevel(playerId).join();
    }

    /**
     * Set player level.
     */
    public static void setLevel(UUID playerId, int level) {
        DatabaseManager.setLevel(playerId, level).join();
    }

    /**
     * Get unlocked upgrades.
     */
    public static Set<String> getUnlockedUpgrades(UUID playerId) {
        return DatabaseManager.getUnlockedUpgrades(playerId).join();
    }

    /**
     * Unlock an upgrade.
     */
    public static void unlockUpgrade(UUID playerId, String upgradeId) {
        DatabaseManager.unlockUpgrade(playerId, upgradeId).join();
    }

    /**
     * Revoke an upgrade.
     */
    public static void revokeUpgrade(UUID playerId, String upgradeId) {
        DatabaseManager.revokeUpgrade(playerId, upgradeId).join();
    }

    /**
     * Get player cooldown.
     */
    public static long getCooldown(UUID playerId) {
        return DatabaseManager.getCooldown(playerId).join();
    }

    /**
     * Set player cooldown.
     */
    public static void setCooldown(UUID playerId, long timestamp) {
        DatabaseManager.setCooldown(playerId, timestamp).join();
    }

    /**
     * Save all data.
     */
    public static void saveAll() {
        // Create batch data from current state
        Map<UUID, PlayerData> batchData = new HashMap<>();
        // TODO: Implement batch data collection
        DatabaseManager.saveBatch(batchData).join();
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
    
    /**
     * Async version of getLevel.
     */
    public static CompletableFuture<Integer> getLevelAsync(UUID playerId) {
        return DatabaseManager.getLevel(playerId);
    }
    
    /**
     * Async version of setLevel.
     */
    public static CompletableFuture<Void> setLevelAsync(UUID playerId, int level) {
        return DatabaseManager.setLevel(playerId, level);
    }
    
    /**
     * Async version of getUnlockedUpgrades.
     */
    public static CompletableFuture<Set<String>> getUnlockedUpgradesAsync(UUID playerId) {
        return DatabaseManager.getUnlockedUpgrades(playerId);
    }
    
    /**
     * Async version of unlockUpgrade.
     */
    public static CompletableFuture<Void> unlockUpgradeAsync(UUID playerId, String upgradeId) {
        return DatabaseManager.unlockUpgrade(playerId, upgradeId);
    }
    
    /**
     * Async version of revokeUpgrade.
     */
    public static CompletableFuture<Void> revokeUpgradeAsync(UUID playerId, String upgradeId) {
        return DatabaseManager.revokeUpgrade(playerId, upgradeId);
    }
    
    /**
     * Async version of getCooldown.
     */
    public static CompletableFuture<Long> getCooldownAsync(UUID playerId) {
        return DatabaseManager.getCooldown(playerId);
    }
    
    /**
     * Async version of setCooldown.
     */
    public static CompletableFuture<Void> setCooldownAsync(UUID playerId, long timestamp) {
        return DatabaseManager.setCooldown(playerId, timestamp);
    }

    /**
     * Save player statistics.
     */
    public static void savePlayerStats(UUID playerId, PlayerStatisticsManager.PlayerStats stats) {
        // Create batch data
        Map<UUID, PlayerData> batchData = new HashMap<>();
        PlayerData data = new PlayerData(
            getLevel(playerId),
            getUnlockedUpgrades(playerId),
            getCooldown(playerId)
        );
        
        // Set statistics
        data.setCrystalsActivated(stats.crystalsActivated.get());
        data.setSuccessfulFusions(stats.successfulFusions.get());
        data.setFailedFusions(stats.failedFusions.get());
        data.setSuccessfulSockets(stats.successfulSockets.get());
        data.setFailedSockets(stats.failedSockets.get());
        data.setTotalEnergyUsed(stats.totalEnergyUsed.get());
        data.setCrystalsDecayed(stats.crystalsDecayed.get());
        data.setCrystalsCorrupted(stats.crystalsCorrupted.get());
        data.setSuccessfulIdentifications(stats.successfulIdentifications.get());
        data.setFailedIdentifications(stats.failedIdentifications.get());
        
        batchData.put(playerId, data);
        DatabaseManager.saveBatch(batchData).join();
    }

    public static int getPlayerLevel(UUID playerId) {
        PlayerData data = DatabaseManager.getPlayerData(playerId).join();
        return data != null ? data.getLevel() : 1;
    }
}
