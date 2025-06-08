package dev.lsdmc.arcaniteCrystals.database;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * SQLite implementation of DataStore interface with proper connection management,
 * prepared statements, and async operations.
 */
public class SqliteDataStore implements DataStore {
    private static final String DATABASE_VERSION = "1.0";
    
    private final Connection connection;
    private final Logger logger;
    private final File databaseFile;
    private volatile boolean isHealthy = false;
    
    // Cache for frequently accessed data
    private final ConcurrentHashMap<UUID, Integer> levelCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<String>> upgradeCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> cooldownCache = new ConcurrentHashMap<>();
    
    public SqliteDataStore(JavaPlugin plugin) {
        this.logger = plugin.getLogger();
        
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
            
            isHealthy = true;
            logger.info("SQLite database initialized successfully at: " + databaseFile.getAbsolutePath());
            logger.info("Database version: " + DATABASE_VERSION);
            
        } catch (SQLException e) {
            logger.severe("Failed to initialize SQLite database: " + e.getMessage());
            throw new DatabaseException("SQLite initialization failed", e);
        }
    }
    
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Players table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid TEXT PRIMARY KEY,
                    level INTEGER NOT NULL DEFAULT 1,
                    cooldown BIGINT NOT NULL DEFAULT 0
                )
            """);
            
            // Upgrades table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS upgrades (
                    uuid TEXT NOT NULL,
                    upgrade_id TEXT NOT NULL,
                    PRIMARY KEY (uuid, upgrade_id),
                    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
                )
            """);
            
            // Create indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_upgrades_uuid ON upgrades(uuid)");
        }
    }
    
    private void loadCacheFromDatabase() {
        try (Statement stmt = connection.createStatement()) {
            // Load levels and cooldowns
            ResultSet rs = stmt.executeQuery("SELECT uuid, level, cooldown FROM players");
            while (rs.next()) {
                UUID playerId = UUID.fromString(rs.getString("uuid"));
                levelCache.put(playerId, rs.getInt("level"));
                cooldownCache.put(playerId, rs.getLong("cooldown"));
            }
            
            // Load upgrades
            rs = stmt.executeQuery("SELECT uuid, upgrade_id FROM upgrades");
            while (rs.next()) {
                UUID playerId = UUID.fromString(rs.getString("uuid"));
                String upgradeId = rs.getString("upgrade_id");
                upgradeCache.computeIfAbsent(playerId, k -> new HashSet<>()).add(upgradeId);
            }
        } catch (SQLException e) {
            logger.warning("Error loading cache from database: " + e.getMessage());
        }
    }
    
    @Override
    public CompletableFuture<Integer> getLevel(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache first
            Integer cached = levelCache.get(playerId);
            if (cached != null) {
                return cached;
            }
            
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT level FROM players WHERE uuid = ?")) {
                stmt.setString(1, playerId.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    int level = rs.getInt("level");
                    levelCache.put(playerId, level);
                    return level;
                } else {
                    // Create new player entry
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO players (uuid, level) VALUES (?, 1)")) {
                        insert.setString(1, playerId.toString());
                        insert.executeUpdate();
                    }
                    levelCache.put(playerId, 1);
                    return 1;
                }
            } catch (SQLException e) {
                logger.warning("Error getting player level: " + e.getMessage());
                return 1;
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> setLevel(UUID playerId, int level) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO players (uuid, level) VALUES (?, ?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET level = ?")) {
                stmt.setString(1, playerId.toString());
                stmt.setInt(2, level);
                stmt.setInt(3, level);
                stmt.executeUpdate();
                
                levelCache.put(playerId, level);
            } catch (SQLException e) {
                logger.severe("Error setting player level: " + e.getMessage());
                throw new DatabaseException("SQLite operation failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Set<String>> getUnlockedUpgrades(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache first
            Set<String> cached = upgradeCache.get(playerId);
            if (cached != null) {
                return new HashSet<>(cached);
            }
            
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT upgrade_id FROM upgrades WHERE uuid = ?")) {
                stmt.setString(1, playerId.toString());
                ResultSet rs = stmt.executeQuery();
                
                Set<String> upgrades = new HashSet<>();
                while (rs.next()) {
                    upgrades.add(rs.getString("upgrade_id"));
                }
                
                upgradeCache.put(playerId, new HashSet<>(upgrades));
                return upgrades;
            } catch (SQLException e) {
                logger.warning("Error getting unlocked upgrades: " + e.getMessage());
                return new HashSet<>();
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> unlockUpgrade(UUID playerId, String upgradeId) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO upgrades (uuid, upgrade_id) VALUES (?, ?)")) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, upgradeId);
                stmt.executeUpdate();
                
                upgradeCache.computeIfAbsent(playerId, k -> new HashSet<>()).add(upgradeId);
            } catch (SQLException e) {
                logger.severe("Error unlocking upgrade: " + e.getMessage());
                throw new DatabaseException("SQLite operation failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> revokeUpgrade(UUID playerId, String upgradeId) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM upgrades WHERE uuid = ? AND upgrade_id = ?")) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, upgradeId);
                stmt.executeUpdate();
                
                Set<String> upgrades = upgradeCache.get(playerId);
                if (upgrades != null) {
                    upgrades.remove(upgradeId);
                }
            } catch (SQLException e) {
                logger.severe("Error revoking upgrade: " + e.getMessage());
                throw new DatabaseException("SQLite operation failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Long> getCooldown(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache first
            Long cached = cooldownCache.get(playerId);
            if (cached != null) {
                return cached;
            }
            
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT cooldown FROM players WHERE uuid = ?")) {
                stmt.setString(1, playerId.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    long cooldown = rs.getLong("cooldown");
                    cooldownCache.put(playerId, cooldown);
                    return cooldown;
                } else {
                    cooldownCache.put(playerId, 0L);
                    return 0L;
                }
            } catch (SQLException e) {
                logger.warning("Error getting cooldown: " + e.getMessage());
                return 0L;
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> setCooldown(UUID playerId, long timestamp) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO players (uuid, cooldown) VALUES (?, ?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET cooldown = ?")) {
                stmt.setString(1, playerId.toString());
                stmt.setLong(2, timestamp);
                stmt.setLong(3, timestamp);
                stmt.executeUpdate();
                
                cooldownCache.put(playerId, timestamp);
            } catch (SQLException e) {
                logger.severe("Error setting cooldown: " + e.getMessage());
                throw new DatabaseException("SQLite operation failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> saveBatch(Map<UUID, PlayerData> data) {
        return CompletableFuture.runAsync(() -> {
            try {
                connection.setAutoCommit(false);
                
                try (PreparedStatement playerStmt = connection.prepareStatement(
                        "INSERT INTO players (uuid, level, cooldown) VALUES (?, ?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET level = ?, cooldown = ?");
                     PreparedStatement upgradeStmt = connection.prepareStatement(
                        "INSERT INTO upgrades (uuid, upgrade_id) VALUES (?, ?) " +
                        "ON CONFLICT(uuid, upgrade_id) DO NOTHING")) {
                    
                    for (Map.Entry<UUID, PlayerData> entry : data.entrySet()) {
                        UUID playerId = entry.getKey();
                        PlayerData playerData = entry.getValue();
                        
                        // Update player data
                        playerStmt.setString(1, playerId.toString());
                        playerStmt.setInt(2, playerData.getLevel());
                        playerStmt.setLong(3, playerData.getCooldown());
                        playerStmt.setInt(4, playerData.getLevel());
                        playerStmt.setLong(5, playerData.getCooldown());
                        playerStmt.executeUpdate();
                        
                        // Update cache
                        levelCache.put(playerId, playerData.getLevel());
                        cooldownCache.put(playerId, playerData.getCooldown());
                        
                        // Update upgrades
                        Set<String> upgrades = playerData.getUnlockedUpgrades();
                        upgradeCache.put(playerId, new HashSet<>(upgrades));
                        
                        for (String upgradeId : upgrades) {
                            upgradeStmt.setString(1, playerId.toString());
                            upgradeStmt.setString(2, upgradeId);
                            upgradeStmt.executeUpdate();
                        }
                    }
                }
                
                connection.commit();
                
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    logger.severe("Error rolling back batch save: " + rollbackError.getMessage());
                }
                logger.severe("Error during batch save: " + e.getMessage());
                throw new DatabaseException("SQLite batch operation failed", e);
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.warning("Error resetting auto-commit: " + e.getMessage());
                }
            }
        });
    }
    
    @Override
    public boolean isHealthy() {
        return isHealthy && connection != null;
    }
    
    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    isHealthy = false;
                    logger.info("SQLite connection closed successfully.");
                }
            } catch (SQLException e) {
                logger.warning("Error closing SQLite connection: " + e.getMessage());
            }
        });
    }
    
    @Override
    public String getStats() {
        try {
            int playerCount = 0;
            int upgradeCount = 0;
            
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM players");
                if (rs.next()) {
                    playerCount = rs.getInt(1);
                }
                
                rs = stmt.executeQuery("SELECT COUNT(*) FROM upgrades");
                if (rs.next()) {
                    upgradeCount = rs.getInt(1);
                }
            }
            
            return String.format("Players: %d, Upgrades: %d, Cache Size: %d", 
                    playerCount, 
                    upgradeCount,
                    levelCache.size() + upgradeCache.size() + cooldownCache.size());
                    
        } catch (SQLException e) {
            return "Error getting stats: " + e.getMessage();
        }
    }
} 