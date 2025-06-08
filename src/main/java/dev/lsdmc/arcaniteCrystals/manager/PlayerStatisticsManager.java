package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.DatabaseManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Manages player statistics and crystal usage tracking.
 */
public class PlayerStatisticsManager {
    private static final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Long>> stats = new ConcurrentHashMap<>();
    private static final Map<UUID, List<CrystalUsageRecord>> usageHistory = new ConcurrentHashMap<>();
    private static final Map<UUID, List<PurchaseRecord>> purchaseHistory = new ConcurrentHashMap<>();
    private static final Logger logger = ArcaniteCrystals.getInstance().getLogger();
    
    private static PlayerStatisticsManager instance;
    private final FileConfiguration config;
    private final int saveInterval;
    
    public static class PlayerStats {
        public final AtomicInteger crystalsActivated = new AtomicInteger(0);
        public final AtomicInteger successfulFusions = new AtomicInteger(0);
        public final AtomicInteger failedFusions = new AtomicInteger(0);
        public final AtomicInteger successfulSockets = new AtomicInteger(0);
        public final AtomicInteger failedSockets = new AtomicInteger(0);
        public final AtomicInteger totalEnergyUsed = new AtomicInteger(0);
        public final AtomicInteger crystalsDecayed = new AtomicInteger(0);
        public final AtomicInteger crystalsCorrupted = new AtomicInteger(0);
        public final AtomicInteger successfulIdentifications = new AtomicInteger(0);
        public final AtomicInteger failedIdentifications = new AtomicInteger(0);
    }
    
    public PlayerStatisticsManager() {
        this.config = ConfigManager.getConfig();
        this.saveInterval = config.getInt("statistics.save-interval", 300); // 5 minutes
        instance = this;
    }
    
    public static PlayerStatisticsManager getInstance() {
        return instance;
    }
    
    /**
     * Record of crystal usage.
     */
    public static class CrystalUsageRecord {
        private final String crystalId;
        private final String action;
        private final int energyBefore;
        private final int energyAfter;
        private final Instant timestamp;
        
        public CrystalUsageRecord(String crystalId, String action, int energyBefore, int energyAfter) {
            this.crystalId = crystalId;
            this.action = action;
            this.energyBefore = energyBefore;
            this.energyAfter = energyAfter;
            this.timestamp = Instant.now();
        }
        
        public String getCrystalId() { return crystalId; }
        public String getAction() { return action; }
        public int getEnergyBefore() { return energyBefore; }
        public int getEnergyAfter() { return energyAfter; }
        public Instant getTimestamp() { return timestamp; }
    }
    
    /**
     * Record of upgrade purchase.
     */
    public static class PurchaseRecord {
        private final String upgradeId;
        private final String costType;
        private final int costAmount;
        private final Instant timestamp;
        
        public PurchaseRecord(String upgradeId, String costType, int costAmount) {
            this.upgradeId = upgradeId;
            this.costType = costType;
            this.costAmount = costAmount;
            this.timestamp = Instant.now();
        }
        
        public String getUpgradeId() { return upgradeId; }
        public String getCostType() { return costType; }
        public int getCostAmount() { return costAmount; }
        public Instant getTimestamp() { return timestamp; }
    }
    
    /**
     * Increments a player's statistic.
     */
    public static void incrementStat(UUID playerId, String statName, long amount) {
        PlayerStats stats = playerStats.computeIfAbsent(playerId, k -> new PlayerStats());
        switch (statName) {
            case "crystals_activated" -> stats.crystalsActivated.addAndGet((int)amount);
            case "successful_fusions" -> stats.successfulFusions.addAndGet((int)amount);
            case "failed_fusions" -> stats.failedFusions.addAndGet((int)amount);
            case "successful_sockets" -> stats.successfulSockets.addAndGet((int)amount);
            case "failed_sockets" -> stats.failedSockets.addAndGet((int)amount);
            case "total_energy_used" -> stats.totalEnergyUsed.addAndGet((int)amount);
            case "crystals_decayed" -> stats.crystalsDecayed.addAndGet((int)amount);
            case "crystals_corrupted" -> stats.crystalsCorrupted.addAndGet((int)amount);
            case "successful_identifications" -> stats.successfulIdentifications.addAndGet((int)amount);
            case "failed_identifications" -> stats.failedIdentifications.addAndGet((int)amount);
        }
        
        // Log significant stat changes
        if (amount > 1000) {
            logger.info("Player " + playerId + " gained " + amount + " " + statName);
        }
    }
    
