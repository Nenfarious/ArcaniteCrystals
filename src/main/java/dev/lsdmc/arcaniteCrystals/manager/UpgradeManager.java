package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages crystal upgrades and their effects with proper effect tracking
 * and cleanup.
 */
public class UpgradeManager {
    private static final Map<UUID, Set<String>> activeEffects = new ConcurrentHashMap<>();
    private static final Map<String, Integer> effectAmplifiers = new ConcurrentHashMap<>();
    
    private final FileConfiguration upgradesConfig;
    
    public UpgradeManager() {
        this.upgradesConfig = ConfigManager.getUpgradesConfig();
    }
    
    /**
     * Applies an upgrade effect to a player.
     */
    public static void applyUpgradeEffect(Player player, String upgradeId) {
        UUID playerId = player.getUniqueId();
        ConfigurationSection upgrade = ConfigManager.getUpgradesConfig()
                .getConfigurationSection("upgrades." + upgradeId);
        
        if (upgrade == null) return;
        
        String effect = upgrade.getString("effect");
        int amplifier = upgrade.getInt("amplifier", 0);
        
        if (effect == null) return;
        
        // Track active effect
        activeEffects.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
                .add(upgradeId);
        
        // Store amplifier for this upgrade
        effectAmplifiers.put(upgradeId, amplifier);
        
        // Apply effect based on type
        switch (effect.toLowerCase()) {
            case "speed" -> applySpeedEffect(player, amplifier);
            case "regeneration" -> applyRegenerationEffect(player, amplifier);
            case "jump_boost" -> applyJumpBoostEffect(player, amplifier);
            case "haste" -> applyHasteEffect(player, amplifier);
            case "strength" -> applyStrengthEffect(player, amplifier);
            case "damage_resistance" -> applyDamageResistanceEffect(player, amplifier);
            case "poison" -> applyPoisonImmunityEffect(player, amplifier);
            default -> applyCustomEffect(player, effect, amplifier);
        }
    }
    
    /**
     * Removes an upgrade effect from a player.
     */
    public static void removeUpgradeEffect(Player player, String upgradeId) {
        UUID playerId = player.getUniqueId();
        ConfigurationSection upgrade = ConfigManager.getUpgradesConfig()
                .getConfigurationSection("upgrades." + upgradeId);
        
        if (upgrade == null) return;
        
        String effect = upgrade.getString("effect");
        if (effect == null) return;
        
        // Remove from active effects
        Set<String> playerEffects = activeEffects.get(playerId);
        if (playerEffects != null) {
            playerEffects.remove(upgradeId);
            if (playerEffects.isEmpty()) {
                activeEffects.remove(playerId);
            }
        }
        
        // Remove amplifier
        effectAmplifiers.remove(upgradeId);
        
        // Remove effect based on type
        switch (effect.toLowerCase()) {
            case "speed" -> removeSpeedEffect(player);
            case "regeneration" -> removeRegenerationEffect(player);
            case "jump_boost" -> removeJumpBoostEffect(player);
            case "haste" -> removeHasteEffect(player);
            case "strength" -> removeStrengthEffect(player);
            case "damage_resistance" -> removeDamageResistanceEffect(player);
            case "poison" -> removePoisonImmunityEffect(player);
            default -> removeCustomEffect(player, effect);
        }
    }
    
    /**
     * Gets the amplifier level for an upgrade.
     */
    public static int getUpgradeAmplifier(String upgradeId) {
        return effectAmplifiers.getOrDefault(upgradeId, 0);
    }
    
    /**
     * Checks if a player has a specific upgrade active.
     */
    public static boolean hasActiveUpgrade(UUID playerId, String upgradeId) {
        Set<String> playerEffects = activeEffects.get(playerId);
        return playerEffects != null && playerEffects.contains(upgradeId);
    }
    
    /**
     * Gets all active upgrades for a player.
     */
    public static Set<String> getActiveUpgrades(UUID playerId) {
        return activeEffects.getOrDefault(playerId, Set.of());
    }
    
    /**
     * Cleans up all active effects.
     * Should be called on plugin disable.
     */
    public static void cleanup() {
        activeEffects.clear();
        effectAmplifiers.clear();
    }
    
    // Effect application methods
    private static void applySpeedEffect(Player player, int amplifier) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, amplifier, true, false));
    }
    
    private static void applyRegenerationEffect(Player player, int amplifier) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, amplifier, true, false));
    }
    
    private static void applyJumpBoostEffect(Player player, int amplifier) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, amplifier, true, false));
    }
    
    private static void applyHasteEffect(Player player, int amplifier) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, amplifier, true, false));
    }
    
    private static void applyStrengthEffect(Player player, int amplifier) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, amplifier, true, false));
    }
    
    private static void applyDamageResistanceEffect(Player player, int amplifier) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, amplifier, true, false));
    }
    
    private static void applyPoisonImmunityEffect(Player player, int amplifier) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, Integer.MAX_VALUE, amplifier, true, false));
    }
    
    private static void applyCustomEffect(Player player, String effect, int amplifier) {
        PotionEffectType type = PotionEffectType.getByName(effect.toUpperCase());
        if (type != null) {
            player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, true, false));
        }
    }
    
    // Effect removal methods
    private static void removeSpeedEffect(Player player) {
        player.removePotionEffect(PotionEffectType.SPEED);
    }
    
    private static void removeRegenerationEffect(Player player) {
        player.removePotionEffect(PotionEffectType.REGENERATION);
    }
    
    private static void removeJumpBoostEffect(Player player) {
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
    }
    
    private static void removeHasteEffect(Player player) {
        player.removePotionEffect(PotionEffectType.HASTE);
    }
    
    private static void removeStrengthEffect(Player player) {
        player.removePotionEffect(PotionEffectType.STRENGTH);
    }
    
    private static void removeDamageResistanceEffect(Player player) {
        player.removePotionEffect(PotionEffectType.RESISTANCE);
    }
    
    private static void removePoisonImmunityEffect(Player player) {
        player.removePotionEffect(PotionEffectType.POISON);
    }
    
    private static void removeCustomEffect(Player player, String effect) {
        PotionEffectType type = PotionEffectType.getByName(effect.toUpperCase());
        if (type != null) {
            player.removePotionEffect(type);
        }
    }
} 