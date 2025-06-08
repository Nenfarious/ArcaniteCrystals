package dev.lsdmc.arcaniteCrystals.database;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface defining the contract for all database implementations.
 * All operations are asynchronous to prevent blocking the main thread.
 */
public interface DataStore {
    // Player data operations
    CompletableFuture<Integer> getLevel(UUID playerId);
    CompletableFuture<Void> setLevel(UUID playerId, int level);
    CompletableFuture<Set<String>> getUnlockedUpgrades(UUID playerId);
    CompletableFuture<Void> unlockUpgrade(UUID playerId, String upgradeId);
    CompletableFuture<Void> revokeUpgrade(UUID playerId, String upgradeId);
    CompletableFuture<Long> getCooldown(UUID playerId);
    CompletableFuture<Void> setCooldown(UUID playerId, long timestamp);
    
    // Batch operations
    CompletableFuture<Void> saveBatch(Map<UUID, PlayerData> data);
    
    // Health and management
    boolean isHealthy();
    CompletableFuture<Void> shutdown();
    String getStats();
} 