    /**
     * Gets a player's statistic value.
     */
    public static long getStat(UUID playerId, String statName) {
        PlayerStats stats = playerStats.get(playerId);
        if (stats == null) return 0L;
        
        return switch (statName) {
            case "crystals_activated" -> stats.crystalsActivated.get();
            case "successful_fusions" -> stats.successfulFusions.get();
            case "failed_fusions" -> stats.failedFusions.get();
            case "successful_sockets" -> stats.successfulSockets.get();
            case "failed_sockets" -> stats.failedSockets.get();
            case "total_energy_used" -> stats.totalEnergyUsed.get();
            case "crystals_decayed" -> stats.crystalsDecayed.get();
            case "crystals_corrupted" -> stats.crystalsCorrupted.get();
            case "successful_identifications" -> stats.successfulIdentifications.get();
            case "failed_identifications" -> stats.failedIdentifications.get();
            default -> 0L;
        };
    }
    
    /**
     * Gets all statistics for a player.
     */
    public static Map<String, Long> getAllStats(UUID playerId) {
        PlayerStats stats = playerStats.get(playerId);
        if (stats == null) return new HashMap<>();
        
        Map<String, Long> result = new HashMap<>();
        result.put("crystals_activated", (long)stats.crystalsActivated.get());
        result.put("successful_fusions", (long)stats.successfulFusions.get());
        result.put("failed_fusions", (long)stats.failedFusions.get());
        result.put("successful_sockets", (long)stats.successfulSockets.get());
        result.put("failed_sockets", (long)stats.failedSockets.get());
        result.put("total_energy_used", (long)stats.totalEnergyUsed.get());
        result.put("crystals_decayed", (long)stats.crystalsDecayed.get());
        result.put("crystals_corrupted", (long)stats.crystalsCorrupted.get());
        result.put("successful_identifications", (long)stats.successfulIdentifications.get());
        result.put("failed_identifications", (long)stats.failedIdentifications.get());
        return result;
    }
    
    /**
     * Logs crystal usage.
     */
    public static void logCrystalUsage(UUID playerId, String crystalId, String action, int energyBefore, int energyAfter) {
        List<CrystalUsageRecord> history = usageHistory.computeIfAbsent(playerId, k -> new ArrayList<>());
        history.add(new CrystalUsageRecord(crystalId, action, energyBefore, energyAfter));
        
        // Keep history size manageable
        if (history.size() > 1000) {
            history.subList(0, history.size() - 1000).clear();
        }
        
        // Update relevant stats
        incrementStat(playerId, "crystal_uses", 1);
        incrementStat(playerId, "energy_used", energyBefore - energyAfter);
    }
    
    /**
     * Logs upgrade purchase.
     */
    public static void logUpgradePurchase(UUID playerId, String upgradeId, String costType, int amount) {
        List<PurchaseRecord> history = purchaseHistory.computeIfAbsent(playerId, k -> new ArrayList<>());
        history.add(new PurchaseRecord(upgradeId, costType, amount));
        
        // Keep history size manageable
        if (history.size() > 1000) {
            history.subList(0, history.size() - 1000).clear();
        }
        
        // Update relevant stats
        incrementStat(playerId, "upgrades_purchased", 1);
        incrementStat(playerId, "total_spent_" + costType, amount);
    }
    
    /**
     * Gets crystal usage history for a player.
     */
    public static List<CrystalUsageRecord> getCrystalHistory(UUID playerId, int limit) {
        List<CrystalUsageRecord> history = usageHistory.get(playerId);
        if (history == null) return Collections.emptyList();
        
        int startIndex = Math.max(0, history.size() - limit);
        return new ArrayList<>(history.subList(startIndex, history.size()));
    }
    
    /**
     * Gets purchase history for a player.
     */
    public static List<PurchaseRecord> getPurchaseHistory(UUID playerId, int limit) {
        List<PurchaseRecord> history = purchaseHistory.get(playerId);
        if (history == null) return Collections.emptyList();
        
        int startIndex = Math.max(0, history.size() - limit);
        return new ArrayList<>(history.subList(startIndex, history.size()));
    }
    
    /**
     * Gets top players by a specific statistic.
     */
    public static List<Map.Entry<UUID, Long>> getTopPlayers(String statName, int limit) {
        List<Map.Entry<UUID, Long>> topPlayers = new ArrayList<>();
        
        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            long value = getStat(entry.getKey(), statName);
            if (value > 0) {
                topPlayers.add(new AbstractMap.SimpleEntry<>(entry.getKey(), value));
            }
        }
        
        topPlayers.sort(Map.Entry.<UUID, Long>comparingByValue().reversed());
        return topPlayers.subList(0, Math.min(limit, topPlayers.size()));
    }
    
    /**
     * Cleans up all statistics data.
     */
    public static void cleanup() {
        playerStats.clear();
        usageHistory.clear();
        purchaseHistory.clear();
    }
    
    public PlayerStats getStats(UUID playerId) {
        return playerStats.computeIfAbsent(playerId, k -> new PlayerStats());
    }
} 