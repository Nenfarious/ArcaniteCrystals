package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CrystalEffectManager {
    private static final Map<UUID, Map<String, Integer>> activeEffects = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitRunnable> effectTasks = new ConcurrentHashMap<>();
    
    /**
     * Applies crystal effects to a player.
     */
    public static void applyEffects(Player player, Map<String, Integer> effects) {
        UUID playerId = player.getUniqueId();
        
        // Cancel existing effect task if any
        cancelEffects(player);
        
        // Store active effects
        activeEffects.put(playerId, new HashMap<>(effects));
        
        // Start effect task
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelEffects(player);
                    return;
                }
                
                applyEffectTick(player);
            }
        };
        task.runTaskTimer(ArcaniteCrystals.getInstance(), 0L, 20L); // Every second
        effectTasks.put(playerId, task);
        
        // Play activation effects
        ParticleManager.playCrystalActivationEffect(player);
        SoundManager.playCrystalActivateSound(player);
        
        MessageManager.sendNotification(player, 
            "Crystal effects activated!", 
            MessageManager.NotificationType.SUCCESS);
    }
    
    /**
     * Cancels all active effects for a player.
     */
    public static void cancelEffects(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel effect task
        BukkitRunnable task = effectTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        
        // Remove all effects
        Map<String, Integer> effects = activeEffects.remove(playerId);
        if (effects != null) {
            for (String effect : effects.keySet()) {
                removeEffect(player, effect);
            }
        }
        
        // Play deactivation effects
        ParticleManager.playCrystalDepletionEffect(player);
        SoundManager.playCrystalDepletionSound(player);
        
        MessageManager.sendNotification(player, 
            "Crystal effects deactivated.", 
            MessageManager.NotificationType.INFO);
    }
    
    /**
     * Applies effects for a single tick.
     */
    private static void applyEffectTick(Player player) {
        Map<String, Integer> effects = activeEffects.get(player.getUniqueId());
        if (effects == null) {
            return;
        }
        
        for (Map.Entry<String, Integer> entry : effects.entrySet()) {
            String effect = entry.getKey();
            int level = entry.getValue();
            
            applyEffect(player, effect, level);
        }
        
        // Play ambient effects
        ParticleManager.playActiveAuraEffect(player);
        SoundManager.playAmbientCrystalSound(player);
    }
    
    /**
     * Applies a single effect to a player.
     */
    private static void applyEffect(Player player, String effect, int level) {
        PotionEffectType type = getEffectType(effect);
        if (type == null) {
            return;
        }
        
        player.addPotionEffect(new PotionEffect(type, 40, level - 1, true, false));
    }
    
    /**
     * Removes a single effect from a player.
     */
    private static void removeEffect(Player player, String effect) {
        PotionEffectType type = getEffectType(effect);
        if (type == null) {
            return;
        }
        
        player.removePotionEffect(type);
    }
    
    /**
     * Gets the PotionEffectType for an effect name.
     */
    private static PotionEffectType getEffectType(String effect) {
        return switch (effect.toLowerCase()) {
            case "strength" -> PotionEffectType.STRENGTH;
            case "speed" -> PotionEffectType.SPEED;
            case "jump_boost" -> PotionEffectType.JUMP_BOOST;
            case "regeneration" -> PotionEffectType.REGENERATION;
            case "resistance" -> PotionEffectType.RESISTANCE;
            case "fire_resistance" -> PotionEffectType.FIRE_RESISTANCE;
            case "water_breathing" -> PotionEffectType.WATER_BREATHING;
            case "night_vision" -> PotionEffectType.NIGHT_VISION;
            case "invisibility" -> PotionEffectType.INVISIBILITY;
            case "glowing" -> PotionEffectType.GLOWING;
            default -> null;
        };
    }
    
    /**
     * Gets the current active effects for a player.
     */
    public static Map<String, Integer> getActiveEffects(Player player) {
        return activeEffects.getOrDefault(player.getUniqueId(), new HashMap<>());
    }
    
    /**
     * Checks if a player has a specific effect active.
     */
    public static boolean hasEffect(Player player, String effect) {
        Map<String, Integer> effects = activeEffects.get(player.getUniqueId());
        return effects != null && effects.containsKey(effect);
    }
    
    /**
     * Gets the level of a specific effect for a player.
     */
    public static int getEffectLevel(Player player, String effect) {
        Map<String, Integer> effects = activeEffects.get(player.getUniqueId());
        return effects != null ? effects.getOrDefault(effect, 0) : 0;
    }
} 