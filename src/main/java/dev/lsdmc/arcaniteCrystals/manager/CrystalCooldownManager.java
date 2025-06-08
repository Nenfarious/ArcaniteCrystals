package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CrystalCooldownManager {
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitRunnable> cooldownTasks = new ConcurrentHashMap<>();
    private static final long DEFAULT_COOLDOWN = 300000; // 5 minutes in milliseconds
    
    /**
     * Checks if a player is on cooldown.
     */
    public static boolean isOnCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        Long cooldownEnd = cooldowns.get(playerId);
        
        if (cooldownEnd == null) {
            return false;
        }
        
        if (System.currentTimeMillis() >= cooldownEnd) {
            cooldowns.remove(playerId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets the remaining cooldown time in milliseconds.
     */
    public static long getRemainingCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        Long cooldownEnd = cooldowns.get(playerId);
        
        if (cooldownEnd == null) {
            return 0;
        }
        
        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    /**
     * Starts a cooldown for a player.
     */
    public static void startCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel existing cooldown task if any
        cancelCooldown(player);
        
        // Set cooldown end time
        long cooldownEnd = System.currentTimeMillis() + DEFAULT_COOLDOWN;
        cooldowns.put(playerId, cooldownEnd);
        
        // Start cooldown task
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelCooldown(player);
                    return;
                }
                
                long remaining = getRemainingCooldown(player);
                if (remaining <= 0) {
                    cancelCooldown(player);
                    return;
                }
                
                // Play warning effects when cooldown is about to end
                if (remaining <= 5000) { // 5 seconds
                    ParticleManager.playCooldownWarningEffect(player);
                    SoundManager.playCooldownSound(player);
                }
            }
        };
        task.runTaskTimer(ArcaniteCrystals.getInstance(), 0L, 20L); // Every second
        cooldownTasks.put(playerId, task);
        
        // Notify player
        MessageManager.sendNotification(player, 
            "Crystal cooldown started. Time remaining: " + formatTime(DEFAULT_COOLDOWN / 1000), 
            MessageManager.NotificationType.INFO);
    }
    
    /**
     * Cancels a player's cooldown.
     */
    public static void cancelCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel cooldown task
        BukkitRunnable task = cooldownTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        
        // Remove cooldown
        cooldowns.remove(playerId);
    }
    
    /**
     * Formats time in seconds to readable format.
     */
    private static String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }
    
    /**
     * Gets the cooldown duration for a crystal tier.
     */
    public static long getCooldownDuration(int tier) {
        return switch (tier) {
            case 1 -> 300000;  // 5 minutes
            case 2 -> 600000;  // 10 minutes
            case 3 -> 900000;  // 15 minutes
            case 4 -> 1800000; // 30 minutes
            default -> 300000;
        };
    }
    
    /**
     * Cleans up all cooldown tasks.
     */
    public static void cleanup() {
        for (BukkitRunnable task : cooldownTasks.values()) {
            task.cancel();
        }
        cooldownTasks.clear();
        cooldowns.clear();
    }
} 