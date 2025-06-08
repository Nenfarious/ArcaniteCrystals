package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CrystalEnergyRegenerationManager {
    private static final Map<UUID, Integer> regenerationTasks = new HashMap<>();
    private static final int REGENERATION_INTERVAL = 20; // 1 second
    private static final int REGENERATION_AMOUNT = 1;
    
    /**
     * Starts energy regeneration for a player's crystal.
     */
    public static void startRegeneration(Player player, ItemStack crystal) {
        UUID playerId = player.getUniqueId();
        
        // Cancel existing regeneration task if any
        stopRegeneration(player);
        
        // Start new regeneration task
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    stopRegeneration(player);
                    return;
                }
                
                regenerateEnergy(player, crystal);
            }
        }.runTaskTimer(ArcaniteCrystals.getInstance(), REGENERATION_INTERVAL, REGENERATION_INTERVAL).getTaskId();
        
        regenerationTasks.put(playerId, taskId);
    }
    
    /**
     * Stops energy regeneration for a player.
     */
    public static void stopRegeneration(Player player) {
        UUID playerId = player.getUniqueId();
        Integer taskId = regenerationTasks.remove(playerId);
        
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
    
    /**
     * Regenerates energy for a crystal.
     */
    private static void regenerateEnergy(Player player, ItemStack crystal) {
        if (crystal == null || !crystal.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) {
            return;
        }
        
        int currentEnergy = meta.getPersistentDataContainer().getOrDefault(
            CrystalManager.KEY_ENERGY,
            PersistentDataType.INTEGER,
            0
        );
        
        int maxEnergy = CrystalManager.getMaxEnergy(crystal);
        
        if (currentEnergy < maxEnergy) {
            currentEnergy = Math.min(currentEnergy + REGENERATION_AMOUNT, maxEnergy);
            
            meta.getPersistentDataContainer().set(
                CrystalManager.KEY_ENERGY,
                PersistentDataType.INTEGER,
                currentEnergy
            );
            
            crystal.setItemMeta(meta);
            
            // Update crystal lore
            CrystalManager.updateCrystalLore(
                crystal,
                meta,
                CrystalManager.getCrystalEffects(crystal),
                currentEnergy,
                maxEnergy
            );
            
            // Play regeneration effect if energy is full
            if (currentEnergy == maxEnergy) {
                ParticleManager.playCrystalRechargeEffect(player);
                SoundManager.playCrystalRechargeSound(player);
            }
        }
    }
    
    /**
     * Gets the regeneration rate for a crystal tier.
     */
    public static int getRegenerationRate(int tier) {
        return switch (tier) {
            case 1 -> 1;  // Tier 1: 1 energy per second
            case 2 -> 2;  // Tier 2: 2 energy per second
            case 3 -> 3;  // Tier 3: 3 energy per second
            case 4 -> 5;  // Tier 4: 5 energy per second
            default -> 1;
        };
    }
    
    /**
     * Cleans up all regeneration tasks.
     */
    public static void cleanup() {
        for (int taskId : regenerationTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        regenerationTasks.clear();
    }
} 