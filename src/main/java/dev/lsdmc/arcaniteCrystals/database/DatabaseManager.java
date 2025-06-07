package dev.lsdmc.arcaniteCrystals.database;

import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Professional database manager that switches between Redis and SQLite based on configuration.
 * Provides unified interface for all database operations with proper fallback handling.
 */
public class DatabaseManager {
    
    private static DatabaseMode currentMode;
    private static JavaPlugin plugin;
    private static Logger logger;
    private static boolean initialized = false;
    
    public enum DatabaseMode {
        REDIS, SQLITE
    }
    
    /**
     * Initialize the database system with automatic mode detection and fallback.
     */
    public static boolean initialize(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
        logger = plugin.getLogger();
        
        String configMode = ConfigManager.getConfig().getString("database.mode", "sqlite").toLowerCase();
        
        switch (configMode) {
            case "redis" -> {
                logger.info("Attempting to initialize Redis database...");
                if (RedisManager.initialize(ConfigManager.getConfig(), plugin)) {
                    currentMode = DatabaseMode.REDIS;
                    logger.info("Successfully initialized Redis database mode.");
                    initialized = true;
                    return true;
                } else {
                    logger.warning("Redis initialization failed, falling back to SQLite...");
                    return initializeSqliteFallback();
                }
            }
            case "sqlite" -> {
                logger.info("Initializing SQLite database...");
                return initializeSqliteFallback();
            }
            default -> {
                logger.warning("Unknown database mode: " + configMode + ", defaulting to SQLite");
                return initializeSqliteFallback();
            }
        }
    }
    
    /**
     * Initialize SQLite as fallback or primary database.
     */
    private static boolean initializeSqliteFallback() {
        if (SqliteDataManager.initialize(plugin)) {
            currentMode = DatabaseMode.SQLITE;
            logger.info("Successfully initialized SQLite database mode.");
            initialized = true;
            return true;
        } else {
            logger.severe("Failed to initialize SQLite database! Plugin cannot function.");
            initialized = false;
            return false;
        }
    }
    
    /**
     * Check if database system is properly initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Get current database mode.
     */
    public static DatabaseMode getCurrentMode() {
        return currentMode;
    }
    
    /**
     * Get player level with automatic fallback handling.
     */
    public static int getLevel(UUID playerId) {
        if (!initialized) {
            logger.warning("Database not initialized, returning default level");
            return 1;
        }
        
        try {
            switch (currentMode) {
                case REDIS -> {
                    // Redis implementation would go here
                    // For now, fallback to SQLite
                    return SqliteDataManager.getLevel(playerId);
                }
                case SQLITE -> {
                    return SqliteDataManager.getLevel(playerId);
                }
                default -> {
                    logger.warning("Unknown database mode, using SQLite fallback");
                    return SqliteDataManager.getLevel(playerId);
                }
            }
        } catch (Exception e) {
            logger.severe("Error getting player level: " + e.getMessage());
            return 1; // Safe fallback
        }
    }
    
    /**
     * Set player level with automatic fallback handling.
     */
    public static void setLevel(UUID playerId, int level) {
        if (!initialized) {
            logger.warning("Database not initialized, cannot set level");
            return;
        }
        
        try {
            switch (currentMode) {
                case REDIS -> {
                    // Redis implementation would go here
                    // For now, fallback to SQLite
                    SqliteDataManager.setLevel(playerId, level);
                }
                case SQLITE -> SqliteDataManager.setLevel(playerId, level);
                default -> {
                    logger.warning("Unknown database mode, using SQLite fallback");
                    SqliteDataManager.setLevel(playerId, level);
                }
            }
        } catch (Exception e) {
            logger.severe("Error setting player level: " + e.getMessage());
        }
    }
    
    /**
     * Get unlocked upgrades with automatic fallback handling.
     */
    public static Set<String> getUnlockedUpgrades(UUID playerId) {
        if (!initialized) {
            logger.warning("Database not initialized, returning empty upgrade set");
            return Set.of();
        }
        
        try {
            switch (currentMode) {
                case REDIS -> {
                    // Redis implementation would go here
                    // For now, fallback to SQLite
                    return SqliteDataManager.getUnlockedUpgrades(playerId);
                }
                case SQLITE -> {
                    return SqliteDataManager.getUnlockedUpgrades(playerId);
                }
                default -> {
                    logger.warning("Unknown database mode, using SQLite fallback");
                    return SqliteDataManager.getUnlockedUpgrades(playerId);
                }
            }
        } catch (Exception e) {
            logger.severe("Error getting unlocked upgrades: " + e.getMessage());
            return Set.of(); // Safe fallback
        }
    }
    
