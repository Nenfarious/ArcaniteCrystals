package dev.lsdmc.arcaniteCrystals.database;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Professional SQLite database manager with comprehensive data persistence,
 * connection pooling, and robust error handling.
 */
public class SqliteDataManager {

    private static Connection connection;
    private static JavaPlugin plugin;
    private static Logger logger;
    
    // Cache for frequently accessed data
    private static final ConcurrentHashMap<UUID, Integer> levelCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Set<String>> upgradeCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> cooldownCache = new ConcurrentHashMap<>();
    
    // Database file and connection info
    private static File databaseFile;
    private static final String DATABASE_VERSION = "1.0";

    /**
     * Initialize the SQLite database with professional setup.
     */
    public static boolean initialize(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
        logger = plugin.getLogger();
        
        try {
            // Create database file
            databaseFile = new File(plugin.getDataFolder(), "arcanite_data.db");
            if (!databaseFile.getParentFile().exists()) {
                databaseFile.getParentFile().mkdirs();
            }
            
            // Establish connection
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            
            // Enable foreign keys and WAL mode for better performance
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA journal_mode = WAL;");
                stmt.execute("PRAGMA synchronous = NORMAL;");
                stmt.execute("PRAGMA cache_size = 10000;");
                stmt.execute("PRAGMA temp_store = MEMORY;");
            }
            
            // Create tables
            createTables();
            
            // Load cache from database
            loadCacheFromDatabase();
            
            logger.info("SQLite database initialized successfully at: " + databaseFile.getAbsolutePath());
            logger.info("Database version: " + DATABASE_VERSION);
            
            return true;
            
        } catch (SQLException e) {
            logger.severe("Failed to initialize SQLite database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Create all necessary database tables with proper indexing.
     */
    private static void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            
            // Player levels table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_levels (
                    player_uuid TEXT PRIMARY KEY,
                    level INTEGER NOT NULL DEFAULT 1,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);
            
            // Player upgrades table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_upgrades (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    upgrade_id TEXT NOT NULL,
                    unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(player_uuid, upgrade_id)
                );
            """);
            
            // Player cooldowns table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_cooldowns (
                    player_uuid TEXT PRIMARY KEY,
                    cooldown_timestamp BIGINT NOT NULL DEFAULT 0,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);
            
            // Database metadata table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS database_metadata (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);
            
            // Create indexes for better performance
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_upgrades_player ON player_upgrades(player_uuid);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_upgrades_upgrade ON player_upgrades(upgrade_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_levels_level ON player_levels(level);");
            
            // Insert database version
            stmt.execute("""
                INSERT OR REPLACE INTO database_metadata (key, value) 
                VALUES ('version', ?)
            """);
            
            try (PreparedStatement versionStmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO database_metadata (key, value) VALUES ('version', ?)")) {
                versionStmt.setString(1, DATABASE_VERSION);
                versionStmt.executeUpdate();
            }
            
            logger.info("Database tables created/verified successfully.");
        }
    }

    /**
     * Load all data into cache for fast access.
     */
    private static void loadCacheFromDatabase() {
        try {
            // Load levels
            try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT player_uuid, level FROM player_levels")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                    int level = rs.getInt("level");
                    levelCache.put(playerId, level);
                }
            }
            
            // Load upgrades
            try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT player_uuid, upgrade_id FROM player_upgrades")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                    String upgradeId = rs.getString("upgrade_id");
                    upgradeCache.computeIfAbsent(playerId, k -> new HashSet<>()).add(upgradeId);
                }
            }
            
            // Load cooldowns
            try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT player_uuid, cooldown_timestamp FROM player_cooldowns")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                    long cooldown = rs.getLong("cooldown_timestamp");
                    cooldownCache.put(playerId, cooldown);
                }
            }
            
            logger.info("Loaded " + levelCache.size() + " player levels, " + 
                       upgradeCache.size() + " player upgrade sets, and " + 
                       cooldownCache.size() + " cooldowns into cache.");
            
        } catch (SQLException e) {
            logger.severe("Error loading cache from database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get player level with caching.
     */
    public static int getLevel(UUID playerId) {
        if (playerId == null) {
            logger.warning("Attempted to get level for null UUID");
            return 1;
        }
        
        // Check cache first
        Integer cached = levelCache.get(playerId);
        if (cached != null) {
            return Math.max(1, cached);
        }
        
        // Load from database
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT level FROM player_levels WHERE player_uuid = ?")) {
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int level = Math.max(1, rs.getInt("level"));
                levelCache.put(playerId, level);
                return level;
            }
        } catch (SQLException e) {
            logger.warning("Error getting level for " + playerId + ": " + e.getMessage());
        }
        
        // Default level
        return 1;
    }

    /**
     * Set player level with immediate persistence.
     */
    public static void setLevel(UUID playerId, int level) {
        if (playerId == null) {
            logger.warning("Attempted to set level for null UUID");
            return;
        }
        
        final int finalLevel = Math.max(1, level);
        levelCache.put(playerId, finalLevel);
        
        // Async database update
        CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("""
                INSERT OR REPLACE INTO player_levels (player_uuid, level, updated_at) 
                VALUES (?, ?, CURRENT_TIMESTAMP)
            """)) {
                stmt.setString(1, playerId.toString());
                stmt.setInt(2, finalLevel);
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.severe("Error setting level for " + playerId + ": " + e.getMessage());
            }
        });
    }

    /**
     * Get unlocked upgrades with caching.
     */
    public static Set<String> getUnlockedUpgrades(UUID playerId) {
        if (playerId == null) {
            logger.warning("Attempted to get upgrades for null UUID");
            return new HashSet<>();
        }
        
        // Check cache first
        Set<String> cached = upgradeCache.get(playerId);
        if (cached != null) {
            return new HashSet<>(cached);
        }
        
        // Load from database
        Set<String> upgrades = new HashSet<>();
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT upgrade_id FROM player_upgrades WHERE player_uuid = ?")) {
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                upgrades.add(rs.getString("upgrade_id"));
            }
            
            upgradeCache.put(playerId, new HashSet<>(upgrades));
        } catch (SQLException e) {
            logger.warning("Error getting upgrades for " + playerId + ": " + e.getMessage());
        }
        
        return upgrades;
    }

    /**
     * Unlock an upgrade with immediate persistence.
     */
    public static void unlockUpgrade(UUID playerId, String upgradeId) {
        if (playerId == null || upgradeId == null || upgradeId.trim().isEmpty()) {
            logger.warning("Invalid parameters for unlock upgrade: " + playerId + ", " + upgradeId);
            return;
        }
        
        upgradeId = upgradeId.trim().toLowerCase();
        upgradeCache.computeIfAbsent(playerId, k -> new HashSet<>()).add(upgradeId);
        
        // Async database update
        final String finalUpgradeId = upgradeId;
        CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("""
                INSERT OR IGNORE INTO player_upgrades (player_uuid, upgrade_id, unlocked_at) 
                VALUES (?, ?, CURRENT_TIMESTAMP)
            """)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, finalUpgradeId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.severe("Error unlocking upgrade for " + playerId + ": " + e.getMessage());
            }
        });
    }

    /**
     * Revoke an upgrade with immediate persistence.
     */
    public static void revokeUpgrade(UUID playerId, String upgradeId) {
        if (playerId == null || upgradeId == null || upgradeId.trim().isEmpty()) {
            logger.warning("Invalid parameters for revoke upgrade: " + playerId + ", " + upgradeId);
            return;
        }
        
        upgradeId = upgradeId.trim().toLowerCase();
        Set<String> upgrades = upgradeCache.get(playerId);
        if (upgrades != null) {
            upgrades.remove(upgradeId);
        }
        
        // Async database update
        final String finalUpgradeId = upgradeId;
        CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM player_upgrades WHERE player_uuid = ? AND upgrade_id = ?")) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, finalUpgradeId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.severe("Error revoking upgrade for " + playerId + ": " + e.getMessage());
            }
        });
    }

    /**
     * Get player cooldown with caching.
     */
    public static long getCooldown(UUID playerId) {
        if (playerId == null) {
            logger.warning("Attempted to get cooldown for null UUID");
            return 0L;
        }
        
        // Check cache first
        Long cached = cooldownCache.get(playerId);
        if (cached != null) {
            return cached;
        }
        
        // Load from database
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT cooldown_timestamp FROM player_cooldowns WHERE player_uuid = ?")) {
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                long cooldown = rs.getLong("cooldown_timestamp");
                cooldownCache.put(playerId, cooldown);
                return cooldown;
            }
        } catch (SQLException e) {
            logger.warning("Error getting cooldown for " + playerId + ": " + e.getMessage());
        }
        
        return 0L;
    }

    /**
     * Set player cooldown with immediate persistence.
     */
    public static void setCooldown(UUID playerId, long timestamp) {
        if (playerId == null) {
            logger.warning("Attempted to set cooldown for null UUID");
            return;
        }
        
        cooldownCache.put(playerId, timestamp);
        
        // Async database update
        CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("""
                INSERT OR REPLACE INTO player_cooldowns (player_uuid, cooldown_timestamp, updated_at) 
                VALUES (?, ?, CURRENT_TIMESTAMP)
            """)) {
                stmt.setString(1, playerId.toString());
                stmt.setLong(2, timestamp);
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.severe("Error setting cooldown for " + playerId + ": " + e.getMessage());
            }
        });
    }

    /**
     * Force save all cached data to database.
     */
    public static void saveAll() {
        try {
            logger.info("Saving all cached data to database...");
            
            // Save levels
            try (PreparedStatement stmt = connection.prepareStatement("""
                INSERT OR REPLACE INTO player_levels (player_uuid, level, updated_at) 
                VALUES (?, ?, CURRENT_TIMESTAMP)
            """)) {
                for (var entry : levelCache.entrySet()) {
                    stmt.setString(1, entry.getKey().toString());
                    stmt.setInt(2, entry.getValue());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            
            // Save cooldowns
            try (PreparedStatement stmt = connection.prepareStatement("""
                INSERT OR REPLACE INTO player_cooldowns (player_uuid, cooldown_timestamp, updated_at) 
                VALUES (?, ?, CURRENT_TIMESTAMP)
            """)) {
                for (var entry : cooldownCache.entrySet()) {
                    stmt.setString(1, entry.getKey().toString());
                    stmt.setLong(2, entry.getValue());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            
            logger.info("All cached data saved successfully.");
            
        } catch (SQLException e) {
            logger.severe("Error during saveAll: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get database statistics for monitoring.
     */
    public static String getStats() {
        try (Statement stmt = connection.createStatement()) {
            StringBuilder stats = new StringBuilder();
            
            // Count records in each table
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM player_levels");
            if (rs.next()) {
                stats.append("Levels: ").append(rs.getInt(1));
            }
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM player_upgrades");
            if (rs.next()) {
                stats.append(", Upgrades: ").append(rs.getInt(1));
            }
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM player_cooldowns");
            if (rs.next()) {
                stats.append(", Cooldowns: ").append(rs.getInt(1));
            }
            
            // Cache stats
            stats.append(" | Cache - Levels: ").append(levelCache.size())
                 .append(", Upgrades: ").append(upgradeCache.size())
                 .append(", Cooldowns: ").append(cooldownCache.size());
            
            return stats.toString();
            
        } catch (SQLException e) {
            return "Error getting stats: " + e.getMessage();
        }
    }

    /**
     * Close database connection safely.
     */
    public static void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                saveAll();
                connection.close();
                logger.info("SQLite database connection closed successfully.");
            }
        } catch (SQLException e) {
            logger.severe("Error closing database connection: " + e.getMessage());
        }
    }

    /**
     * Check if database is connected and healthy.
     */
    public static boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(1);
        } catch (SQLException e) {
            return false;
        }
    }
} 