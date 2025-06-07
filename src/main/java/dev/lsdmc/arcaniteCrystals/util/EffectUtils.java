// src/main/java/dev/lsdmc/arcaniteCrystals/util/EffectUtils.java
package dev.lsdmc.arcaniteCrystals.util;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.manager.LevelManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Professional effect utility system with comprehensive potion effect management,
 * crystal item creation, effect validation, and dynamic scaling based on player progress.
 */
public class EffectUtils {

    private static final ArcaniteCrystals plugin = ArcaniteCrystals.getInstance();
    private static final FileConfiguration upgradesCfg = ConfigManager.getUpgradesConfig();

    // Comprehensive effect type mapping for modern compatibility
    private static final Map<String, PotionEffectType> EFFECT_TYPE_MAPPING;
    
    // Effect duration multipliers based on tier
    private static final Map<Integer, Double> TIER_DURATION_MULTIPLIERS = Map.of(
        1, 1.0,   // Base duration
        2, 1.25,  // 25% longer
        3, 1.5    // 50% longer
    );
    
    // Effect amplifier bonuses based on player level
    private static final Map<Integer, Integer> LEVEL_AMPLIFIER_BONUS = Map.of(
        1, 0, 2, 0, 3, 0,     // Levels 1-3: No bonus
        4, 0, 5, 1, 6, 1,     // Levels 4-6: +1 amplifier 
        7, 1, 8, 2, 9, 2,     // Levels 7-9: +1-2 amplifier
        10, 3                 // Level 10: +3 amplifier (mastery)
    );

    static {
        Map<String, PotionEffectType> effectMap = new HashMap<>();
        
        // Primary effects - most commonly used
        effectMap.put("SPEED", PotionEffectType.SPEED);
        effectMap.put("REGENERATION", PotionEffectType.REGENERATION);
        effectMap.put("JUMP_BOOST", PotionEffectType.JUMP_BOOST);
        effectMap.put("HASTE", PotionEffectType.HASTE);
        effectMap.put("STRENGTH", PotionEffectType.STRENGTH);
        effectMap.put("DAMAGE_RESISTANCE", PotionEffectType.RESISTANCE);
        effectMap.put("RESISTANCE", PotionEffectType.RESISTANCE);
        effectMap.put("POISON", PotionEffectType.POISON);
        
        // Extended effects - additional options
        effectMap.put("NIGHT_VISION", PotionEffectType.NIGHT_VISION);
        effectMap.put("INVISIBILITY", PotionEffectType.INVISIBILITY);
        effectMap.put("WATER_BREATHING", PotionEffectType.WATER_BREATHING);
        effectMap.put("FIRE_RESISTANCE", PotionEffectType.FIRE_RESISTANCE);
        effectMap.put("SATURATION", PotionEffectType.SATURATION);
        effectMap.put("ABSORPTION", PotionEffectType.ABSORPTION);
        effectMap.put("WEAKNESS", PotionEffectType.WEAKNESS);
        effectMap.put("SLOWNESS", PotionEffectType.SLOWNESS);
        effectMap.put("MINING_FATIGUE", PotionEffectType.MINING_FATIGUE);
        effectMap.put("NAUSEA", PotionEffectType.NAUSEA);
        effectMap.put("BLINDNESS", PotionEffectType.BLINDNESS);
        effectMap.put("HUNGER", PotionEffectType.HUNGER);
        effectMap.put("WITHER", PotionEffectType.WITHER);
        effectMap.put("HEALTH_BOOST", PotionEffectType.HEALTH_BOOST);
        
        EFFECT_TYPE_MAPPING = Collections.unmodifiableMap(effectMap);
    }