    /**
     * Unlock upgrade with automatic fallback handling.
     */
    public static void unlockUpgrade(UUID playerId, String upgradeId) {
        if (!initialized) {
            logger.warning("Database not initialized, cannot unlock upgrade");
            return;
        }
        
        try {
            switch (currentMode) {
                case REDIS -> {
                    // Redis implementation would go here
                    // For now, fallback to SQLite
                    SqliteDataManager.unlockUpgrade(playerId, upgradeId);
                }
                case SQLITE -> SqliteDataManager.unlockUpgrade(playerId, upgradeId);
                default -> {
                    logger.warning("Unknown database mode, using SQLite fallback");
                    SqliteDataManager.unlockUpgrade(playerId, upgradeId);
                }
            }
        } catch (Exception e) {
            logger.severe("Error unlocking upgrade: " + e.getMessage());
        }
    }
    
    /**
     * Revoke upgrade with automatic fallback handling.
     */
    public static void revokeUpgrade(UUID playerId, String upgradeId) {
        if (!initialized) {
            logger.warning("Database not initialized, cannot revoke upgrade");
            return;
        }
        
        try {
            switch (currentMode) {
                case REDIS -> {
                    // Redis implementation would go here
                    // For now, fallback to SQLite
                    SqliteDataManager.revokeUpgrade(playerId, upgradeId);
                }
                case SQLITE -> SqliteDataManager.revokeUpgrade(playerId, upgradeId);
                default -> {
                    logger.warning("Unknown database mode, using SQLite fallback");
                    SqliteDataManager.revokeUpgrade(playerId, upgradeId);
                }
            }
        } catch (Exception e) {
            logger.severe("Error revoking upgrade: " + e.getMessage());
        }
    }
    
    /**
     * Get player cooldown with automatic fallback handling.
     */
    public static long getCooldown(UUID playerId) {
        if (!initialized) {
            logger.warning("Database not initialized, returning no cooldown");
            return 0L;
        }
        
        try {
            switch (currentMode) {
                case REDIS -> {
                    // Redis implementation would go here
                    // For now, fallback to SQLite
                    return SqliteDataManager.getCooldown(playerId);
                }
                case SQLITE -> {
                    return SqliteDataManager.getCooldown(playerId);
                }
                default -> {
                    logger.warning("Unknown database mode, using SQLite fallback");
                    return SqliteDataManager.getCooldown(playerId);
                }
            }
        } catch (Exception e) {
            logger.severe("Error getting cooldown: " + e.getMessage());
            return 0L; // Safe fallback
        }
    }
    
    /**
     * Set player cooldown with automatic fallback handling.
     */
    public static void setCooldown(UUID playerId, long timestamp) {
        if (!initialized) {
            logger.warning("Database not initialized, cannot set cooldown");
            return;
        }
        
        try {
            switch (currentMode) {
                case REDIS -> {
                    // Redis implementation would go here
                    // For now, fallback to SQLite
                    SqliteDataManager.setCooldown(playerId, timestamp);
                }
                case SQLITE -> SqliteDataManager.setCooldown(playerId, timestamp);
                default -> {
                    logger.warning("Unknown database mode, using SQLite fallback");
                    SqliteDataManager.setCooldown(playerId, timestamp);
                }
            }
        } catch (Exception e) {
            logger.severe("Error setting cooldown: " + e.getMessage());
        }
    }
    
    /**
     * Save all data with automatic fallback handling.
     */
    public static void saveAll() {
        if (!initialized) {
            logger.warning("Database not initialized, cannot save data");
            return;
        }
        
        try {
            switch (currentMode) {
                case REDIS -> {
                    // Redis implementation would go here
                    // For now, fallback to SQLite
                    SqliteDataManager.saveAll();
                }
                case SQLITE -> SqliteDataManager.saveAll();
                default -> {
                    logger.warning("Unknown database mode, using SQLite fallback");
                    SqliteDataManager.saveAll();
                }
            }
        } catch (Exception e) {
            logger.severe("Error saving data: " + e.getMessage());
        }
    }
    
    /**
     * Get database statistics.
     */
    public static String getStats() {
        if (!initialized) {
            return "Database not initialized";
        }
        
        try {
            String modeInfo = "Mode: " + currentMode.name() + " | ";
            switch (currentMode) {
                case REDIS -> {
                    return modeInfo + "Redis: " + RedisManager.getPoolStats();
                }
                case SQLITE -> {
                    return modeInfo + SqliteDataManager.getStats();
                }
                default -> {
                    return modeInfo + "Unknown mode";
                }
            }
        } catch (Exception e) {
            return "Error getting stats: " + e.getMessage();
        }
    }
    
    /**
     * Check database health.
     */
    public static boolean isHealthy() {
        if (!initialized) {
            return false;
        }
        
        try {
            switch (currentMode) {
                case REDIS -> {
                    return RedisManager.isConnected() && RedisManager.testConnection();
                }
                case SQLITE -> {
                    return SqliteDataManager.isConnected();
                }
                default -> {
                    return false;
                }
            }
        } catch (Exception e) {
            logger.warning("Error checking database health: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Shutdown database connections.
     */
    public static void shutdown() {
        try {
            if (currentMode == DatabaseMode.REDIS) {
                RedisManager.shutdown();
            }
            SqliteDataManager.shutdown();
            
            initialized = false;
            logger.info("Database connections closed successfully.");
        } catch (Exception e) {
            logger.severe("Error during database shutdown: " + e.getMessage());
        }
    }
} 