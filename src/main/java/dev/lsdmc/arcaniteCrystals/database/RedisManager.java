// src/main/java/dev/lsdmc/arcaniteCrystals/database/RedisManager.java
package dev.lsdmc.arcaniteCrystals.database;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.util.logging.Logger;

/**
 * Professional Redis connection manager with proper error handling,
 * connection validation, and resource management.
 */
public class RedisManager {

    private static JedisPool pool;
    private static JavaPlugin plugin;
    private static Logger logger;
    private static boolean isConnected = false;
    
    // Rate limiting for error messages to prevent console spam
    private static long lastErrorLogTime = 0;
    private static final long ERROR_LOG_INTERVAL = 30000; // 30 seconds

    /**
     * Initializes Redis connection pool with comprehensive error handling.
     * 
     * @param cfg The configuration containing Redis settings
     * @param pluginInstance The plugin instance for logging
     * @return true if connection successful, false otherwise
     */
    public static boolean initialize(FileConfiguration cfg, JavaPlugin pluginInstance) {
        plugin = pluginInstance;
        logger = plugin.getLogger();
        
        try {
            String host = cfg.getString("database.redis.host", "localhost");
            int port = cfg.getInt("database.redis.port", 6379);
            String password = cfg.getString("database.redis.password", "");
            int timeout = cfg.getInt("database.redis.timeout", 2000);

            // Validate configuration
            if (host == null || host.trim().isEmpty()) {
                logger.severe("Redis host cannot be null or empty!");
                return false;
            }
            
            if (port < 1 || port > 65535) {
                logger.severe("Invalid Redis port: " + port + ". Must be between 1-65535.");
                return false;
            }

            // Configure connection pool
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(cfg.getInt("database.redis.max-total", 128));
            poolConfig.setMaxIdle(cfg.getInt("database.redis.max-idle", 16));
            poolConfig.setMinIdle(cfg.getInt("database.redis.min-idle", 1));
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            
            // Note: Eviction configuration methods are deprecated in newer versions
            // Using basic pool configuration for maximum compatibility

            // Create connection pool
            if (password == null || password.trim().isEmpty()) {
                pool = new JedisPool(poolConfig, host, port, timeout);
            } else {
                pool = new JedisPool(poolConfig, host, port, timeout, password.trim());
            }

            // Test connection
            if (testConnection()) {
                isConnected = true;
                logger.info("Successfully connected to Redis at " + host + ":" + port);
                return true;
            } else {
                logger.severe("Failed to establish Redis connection!");
                return false;
            }
            
        } catch (Exception e) {
            logger.severe("Error initializing Redis connection: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets a Redis connection with proper error handling and rate-limited logging.
     * 
     * @return Jedis connection or null if unavailable
     */
    public static Jedis getResource() {
        if (!isConnected || pool == null) {
            logRateLimited("Redis pool not available. Ensure initialization was successful.");
            return null;
        }
        
        try {
            return pool.getResource();
        } catch (JedisException e) {
            logRateLimited("Failed to get Redis resource: " + e.getMessage());
            return null;
        }
    }

    /**
     * Tests if Redis connection is working with rate-limited error logging.
     * 
     * @return true if connection is healthy
     */
    public static boolean testConnection() {
        if (pool == null) return false;
        
        try (Jedis jedis = pool.getResource()) {
            String response = jedis.ping();
            return "PONG".equals(response);
        } catch (Exception e) {
            logRateLimited("Redis connection test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Logs messages with rate limiting to prevent console spam.
     * Only logs one message per ERROR_LOG_INTERVAL.
     */
    private static void logRateLimited(String message) {
        long now = System.currentTimeMillis();
        if (now - lastErrorLogTime > ERROR_LOG_INTERVAL) {
            logger.warning(message);
            lastErrorLogTime = now;
        }
    }

    /**
     * Checks if Redis is currently connected and available.
     */
    public static boolean isConnected() {
        return isConnected && pool != null && !pool.isClosed();
    }

    /**
     * Gracefully shuts down Redis connection pool.
     */
    public static void shutdown() {
        if (pool != null && !pool.isClosed()) {
            try {
                pool.close();
                isConnected = false;
                logger.info("Redis connection pool closed successfully.");
            } catch (Exception e) {
                logger.warning("Error closing Redis pool: " + e.getMessage());
            }
        }
    }

    /**
     * Gets connection pool statistics for monitoring.
     */
    public static String getPoolStats() {
        if (pool == null) return "Pool not initialized";
        
        return String.format("Active: %d, Idle: %d, Total: %d", 
                pool.getNumActive(), 
                pool.getNumIdle(), 
                pool.getNumActive() + pool.getNumIdle());
    }
}
