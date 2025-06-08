package dev.lsdmc.arcaniteCrystals.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.function.Function;

/**
 * Professional database manager that switches between Redis and SQLite based on configuration.
 * Provides unified interface for all database operations with proper fallback handling.
 */
public class DatabaseManager {
    
    private static DataStore primaryStore;
    private static DataStore fallbackStore;
    private static boolean usingFallback = false;
    private static JavaPlugin plugin;
    private static Logger logger;
    private static boolean initialized = false;
    private static HikariDataSource dataSource;
    
    /**
     * Initialize the database system with automatic mode detection and fallback.
     */
    public static boolean initialize(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
        logger = plugin.getLogger();
        
        try {
            // Always initialize SQLite as fallback
            fallbackStore = new SqliteDataStore(plugin);
            logger.info("SQLite fallback store initialized");
            
            String mode = ConfigManager.getConfig().getString("database.mode", "sqlite");
            if ("redis".equalsIgnoreCase(mode)) {
                try {
                    primaryStore = new RedisDataStore(ConfigManager.getConfig(), plugin);
                    if (primaryStore.isHealthy()) {
                        logger.info("Redis primary store initialized successfully");
                        initialized = true;
                        return true;
                    } else {
                        logger.warning("Redis unhealthy, falling back to SQLite");
                        primaryStore = fallbackStore;
                        usingFallback = true;
                        initialized = true;
                        return true;
                    }
                } catch (Exception e) {
                    logger.warning("Redis initialization failed: " + e.getMessage());
                    primaryStore = fallbackStore;
                    usingFallback = true;
                    initialized = true;
                    return true;
                }
            } else {
                primaryStore = fallbackStore;
                logger.info("Using SQLite as primary store");
                initialized = true;
                return true;
            }
        } catch (Exception e) {
            logger.severe("Critical: All database initialization failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get player level with automatic fallback handling.
     */
    public static CompletableFuture<Integer> getLevel(UUID playerId) {
        return executeWithFailover(store -> store.getLevel(playerId), 1);
    }
    
    /**
     * Set player level with automatic fallback handling.
     */
    public static CompletableFuture<Void> setLevel(UUID playerId, int level) {
        return executeWithFailover(store -> store.setLevel(playerId, level), null);
    }
    
    /**
     * Get unlocked upgrades with automatic fallback handling.
     */
    public static CompletableFuture<Set<String>> getUnlockedUpgrades(UUID playerId) {
        return executeWithFailover(store -> store.getUnlockedUpgrades(playerId), Set.of());
    }
    
    /**
     * Unlock upgrade with automatic fallback handling.
     */
    public static CompletableFuture<Void> unlockUpgrade(UUID playerId, String upgradeId) {
        return executeWithFailover(store -> store.unlockUpgrade(playerId, upgradeId), null);
    }
    
    /**
     * Revoke upgrade with automatic fallback handling.
     */
    public static CompletableFuture<Void> revokeUpgrade(UUID playerId, String upgradeId) {
        return executeWithFailover(store -> store.revokeUpgrade(playerId, upgradeId), null);
    }
    
    /**
     * Get player cooldown with automatic fallback handling.
     */
    public static CompletableFuture<Long> getCooldown(UUID playerId) {
        return executeWithFailover(store -> store.getCooldown(playerId), 0L);
    }
    
    /**
     * Set player cooldown with automatic fallback handling.
     */
    public static CompletableFuture<Void> setCooldown(UUID playerId, long timestamp) {
        return executeWithFailover(store -> store.setCooldown(playerId, timestamp), null);
    }
    
    /**
     * Save all data with automatic fallback handling.
     */
    public static CompletableFuture<Void> saveBatch(Map<UUID, PlayerData> data) {
        return executeWithFailover(store -> store.saveBatch(data), null);
    }
    
    /**
     * Execute operation with automatic failover to fallback store.
     */
    private static <T> CompletableFuture<T> executeWithFailover(
            Function<DataStore, CompletableFuture<T>> operation, T defaultValue) {
        if (!initialized) {
            logger.warning("Database not initialized, returning default value");
            return CompletableFuture.completedFuture(defaultValue);
        }
        
        return operation.apply(primaryStore)
            .exceptionally(throwable -> {
                if (!usingFallback && fallbackStore != primaryStore) {
                    logger.warning("Primary store failed, attempting fallback: " + throwable.getMessage());
                    try {
                        return operation.apply(fallbackStore).join();
                    } catch (Exception fallbackError) {
                        logger.severe("Both primary and fallback failed: " + fallbackError.getMessage());
                        return defaultValue;
                    }
                }
                logger.severe("Database operation failed: " + throwable.getMessage());
                return defaultValue;
            });
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
    public static String getCurrentMode() {
        if (!initialized) return "Not Initialized";
        if (usingFallback) return "SQLite (Fallback)";
        return primaryStore instanceof RedisDataStore ? "Redis" : "SQLite";
    }
    
    /**
     * Get database statistics.
     */
    public static String getStats() {
        if (!initialized) {
            return "Database not initialized";
        }
        
        try {
            String modeInfo = "Mode: " + getCurrentMode() + " | ";
            return modeInfo + primaryStore.getStats();
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
            return primaryStore.isHealthy();
        } catch (Exception e) {
            logger.warning("Error checking database health: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Shutdown database connections.
     */
    public static CompletableFuture<Void> shutdown() {
        if (!initialized) {
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.allOf(
            primaryStore.shutdown(),
            fallbackStore.shutdown()
        ).whenComplete((v, e) -> {
            if (e != null) {
                logger.severe("Error during database shutdown: " + e.getMessage());
            } else {
                logger.info("Database connections closed successfully.");
            }
            initialized = false;
        });
    }

    public static CompletableFuture<PlayerData> getPlayerData(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                String sql = "SELECT * FROM player_data WHERE player_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, playerId.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            int level = rs.getInt("level");
                            Set<String> unlockedUpgrades = new HashSet<>();
                            String upgradesStr = rs.getString("unlocked_upgrades");
                            if (upgradesStr != null && !upgradesStr.isEmpty()) {
                                unlockedUpgrades.addAll(Arrays.asList(upgradesStr.split(",")));
                            }
                            long cooldown = rs.getLong("cooldown");
                            return new PlayerData(level, unlockedUpgrades, cooldown);
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    private static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void initialize() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder() + "/database.db");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        
        dataSource = new HikariDataSource(config);
        createTables();
    }
    
    private static void createTables() {
        try (Connection conn = getConnection()) {
            String sql = """
                CREATE TABLE IF NOT EXISTS player_data (
                    player_id TEXT PRIMARY KEY,
                    level INTEGER DEFAULT 1,
                    unlocked_upgrades TEXT,
                    cooldown BIGINT DEFAULT 0
                )
            """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void cleanup() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
} 