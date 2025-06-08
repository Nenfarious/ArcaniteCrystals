package dev.lsdmc.arcaniteCrystals.database;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Redis implementation of DataStore interface with proper connection pooling,
 * error handling, and async operations.
 */
public class RedisDataStore implements DataStore {
    private static final String LEVEL_KEY_PREFIX = "arcanite:level:";
    private static final String UPGRADES_KEY_PREFIX = "arcanite:upgrades:";
    private static final String COOLDOWN_KEY_PREFIX = "arcanite:cooldown:";
    private static final int DEFAULT_TTL = 86400 * 30; // 30 days
    
    private final JedisPool pool;
    private final Logger logger;
    private volatile boolean isHealthy = false;
    
    public RedisDataStore(FileConfiguration config, JavaPlugin plugin) {
        this.logger = plugin.getLogger();
        
        // Configure connection pool
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(config.getInt("database.redis.max-total", 128));
        poolConfig.setMaxIdle(config.getInt("database.redis.max-idle", 16));
        poolConfig.setMinIdle(config.getInt("database.redis.min-idle", 1));
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        
        // Create connection pool
        String host = config.getString("database.redis.host", "localhost");
        int port = config.getInt("database.redis.port", 6379);
        String password = config.getString("database.redis.password", "");
        int timeout = config.getInt("database.redis.timeout", 2000);
        
        if (password == null || password.trim().isEmpty()) {
            pool = new JedisPool(poolConfig, host, port, timeout);
        } else {
            pool = new JedisPool(poolConfig, host, port, timeout, password.trim());
        }
        
        // Test connection
        isHealthy = testConnection();
        if (isHealthy) {
            logger.info("Successfully connected to Redis at " + host + ":" + port);
        } else {
            logger.severe("Failed to establish Redis connection!");
        }
    }
    
    @Override
    public CompletableFuture<Integer> getLevel(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                String levelStr = jedis.get(LEVEL_KEY_PREFIX + playerId.toString());
                return levelStr != null ? Integer.parseInt(levelStr) : 1;
            } catch (Exception e) {
                logger.warning("Redis getLevel failed for " + playerId + ": " + e.getMessage());
                return 1; // Default level
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> setLevel(UUID playerId, int level) {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.setex(LEVEL_KEY_PREFIX + playerId.toString(), 
                           DEFAULT_TTL,
                           String.valueOf(level));
            } catch (Exception e) {
                logger.severe("Redis setLevel failed for " + playerId + ": " + e.getMessage());
                throw new DatabaseException("Redis operation failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Set<String>> getUnlockedUpgrades(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                Set<String> upgrades = jedis.smembers(UPGRADES_KEY_PREFIX + playerId.toString());
                return upgrades != null ? upgrades : new HashSet<>();
            } catch (Exception e) {
                logger.warning("Redis getUpgrades failed for " + playerId + ": " + e.getMessage());
                return new HashSet<>();
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> unlockUpgrade(UUID playerId, String upgradeId) {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                String key = UPGRADES_KEY_PREFIX + playerId.toString();
                jedis.sadd(key, upgradeId);
                jedis.expire(key, DEFAULT_TTL);
            } catch (Exception e) {
                logger.severe("Redis unlockUpgrade failed: " + e.getMessage());
                throw new DatabaseException("Redis operation failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> revokeUpgrade(UUID playerId, String upgradeId) {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                String key = UPGRADES_KEY_PREFIX + playerId.toString();
                jedis.srem(key, upgradeId);
            } catch (Exception e) {
                logger.severe("Redis revokeUpgrade failed: " + e.getMessage());
                throw new DatabaseException("Redis operation failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Long> getCooldown(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                String cooldownStr = jedis.get(COOLDOWN_KEY_PREFIX + playerId.toString());
                return cooldownStr != null ? Long.parseLong(cooldownStr) : 0L;
            } catch (Exception e) {
                logger.warning("Redis getCooldown failed for " + playerId + ": " + e.getMessage());
                return 0L;
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> setCooldown(UUID playerId, long timestamp) {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.setex(COOLDOWN_KEY_PREFIX + playerId.toString(),
                           DEFAULT_TTL,
                           String.valueOf(timestamp));
            } catch (Exception e) {
                logger.severe("Redis setCooldown failed: " + e.getMessage());
                throw new DatabaseException("Redis operation failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> saveBatch(Map<UUID, PlayerData> data) {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                for (Map.Entry<UUID, PlayerData> entry : data.entrySet()) {
                    UUID playerId = entry.getKey();
                    PlayerData playerData = entry.getValue();
                    
                    // Save level
                    jedis.setex(LEVEL_KEY_PREFIX + playerId.toString(),
                               DEFAULT_TTL,
                               String.valueOf(playerData.getLevel()));
                    
                    // Save upgrades
                    String upgradesKey = UPGRADES_KEY_PREFIX + playerId.toString();
                    jedis.del(upgradesKey); // Clear existing
                    if (!playerData.getUnlockedUpgrades().isEmpty()) {
                        jedis.sadd(upgradesKey, 
                                 playerData.getUnlockedUpgrades().toArray(new String[0]));
                        jedis.expire(upgradesKey, DEFAULT_TTL);
                    }
                    
                    // Save cooldown
                    jedis.setex(COOLDOWN_KEY_PREFIX + playerId.toString(),
                               DEFAULT_TTL,
                               String.valueOf(playerData.getCooldown()));
                }
            } catch (Exception e) {
                logger.severe("Redis batch save failed: " + e.getMessage());
                throw new DatabaseException("Redis batch operation failed", e);
            }
        });
    }
    
    @Override
    public boolean isHealthy() {
        return isHealthy && pool != null && !pool.isClosed();
    }
    
    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            if (pool != null && !pool.isClosed()) {
                try {
                    pool.close();
                    isHealthy = false;
                    logger.info("Redis connection pool closed successfully.");
                } catch (Exception e) {
                    logger.warning("Error closing Redis pool: " + e.getMessage());
                }
            }
        });
    }
    
    @Override
    public String getStats() {
        if (pool == null) return "Pool not initialized";
        
        return String.format("Active: %d, Idle: %d, Total: %d", 
                pool.getNumActive(), 
                pool.getNumIdle(), 
                pool.getNumActive() + pool.getNumIdle());
    }
    
    private boolean testConnection() {
        if (pool == null) return false;
        
        try (Jedis jedis = pool.getResource()) {
            String response = jedis.ping();
            return "PONG".equals(response);
        } catch (Exception e) {
            logger.warning("Redis connection test failed: " + e.getMessage());
            return false;
        }
    }
} 