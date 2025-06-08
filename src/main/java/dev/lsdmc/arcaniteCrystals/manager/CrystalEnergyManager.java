package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages crystal energy system with proper race condition prevention
 * and thread-safe operations.
 */
public class CrystalEnergyManager {
    private static final Map<UUID, AtomicInteger> activeCrystals = new ConcurrentHashMap<>();
    private static final Map<UUID, AtomicBoolean> activationLocks = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitTask> drainTasks = new ConcurrentHashMap<>();
    
    private final FileConfiguration config;
    private final int maxEnergy;
    private final int drainRate;
    private final int taskInterval;
    
    public CrystalEnergyManager() {
        this.config = ConfigManager.getConfig();
        this.maxEnergy = config.getInt("crystal.energy", 18000);
        this.drainRate = config.getInt("crystal.drain", 80);
        this.taskInterval = config.getInt("crystal.task-interval", 80);
    }
    
    /**
     * Attempts to activate a crystal for a player with race condition prevention.
     * Returns true if activation was successful.
     */
    public boolean activateCrystal(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Check if player already has an active crystal
        if (activeCrystals.containsKey(playerId)) {
            MessageManager.sendNotification(player, MessageManager.get("crystal.already-active"), MessageManager.NotificationType.WARNING);
            return false;
        }
        
        // Try to acquire activation lock
        AtomicBoolean lock = activationLocks.computeIfAbsent(playerId, k -> new AtomicBoolean(false));
        if (!lock.compareAndSet(false, true)) {
            MessageManager.sendNotification(player, MessageManager.get("crystal.activation-in-progress"), MessageManager.NotificationType.WARNING);
            return false;
        }
        
        try {
            // Check cooldown
            long cooldown = PlayerDataManager.getCooldown(playerId);
            if (cooldown > System.currentTimeMillis()) {
                MessageManager.sendNotification(player, MessageManager.get("crystal.cooldown"), MessageManager.NotificationType.WARNING);
                return false;
            }
            
            // Check if player has a crystal
            if (!hasCrystal(player)) {
                MessageManager.sendNotification(player, MessageManager.get("crystal.not-found"), MessageManager.NotificationType.ERROR);
                return false;
            }
            
            // Initialize crystal energy
            activeCrystals.put(playerId, new AtomicInteger(maxEnergy));
            
            // Start drain task
            startDrainTask(player);
            
            // Apply effects
            applyCrystalEffects(player);
            
            // Play effects
            SoundManager.playCrystalActivateSound(player);
            ParticleManager.playCrystalActivationEffect(player);
            
            MessageManager.sendNotification(player, MessageManager.get("crystal.activate"), MessageManager.NotificationType.SUCCESS);
            return true;
            
        } finally {
            // Always release lock
            lock.set(false);
        }
    }
    
    /**
     * Deactivates a crystal for a player with proper cleanup.
     */
    public void deactivateCrystal(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Stop drain task
        BukkitTask task = drainTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        
        // Remove crystal from active crystals
        activeCrystals.remove(playerId);
        
        // Remove effects
        removeCrystalEffects(player);
        
        // Play effects
        SoundManager.playCrystalDepletionSound(player);
        ParticleManager.playCrystalDepletionEffect(player);
        
        MessageManager.sendNotification(player, MessageManager.get("crystal.deactivate"), MessageManager.NotificationType.INFO);
    }
    
    /**
     * Checks if a player has a crystal in their inventory.
     */
    private boolean hasCrystal(Player player) {
        Material crystalMaterial = Material.valueOf(config.getString("crystal.material", "DIAMOND"));
        return player.getInventory().contains(crystalMaterial);
    }
    
    /**
     * Starts the energy drain task for a player.
     */
    private void startDrainTask(Player player) {
        UUID playerId = player.getUniqueId();
        
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(ArcaniteCrystals.getInstance(), () -> {
            AtomicInteger energy = activeCrystals.get(playerId);
            if (energy == null) return;
            
            int newEnergy = energy.addAndGet(-drainRate);
            
            if (newEnergy <= 0) {
                // Crystal depleted
                deactivateCrystal(player);
                MessageManager.sendNotification(player, MessageManager.get("crystal.depleted"), MessageManager.NotificationType.WARNING);
                
                // Set cooldown
                long cooldownTime = System.currentTimeMillis() + 
                    (config.getInt("crystal.cooldown", 300) * 1000L);
                PlayerDataManager.setCooldown(playerId, cooldownTime);
            }
        }, taskInterval, taskInterval);
        
        drainTasks.put(playerId, task);
    }
    
    /**
     * Applies crystal effects to a player based on their upgrades.
     */
    private void applyCrystalEffects(Player player) {
        Set<String> upgrades = PlayerDataManager.getUnlockedUpgrades(player.getUniqueId());
        
        for (String upgradeId : upgrades) {
            // Apply effect based on upgrade
            // This will be implemented in the UpgradeManager
            UpgradeManager.applyUpgradeEffect(player, upgradeId);
        }
    }
    
    /**
     * Removes crystal effects from a player.
     */
    private void removeCrystalEffects(Player player) {
        Set<String> upgrades = PlayerDataManager.getUnlockedUpgrades(player.getUniqueId());
        
        for (String upgradeId : upgrades) {
            // Remove effect based on upgrade
            // This will be implemented in the UpgradeManager
            UpgradeManager.removeUpgradeEffect(player, upgradeId);
        }
    }
    
    /**
     * Gets the current energy level of a player's active crystal.
     * Returns -1 if no crystal is active.
     */
    public int getEnergyLevel(UUID playerId) {
        AtomicInteger energy = activeCrystals.get(playerId);
        return energy != null ? energy.get() : -1;
    }
    
    /**
     * Gets the energy percentage of a player's active crystal.
     * Returns 0 if no crystal is active.
     */
    public double getEnergyPercentage(UUID playerId) {
        AtomicInteger energy = activeCrystals.get(playerId);
        return energy != null ? (double) energy.get() / maxEnergy : 0;
    }
    
    /**
     * Checks if a player has an active crystal.
     */
    public boolean hasActiveCrystal(UUID playerId) {
        return activeCrystals.containsKey(playerId);
    }
    
    /**
     * Cleans up all active crystals and tasks.
     * Should be called on plugin disable.
     */
    public void cleanup() {
        // Cancel all drain tasks
        drainTasks.values().forEach(BukkitTask::cancel);
        drainTasks.clear();
        
        // Clear all active crystals
        activeCrystals.clear();
        
        // Clear all activation locks
        activationLocks.clear();
    }
} 