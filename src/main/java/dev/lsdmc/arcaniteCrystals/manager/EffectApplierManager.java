package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.util.EffectUtils;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Professional effect application manager with comprehensive performance monitoring,
 * sophisticated error handling, and robust task management.
 */
public class EffectApplierManager {

    private static final Set<UUID> activeUsers = ConcurrentHashMap.newKeySet();
    private static BukkitTask task;
    private static JavaPlugin plugin;
    private static Logger logger;
    private static final NamespacedKey KEY_ENERGY = new NamespacedKey(
            ArcaniteCrystals.getInstance(), "energy"
    );
    
    // Professional performance monitoring
    private static long totalExecutions = 0;
    private static long totalErrors = 0;
    private static long lastExecutionTime = 0;
    private static long totalProcessingTime = 0;
    private static long peakActiveUsers = 0;
    private static long startTime = System.currentTimeMillis();
    
    // Rate limiting for error messages to prevent console spam
    private static long lastErrorLogTime = 0;
    private static final long ERROR_LOG_INTERVAL = 30000; // 30 seconds
    
    // Performance thresholds for professional monitoring
    private static final long WARNING_EXECUTION_TIME = 50; // ms
    private static final long CRITICAL_EXECUTION_TIME = 100; // ms
    private static final double MAX_ERROR_RATE = 0.05; // 5%