    /**
     * Enhanced crystal item builder with professional formatting and validation.
     */
    public static ItemStack buildCrystalItem(Material mat, String name, List<String> lore) {
        try {
            // Validate material
            if (mat == null || mat == Material.AIR) {
                plugin.getLogger().warning("Invalid material for crystal item, using DIAMOND as fallback");
                mat = Material.DIAMOND;
            }
            
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            
            if (meta == null) {
                plugin.getLogger().severe("Could not get ItemMeta for crystal item!");
                return item;
            }
            
            // Enhanced name formatting with validation
            if (name != null && !name.trim().isEmpty()) {
                meta.setDisplayName(ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', name));
            } else {
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Arcanite Crystal");
            }
            
            // Enhanced lore formatting with professional styling
            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    if (line != null) {
                        coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                    }
                }
                meta.setLore(coloredLore);
            }
            
            item.setItemMeta(meta);
            return item;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error building crystal item: " + e.getMessage());
            e.printStackTrace();
            
            // Return safe fallback
            ItemStack fallback = new ItemStack(Material.DIAMOND);
            ItemMeta fallbackMeta = fallback.getItemMeta();
            if (fallbackMeta != null) {
                fallbackMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Arcanite Crystal (Error)");
                fallback.setItemMeta(fallbackMeta);
            }
            return fallback;
        }
    }

    /**
     * Enhanced lore setter with comprehensive formatting and validation.
     */
    public static void setItemLore(ItemStack item, List<String> lore) {
        if (item == null || lore == null) {
            return;
        }
        
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                plugin.getLogger().warning("Could not get ItemMeta for lore setting");
                return;
            }
            
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                if (line != null) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
            }
            
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error setting item lore: " + e.getMessage());
        }
    }

    /**
     * Enhanced effect application with dynamic scaling and professional validation.
     */
    public static void applyEffect(Player player, String upgradeId) {
        if (player == null || upgradeId == null || upgradeId.trim().isEmpty()) {
            plugin.getLogger().warning("Invalid parameters for effect application");
            return;
        }
        
        try {
            // Get upgrade configuration with validation
            ConfigurationSection upgradeSection = upgradesCfg.getConfigurationSection("upgrades." + upgradeId);
            if (upgradeSection == null) {
                plugin.getLogger().warning("No configuration found for upgrade: " + upgradeId);
                return;
            }
            
            // Extract effect information
            String effectName = upgradeSection.getString("effect");
            int configAmplifier = upgradeSection.getInt("amplifier", 0);
            int configDuration = upgradeSection.getInt("duration", 200);
            
            if (effectName == null || effectName.trim().isEmpty()) {
                plugin.getLogger().warning("No effect name specified for upgrade: " + upgradeId);
                return;
            }
            
            // Get PotionEffectType with modern compatibility
            PotionEffectType effectType = getEffectType(effectName);
            if (effectType == null) {
                plugin.getLogger().warning("Unknown effect type: " + effectName + " for upgrade: " + upgradeId);
                return;
            }
            
            // Calculate dynamic scaling
            DynamicEffectData scaledEffect = calculateDynamicEffect(
                player, upgradeId, configAmplifier, configDuration);
            
            // Validate final values
            if (scaledEffect.duration <= 0) {
                plugin.getLogger().warning("Invalid duration calculated for " + upgradeId + ": " + scaledEffect.duration);
                return;
            }
            
            // Apply the effect with enhanced parameters
            PotionEffect effect = new PotionEffect(
                effectType,
                scaledEffect.duration,
                Math.max(0, scaledEffect.amplifier), // Ensure non-negative
                false, // Not ambient
                false  // Show particles (false for cleaner look)
            );
            
            // Remove existing effect of same type to prevent conflicts
            player.removePotionEffect(effectType);
            
            // Apply new effect
            boolean applied = player.addPotionEffect(effect, true);
            
            if (applied) {
                plugin.getLogger().fine("Applied " + effectName + " (Level " + (scaledEffect.amplifier + 1) + 
                                      ") to " + player.getName() + " for " + (scaledEffect.duration / 20) + "s");
            } else {
                plugin.getLogger().warning("Failed to apply effect " + effectName + " to " + player.getName());
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error applying effect " + upgradeId + " to " + player.getName() + 
                                     ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Calculates dynamic effect scaling based on player level and tier.
     */
    private static DynamicEffectData calculateDynamicEffect(Player player, String upgradeId, 
                                                           int baseAmplifier, int baseDuration) {
        try {
            UUID playerId = player.getUniqueId();
            int playerLevel = PlayerDataManager.getLevel(playerId);
            int tier = LevelManager.getTier(upgradeId);
            
            // Calculate amplifier with level bonus
            int levelBonus = LEVEL_AMPLIFIER_BONUS.getOrDefault(playerLevel, 0);
            int finalAmplifier = Math.min(baseAmplifier + levelBonus, 10); // Cap at level 10 effect
            
            // Calculate duration with tier multiplier
            double tierMultiplier = TIER_DURATION_MULTIPLIERS.getOrDefault(tier, 1.0);
            int finalDuration = (int) (baseDuration * tierMultiplier);
            
            // Apply level-based duration bonus (max 50% bonus at level 10)
            double levelDurationBonus = 1.0 + (playerLevel - 1) * 0.05; // 5% per level above 1
            finalDuration = (int) (finalDuration * Math.min(levelDurationBonus, 1.5));
            
            // Ensure minimum duration
            finalDuration = Math.max(finalDuration, 100); // At least 5 seconds
            
            return new DynamicEffectData(finalAmplifier, finalDuration);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error calculating dynamic effect for " + upgradeId + ", using base values");
            return new DynamicEffectData(baseAmplifier, baseDuration);
        }
    }

    /**
     * Gets PotionEffectType with comprehensive compatibility handling.
     */
    private static PotionEffectType getEffectType(String effectName) {
        if (effectName == null) return null;
        
        try {
            // First try direct Bukkit lookup by name
            PotionEffectType directType = PotionEffectType.getByName(effectName.toUpperCase());
            if (directType != null) {
                return directType;
            }
            
            // Try with underscores replaced by spaces (legacy compatibility)
            String spaceVersion = effectName.replace("_", " ");
            PotionEffectType spaceType = PotionEffectType.getByName(spaceVersion.toUpperCase());
            if (spaceType != null) {
                return spaceType;
            }
            
            // Try common aliases and legacy names
            String normalized = effectName.toUpperCase().trim();
            switch (normalized) {
                case "DAMAGE_RESISTANCE", "RESISTANCE" -> {
                    PotionEffectType resistanceType = PotionEffectType.getByName("DAMAGE_RESISTANCE");
                    return resistanceType != null ? resistanceType : PotionEffectType.getByName("RESISTANCE");
                }
                case "JUMP_BOOST", "JUMP" -> {
                    PotionEffectType jumpType = PotionEffectType.getByName("JUMP_BOOST");
                    return jumpType != null ? jumpType : PotionEffectType.getByName("JUMP");
                }
                case "REGENERATION", "REGEN" -> {
                    return PotionEffectType.getByName("REGENERATION");
                }
                case "FAST_DIGGING", "HASTE" -> {
                    PotionEffectType hasteType = PotionEffectType.getByName("HASTE");
                    return hasteType != null ? hasteType : PotionEffectType.getByName("FAST_DIGGING");
                }
                case "SLOW_DIGGING", "MINING_FATIGUE" -> {
                    PotionEffectType fatigueType = PotionEffectType.getByName("MINING_FATIGUE");
                    return fatigueType != null ? fatigueType : PotionEffectType.getByName("SLOW_DIGGING");
                }
                case "CONFUSION", "NAUSEA" -> {
                    PotionEffectType nauseaType = PotionEffectType.getByName("NAUSEA");
                    return nauseaType != null ? nauseaType : PotionEffectType.getByName("CONFUSION");
                }
                default -> {
                    // Try fallback mappings for known effects
                    return EFFECT_TYPE_MAPPING.get(normalized);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error looking up effect type '" + effectName + "': " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Retrieves enhanced display name for upgrades with professional formatting.
     */
    public static String getDisplayName(String upgradeId) {
        try {
            if (upgradeId == null || upgradeId.trim().isEmpty()) {
                return "Unknown Upgrade";
            }
            
            // Get configuration path for the upgrade
            ConfigurationSection upgradeSection = upgradesCfg.getConfigurationSection("upgrades." + upgradeId);
            if (upgradeSection == null) {
                return beautifyUpgradeId(upgradeId);
            }
            
            // Try to get custom display name first
            String customName = upgradeSection.getString("display-name");
            if (customName != null && !customName.trim().isEmpty()) {
                return ChatColor.translateAlternateColorCodes('&', customName);
            }
            
            // Generate name from effect and tier
            String effect = upgradeSection.getString("effect", upgradeId);
            int tier = upgradeSection.getInt("tier", 1);
            
            return generateDisplayName(effect, tier);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting display name for " + upgradeId + ": " + e.getMessage());
            return beautifyUpgradeId(upgradeId);
        }
    }

    /**
     * Generates professional display names for effects.
     */
    private static String generateDisplayName(String effect, int tier) {
        String beautifulName = switch (effect.toUpperCase()) {
            case "SPEED" -> "Swift Movement";
            case "REGENERATION" -> "Life Restoration";
            case "JUMP_BOOST" -> "Leap Enhancement";
            case "HASTE" -> "Mining Acceleration";
            case "STRENGTH" -> "Combat Prowess";
            case "DAMAGE_RESISTANCE", "RESISTANCE" -> "Damage Resistance";
            case "POISON" -> "Toxic Immunity";
            case "NIGHT_VISION" -> "Night Vision";
            case "INVISIBILITY" -> "Stealth Mastery";
            case "WATER_BREATHING" -> "Aquatic Adaptation";
            case "FIRE_RESISTANCE" -> "Flame Protection";
            case "SATURATION" -> "Eternal Sustenance";
            case "ABSORPTION" -> "Extra Hearts";
            case "HEALTH_BOOST" -> "Vitality Boost";
            default -> beautifyString(effect);
        };
        
        return beautifulName + " " + getRomanNumeral(tier);
    }

    /**
     * Beautifies raw upgrade IDs for fallback display.
     */
    private static String beautifyUpgradeId(String upgradeId) {
        if (upgradeId == null) return "Unknown";
        
        // Split by underscores and beautify each part
        String[] parts = upgradeId.split("_");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) result.append(" ");
            result.append(beautifyString(parts[i]));
        }
        
        return result.toString();
    }

    /**
     * Beautifies individual strings for display.
     */
    private static String beautifyString(String input) {
        if (input == null || input.isEmpty()) return "";
        
        // Handle Roman numerals
        if (input.matches("^(I|II|III|IV|V)$")) {
            return input; // Keep as-is
        }
        
        // Capitalize first letter, rest lowercase
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    /**
     * Converts numbers to Roman numerals with extended support.
     */
    private static String getRomanNumeral(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(number);
        };
    }

    /**
     * Validates if an effect ID is properly configured.
     */
    public static boolean isValidEffect(String upgradeId) {
        if (upgradeId == null || upgradeId.trim().isEmpty()) {
            return false;
        }
        
        try {
            ConfigurationSection upgradeSection = upgradesCfg.getConfigurationSection("upgrades." + upgradeId);
            if (upgradeSection == null) {
                return false;
            }
            
            String effectName = upgradeSection.getString("effect");
            return effectName != null && getEffectType(effectName) != null;
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets all available effect types for validation.
     */
    public static Set<String> getAvailableEffectTypes() {
        return new HashSet<>(EFFECT_TYPE_MAPPING.keySet());
    }

    /**
     * Creates a comprehensive effect information string for display.
     */
    public static String getEffectInfo(String upgradeId) {
        try {
            ConfigurationSection upgradeSection = upgradesCfg.getConfigurationSection("upgrades." + upgradeId);
            if (upgradeSection == null) {
                return "Unknown effect";
            }
            
            String effect = upgradeSection.getString("effect", "UNKNOWN");
            int amplifier = upgradeSection.getInt("amplifier", 0);
            int duration = upgradeSection.getInt("duration", 200);
            int tier = upgradeSection.getInt("tier", 1);
            
            return String.format("%s (Level %d) - %ds duration - Tier %s",
                generateDisplayName(effect, tier),
                amplifier + 1,
                duration / 20,
                getRomanNumeral(tier));
                
        } catch (Exception e) {
            return "Error loading effect info";
        }
    }

    /**
     * Removes all crystal-related effects from a player.
     */
    public static void removeAllCrystalEffects(Player player) {
        if (player == null) return;
        
        try {
            // Get all upgrade IDs that the player has unlocked
            Set<String> upgrades = PlayerDataManager.getUnlockedUpgrades(player.getUniqueId());
            
            for (String upgradeId : upgrades) {
                ConfigurationSection upgradeSection = upgradesCfg.getConfigurationSection("upgrades." + upgradeId);
                if (upgradeSection != null) {
                    String effectName = upgradeSection.getString("effect");
                    if (effectName != null) {
                        PotionEffectType effectType = getEffectType(effectName);
                        if (effectType != null && player.hasPotionEffect(effectType)) {
                            player.removePotionEffect(effectType);
                        }
                    }
                }
            }
            
            plugin.getLogger().fine("Removed all crystal effects from " + player.getName());
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error removing crystal effects from " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Data class for dynamic effect calculations.
     */
    private static class DynamicEffectData {
        final int amplifier;
        final int duration;
        
        DynamicEffectData(int amplifier, int duration) {
            this.amplifier = amplifier;
            this.duration = duration;
        }
    }
}
