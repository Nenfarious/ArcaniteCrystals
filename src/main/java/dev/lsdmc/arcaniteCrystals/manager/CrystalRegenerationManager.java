package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CrystalRegenerationManager {
    private static final Map<UUID, BukkitRunnable> regenerationTasks = new ConcurrentHashMap<>();
    private static final int REGENERATION_INTERVAL = 20; // 1 second
    private static final int REGENERATION_AMOUNT = 100; // Energy per tick
    private static final int MAX_ENERGY = 18000; // Maximum energy capacity
    
    /**
     * Starts energy regeneration for a crystal.
     */
    public static void startRegeneration(Player player, ItemStack crystal) {
        UUID playerId = player.getUniqueId();
        
        // Cancel existing regeneration task if any
        stopRegeneration(player);
        
        // Create new regeneration task
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    stopRegeneration(player);
                    return;
                }
                
                // Check if player still has the crystal
                if (!hasCrystal(player, crystal)) {
                    stopRegeneration(player);
                    return;
                }
                
                // Regenerate energy
                int currentEnergy = CrystalManager.getCurrentEnergy(crystal);
                if (currentEnergy < MAX_ENERGY) {
                    int newEnergy = Math.min(currentEnergy + REGENERATION_AMOUNT, MAX_ENERGY);
                    updateCrystalEnergy(crystal, newEnergy);
                    
                    // Play effects when fully recharged
                    if (newEnergy == MAX_ENERGY) {
                        ParticleManager.playCrystalRechargeEffect(player);
                        SoundManager.playCrystalRechargeSound(player);
                        MessageManager.sendNotification(player, 
                            "Crystal fully recharged!", 
                            MessageManager.NotificationType.SUCCESS);
                        stopRegeneration(player);
                    }
                }
            }
        };
        
        task.runTaskTimer(ArcaniteCrystals.getInstance(), 0L, REGENERATION_INTERVAL);
        regenerationTasks.put(playerId, task);
        
        MessageManager.sendNotification(player, 
            "Crystal regeneration started", 
            MessageManager.NotificationType.INFO);
    }
    
    /**
     * Stops energy regeneration for a player.
     */
    public static void stopRegeneration(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitRunnable task = regenerationTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Checks if a player has a specific crystal.
     */
    private static boolean hasCrystal(Player player, ItemStack crystal) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(crystal)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Updates a crystal's energy level.
     */
    private static void updateCrystalEnergy(ItemStack crystal, int energy) {
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) return;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(CrystalManager.KEY_ENERGY, PersistentDataType.INTEGER, energy);
        crystal.setItemMeta(meta);
    }
    
    /**
     * Handles crystal recharging with materials.
     */
    public static boolean rechargeCrystal(Player player, ItemStack crystal, Material material) {
        if (!CrystalManager.isDepletedCrystal(crystal)) {
            return false;
        }
        
        int rechargeAmount = getRechargeAmount(material);
        if (rechargeAmount <= 0) {
            return false;
        }
        
        // Remove recharge material
        ItemStack materialItem = new ItemStack(material);
        if (!player.getInventory().containsAtLeast(materialItem, 1)) {
            return false;
        }
        player.getInventory().removeItem(materialItem);
        
        // Recharge crystal
        int currentEnergy = CrystalManager.getCurrentEnergy(crystal);
        int newEnergy = Math.min(currentEnergy + rechargeAmount, MAX_ENERGY);
        updateCrystalEnergy(crystal, newEnergy);
        
        // Play effects
        ParticleManager.playCrystalRechargeEffect(player);
        SoundManager.playCrystalRechargeSound(player);
        
        MessageManager.sendNotification(player, 
            "Crystal recharged with " + material.name().toLowerCase().replace("_", " "), 
            MessageManager.NotificationType.SUCCESS);
        
        return true;
    }
    
    /**
     * Gets the recharge amount for a material.
     */
    private static int getRechargeAmount(Material material) {
        return switch (material) {
            case QUARTZ -> 1000;
            case AMETHYST_SHARD -> 2000;
            case DIAMOND -> 5000;
            case NETHERITE_INGOT -> 10000;
            default -> 0;
        };
    }
    
    /**
     * Cleans up all regeneration tasks.
     */
    public static void cleanup() {
        for (BukkitRunnable task : regenerationTasks.values()) {
            task.cancel();
        }
        regenerationTasks.clear();
    }
} 