    /**
     * Starts the professional effect application system with comprehensive monitoring.
     */
    public static void start(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
        logger = plugin.getLogger();
        
        if (task != null && !task.isCancelled()) {
            logger.warning("EffectApplierManager is already running! Stopping existing task.");
            stop();
        }

        long drainAmount = plugin.getConfig().getLong("crystal.drain", 80);
        long intervalTicks = plugin.getConfig().getLong("crystal.task-interval", 80L);
        
        // Validate configuration with professional standards
        if (drainAmount <= 0) {
            logger.warning("Invalid drain amount: " + drainAmount + ". Using default of 80.");
            drainAmount = 80;
        }
        
        if (intervalTicks <= 0 || intervalTicks > 1200) { // Max 1 minute
            logger.warning("Invalid task interval: " + intervalTicks + ". Using default of 80 ticks.");
            intervalTicks = 80L;
        }

        final long finalDrainAmount = drainAmount;
        startTime = System.currentTimeMillis();
        
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long executionStart = System.nanoTime();
            
            try {
                processActivePlayers(finalDrainAmount);
                
                // Professional performance tracking
                long executionTimeNs = System.nanoTime() - executionStart;
                lastExecutionTime = executionTimeNs / 1_000_000; // Convert to ms
                totalProcessingTime += lastExecutionTime;
                totalExecutions++;
                
                // Update peak statistics
                int currentUsers = activeUsers.size();
                if (currentUsers > peakActiveUsers) {
                    peakActiveUsers = currentUsers;
                }
                
                // Performance monitoring and alerts
                if (lastExecutionTime > CRITICAL_EXECUTION_TIME) {
                    logger.severe("CRITICAL: Effect task took " + lastExecutionTime + "ms! " +
                                "This may cause server lag. Active users: " + currentUsers);
                } else if (lastExecutionTime > WARNING_EXECUTION_TIME) {
                    logRateLimited("WARNING: Effect task took " + lastExecutionTime + "ms. " +
                                 "Consider optimizing. Active users: " + currentUsers);
                }
                
            } catch (Exception e) {
                totalErrors++;
                logger.severe("Critical error in EffectApplierManager: " + e.getMessage());
                e.printStackTrace();
                
                // Sophisticated error rate monitoring
                if (totalExecutions > 10) {
                    double errorRate = (double) totalErrors / totalExecutions;
                    if (errorRate > MAX_ERROR_RATE) {
                        logger.severe("ERROR RATE CRITICAL: " + String.format("%.2f%%", errorRate * 100) + 
                                     " (" + totalErrors + "/" + totalExecutions + "). Consider investigating!");
                        
                        // Auto-recovery mechanism
                        if (errorRate > 0.20) { // 20% error rate - emergency stop
                            logger.severe("EMERGENCY STOP: Error rate exceeds 20%. Stopping EffectApplierManager.");
                            stop();
                        }
                    }
                }
            }
        }, 0L, intervalTicks);

        logger.info("EffectApplierManager started successfully with professional monitoring.");
        logger.info("Configuration: Drain=" + drainAmount + "/cycle, Interval=" + intervalTicks + " ticks");
    }

    /**
     * Processes all active players with sophisticated error handling and monitoring.
     */
    private static void processActivePlayers(long drainAmount) {
        if (activeUsers.isEmpty()) return;
        
        // Create a snapshot of active users to avoid concurrent modification
        Set<UUID> activeUsersSnapshot = new HashSet<>(activeUsers);
        Set<UUID> toRemove = new HashSet<>();
        int processed = 0;
        int failed = 0;
        
        for (UUID uuid : activeUsersSnapshot) {
            try {
                if (processPlayer(uuid, drainAmount)) {
                    processed++;
                } else {
                    toRemove.add(uuid);
                }
            } catch (Exception e) {
                failed++;
                logRateLimited("Error processing player " + uuid + ": " + e.getMessage());
                // Don't remove on error - let them retry next cycle for resilience
            }
        }
        
        // Remove inactive players in a thread-safe manner
        if (!toRemove.isEmpty()) {
            activeUsers.removeAll(toRemove);
        }
        
        // Professional logging for monitoring
        if (processed > 0 || failed > 0 || !toRemove.isEmpty()) {
            logger.fine("Processed " + processed + " players, removed " + toRemove.size() + 
                       " inactive, " + failed + " errors");
        }
    }
    
    /**
     * Processes individual player with comprehensive validation and error recovery.
     */
    private static boolean processPlayer(UUID uuid, long drainAmount) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return false; // Player offline, remove from active list
        }

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (!CrystalManager.isActivatedCrystal(offHand)) {
            return false; // No active crystal, remove from list
        }

        // Get abilities with validation
        List<String> abilities = CrystalManager.getAbilities(offHand);
        if (abilities == null || abilities.isEmpty()) {
            return false; // No abilities, crystal may be corrupted
        }

        // Apply potion effects with individual error handling
        for (String upgrade : abilities) {
            try {
                EffectUtils.applyEffect(player, upgrade);
            } catch (Exception e) {
                logRateLimited("Failed to apply effect " + upgrade + " to " + player.getName() + 
                              ": " + e.getMessage());
            }
        }

        // Apply mining effects with sophisticated detection
        try {
            boolean hasAutoSmelt = abilities.stream().anyMatch(id -> 
                id.toLowerCase().contains("auto_smelt") || id.toLowerCase().contains("haste"));
            boolean hasFortune = abilities.stream().anyMatch(id -> 
                id.toLowerCase().contains("fortune") || id.toLowerCase().contains("strength"));
            
            if (hasAutoSmelt) {
                long duration = plugin.getConfig().getLong("mining.effect-duration", 200);
                MiningEffectManager.applyAutoSmelt(player, duration);
            }
            
            if (hasFortune) {
                long duration = plugin.getConfig().getLong("mining.effect-duration", 200);
                MiningEffectManager.applyFortune(player, duration);
            }
        } catch (Exception e) {
            logRateLimited("Error applying mining effects to " + player.getName() + ": " + e.getMessage());
        }

        // Drain energy with professional validation
        return drainCrystalEnergy(player, offHand, drainAmount);
    }

    /**
     * Professional energy draining with comprehensive error handling and user feedback.
     */
    private static boolean drainCrystalEnergy(Player player, ItemStack crystal, long drainAmount) {
        try {
            ItemMeta meta = crystal.getItemMeta();
            if (meta == null) {
                logRateLimited("Crystal has no metadata for player " + player.getName());
                return false;
            }

            int currentEnergy = CrystalManager.getEnergy(crystal);
            int remaining = Math.max(0, currentEnergy - (int) drainAmount);

            if (remaining <= 0) {
                // Crystal depleted - professional handling
                meta.getPersistentDataContainer().set(KEY_ENERGY, PersistentDataType.INTEGER, 0);
                crystal.setType(Material.GRAY_DYE);
                crystal.setItemMeta(meta);
                
                // Professional depletion notification
                String message = MessageManager.get("info.depleted");
                if (message != null && !message.trim().isEmpty()) {
                    player.sendMessage(message);
                } else {
                    // Fallback message
                    player.sendMessage(MessageManager.ERROR + "Your crystal has run out of energy!");
                }
                
                logger.fine("Crystal depleted for player " + player.getName());
                return false;
                
            } else {
                // Update energy with validation
                meta.getPersistentDataContainer().set(KEY_ENERGY, PersistentDataType.INTEGER, remaining);
                crystal.setItemMeta(meta);
                
                // Low energy warning (professional UX)
                double energyPercentage = (double) remaining / plugin.getConfig().getInt("crystal.energy", 18000);
                if (energyPercentage <= 0.1 && Math.random() < 0.1) { // 10% chance at 10% energy
                    player.sendMessage(MessageManager.WARNING + "⚠ Crystal energy low: " + 
                                     MessageManager.ACCENT + (int)(energyPercentage * 100) + "%");
                }
                
                return true;
            }
        } catch (Exception e) {
            logRateLimited("Failed to drain crystal energy for " + player.getName() + ": " + e.getMessage());
            return true; // Don't remove on update failure - resilient approach
        }
    }
    
    /**
     * Professional rate-limited logging to prevent console spam.
     */
    private static void logRateLimited(String message) {
        long now = System.currentTimeMillis();
        if (now - lastErrorLogTime > ERROR_LOG_INTERVAL) {
            logger.warning(message);
            lastErrorLogTime = now;
        }
    }

    /**
     * Professional shutdown with comprehensive cleanup and statistics.
     */
    public static void stop() {
        if (task != null) {
            task.cancel();
            task = null;
            
            // Professional shutdown statistics
            long uptimeMs = System.currentTimeMillis() - startTime;
            double avgExecutionTime = totalExecutions > 0 ? (double) totalProcessingTime / totalExecutions : 0;
            double errorRate = totalExecutions > 0 ? (double) totalErrors / totalExecutions * 100 : 0;
            
            logger.info("EffectApplierManager stopped successfully.");
            logger.info("Session Statistics:");
            logger.info("  Uptime: " + (uptimeMs / 1000) + "s");
            logger.info("  Executions: " + totalExecutions);
            logger.info("  Errors: " + totalErrors + " (" + String.format("%.2f%%", errorRate) + ")");
            logger.info("  Avg Execution Time: " + String.format("%.2fms", avgExecutionTime));
            logger.info("  Peak Active Users: " + peakActiveUsers);
            logger.info("  Final Active Users: " + activeUsers.size());
            
            activeUsers.clear();
        }
    }

    /**
     * Adds a player to active effects with professional logging.
     */
    public static void addActive(UUID uuid) {
        if (uuid != null) {
            boolean added = activeUsers.add(uuid);
            if (added) {
                logger.fine("Added player " + uuid + " to active effects. Total: " + activeUsers.size());
            }
        }
    }

    /**
     * Removes a player from active effects with professional logging.
     */
    public static void removeActive(UUID uuid) {
        if (uuid != null) {
            boolean removed = activeUsers.remove(uuid);
            if (removed) {
                logger.fine("Removed player " + uuid + " from active effects. Total: " + activeUsers.size());
            }
        }
    }

    /**
     * Gets comprehensive performance statistics for professional monitoring.
     */
    public static String getStats() {
        double errorRate = totalExecutions > 0 ? (double) totalErrors / totalExecutions * 100 : 0;
        double avgExecutionTime = totalExecutions > 0 ? (double) totalProcessingTime / totalExecutions : 0;
        long uptimeMs = System.currentTimeMillis() - startTime;
        
        return String.format(
            "Active: %d, Peak: %d, Executions: %d, Errors: %d (%.2f%%), " +
            "Avg Time: %.2fms, Last: %dms, Uptime: %ds",
            activeUsers.size(), peakActiveUsers, totalExecutions, totalErrors, errorRate,
            avgExecutionTime, lastExecutionTime, uptimeMs / 1000
        );
    }

    /**
     * Gets detailed health status for professional monitoring.
     */
    public static String getHealthStatus() {
        boolean healthy = isRunning() && 
                         (totalExecutions == 0 || (double) totalErrors / totalExecutions < MAX_ERROR_RATE) &&
                         lastExecutionTime < CRITICAL_EXECUTION_TIME;
        
        return healthy ? "HEALTHY" : "DEGRADED";
    }

    /**
     * Checks if the manager is running professionally.
     */
    public static boolean isRunning() {
        return task != null && !task.isCancelled();
    }

    /**
     * Gets active user count for monitoring.
     */
    public static int getActiveUserCount() {
        return activeUsers.size();
    }

    /**
     * Emergency cleanup for professional administration.
     */
    public static void clearAllActive() {
        int count = activeUsers.size();
        activeUsers.clear();
        logger.warning("Emergency: Cleared " + count + " active users from EffectApplierManager.");
    }

    /**
     * Resets statistics for professional monitoring.
     */
    public static void resetStatistics() {
        totalExecutions = 0;
        totalErrors = 0;
        totalProcessingTime = 0;
        peakActiveUsers = activeUsers.size();
        startTime = System.currentTimeMillis();
        logger.info("EffectApplierManager statistics reset.");
    }
}
