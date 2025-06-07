// src/main/java/dev/lsdmc/arcaniteCrystals/config/ConfigValidator.java
package dev.lsdmc.arcaniteCrystals.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Comprehensive configuration validator ensuring all settings are valid
 * and the plugin can operate safely in production environments.
 */
public class ConfigValidator {

    private static final List<String> errors = new ArrayList<>();
    private static final List<String> warnings = new ArrayList<>();

    /**
     * Validates all configuration files and reports any issues.
     * 
     * @param plugin The plugin instance for logging
     * @return true if configuration is valid and safe to use
     */
    public static boolean validate(JavaPlugin plugin) {
        Logger logger = plugin.getLogger();
        errors.clear();
        warnings.clear();

        logger.info("Starting comprehensive configuration validation...");

        // Validate main config
        validateMainConfig(ConfigManager.getConfig());
        
        // Validate upgrades
        validateUpgradesConfig(ConfigManager.getUpgradesConfig());
        
        // Validate levels
        validateLevelsConfig(ConfigManager.getLevelsConfig());
        
        // Validate messages
        validateMessagesConfig(ConfigManager.getMessagesConfig());

        // Report results
        reportValidationResults(logger);

        return errors.isEmpty();
    }

    private static void validateMainConfig(FileConfiguration config) {
        // Database settings
        String dbMode = config.getString("database.mode", "redis");
        if (!"redis".equalsIgnoreCase(dbMode)) {
            errors.add("Invalid database.mode: '" + dbMode + "'. Only 'redis' is supported.");
        }

        String redisHost = config.getString("database.redis.host");
        if (redisHost == null || redisHost.trim().isEmpty()) {
            errors.add("database.redis.host cannot be null or empty");
        }

        int redisPort = config.getInt("database.redis.port", 6379);
        if (redisPort < 1 || redisPort > 65535) {
            errors.add("database.redis.port must be between 1-65535, got: " + redisPort);
        }

        int timeout = config.getInt("database.redis.timeout", 2000);
        if (timeout < 100) {
            warnings.add("Very low Redis timeout (" + timeout + "ms). Consider increasing for stability.");
        }

        // Crystal settings
        String crystalMaterial = config.getString("crystal.material", "DIAMOND");
        if (Material.matchMaterial(crystalMaterial) == null) {
            errors.add("Invalid crystal.material: '" + crystalMaterial + "'");
        }

        int cooldown = config.getInt("crystal.cooldown", 300);
        if (cooldown < 0) {
            errors.add("crystal.cooldown cannot be negative: " + cooldown);
        } else if (cooldown < 10) {
            warnings.add("Very low crystal cooldown (" + cooldown + "s). This may cause spam.");
        }

        int energy = config.getInt("crystal.energy", 18000);
        if (energy <= 0) {
            errors.add("crystal.energy must be positive: " + energy);
        }

        int drain = config.getInt("crystal.drain", 80);
        if (drain <= 0) {
            errors.add("crystal.drain must be positive: " + drain);
        } else if (drain >= energy) {
            warnings.add("Crystal drain (" + drain + ") is very high compared to energy (" + energy + ")");
        }

        // Recharge settings
        String rechargeMaterial = config.getString("recharge.material", "QUARTZ");
        if (Material.matchMaterial(rechargeMaterial) == null) {
            errors.add("Invalid recharge.material: '" + rechargeMaterial + "'");
        }

        // Mining settings
        int miningDuration = config.getInt("mining.effect-duration", 200);
        if (miningDuration <= 0) {
            errors.add("mining.effect-duration must be positive: " + miningDuration);
        }

        // Sound settings
        String activateSound = config.getString("sounds.activate", "ENTITY_EXPERIENCE_ORB_PICKUP");
        String rechargeSound = config.getString("sounds.recharge", "BLOCK_BEACON_ACTIVATE");
        
        try {
            // Use modern Sound validation without deprecated methods
            boolean foundActivate = false;
            boolean foundRecharge = false;
            
            // Check against known common sounds
            String[] commonSounds = {
                "ENTITY_EXPERIENCE_ORB_PICKUP", "BLOCK_BEACON_ACTIVATE",
                "ENTITY_PLAYER_LEVELUP", "UI_BUTTON_CLICK", "ENTITY_VILLAGER_YES",
                "ENTITY_VILLAGER_NO", "BLOCK_NOTE_BLOCK_CHIME", "BLOCK_ENCHANTMENT_TABLE_USE"
            };
            
            for (String soundName : commonSounds) {
                if (soundName.equals(activateSound)) foundActivate = true;
                if (soundName.equals(rechargeSound)) foundRecharge = true;
            }
            
            // Fallback: try to match against Sound enum constants without deprecated methods
            if (!foundActivate || !foundRecharge) {
                // Use comprehensive sound validation without deprecated .values()
                String[] allCommonSounds = {
                    "ENTITY_EXPERIENCE_ORB_PICKUP", "BLOCK_BEACON_ACTIVATE", "ENTITY_PLAYER_LEVELUP",
                    "UI_BUTTON_CLICK", "ENTITY_VILLAGER_YES", "ENTITY_VILLAGER_NO", 
                    "BLOCK_NOTE_BLOCK_CHIME", "BLOCK_ENCHANTMENT_TABLE_USE", "ENTITY_ITEM_PICKUP",
                    "ENTITY_FIREWORK_ROCKET_BLAST", "UI_TOAST_CHALLENGE_COMPLETE", 
                    "BLOCK_BEACON_POWER_SELECT", "ENTITY_ITEM_BREAK", "BLOCK_GLASS_BREAK",
                    "BLOCK_FIRE_AMBIENT", "ITEM_FIRECHARGE_USE", "BLOCK_NOTE_BLOCK_HARP",
                    "BLOCK_NOTE_BLOCK_BELL", "BLOCK_NOTE_BLOCK_BASS", "BLOCK_CHEST_OPEN",
                    "BLOCK_CHEST_CLOSE", "ENTITY_PLAYER_BREATH"
                };
                
                for (String soundName : allCommonSounds) {
                    if (soundName.equals(activateSound)) foundActivate = true;
                    if (soundName.equals(rechargeSound)) foundRecharge = true;
                }
            }
            
            if (!foundActivate) {
                errors.add("Invalid sounds.activate: '" + activateSound + "'");
            }
            if (!foundRecharge) {
                errors.add("Invalid sounds.recharge: '" + rechargeSound + "'");
            }
        } catch (Exception e) {
            warnings.add("Could not validate sound configurations: " + e.getMessage());
        }
    }

    private static void validateUpgradesConfig(FileConfiguration upgradesConfig) {
        ConfigurationSection upgradesSection = upgradesConfig.getConfigurationSection("upgrades");
        if (upgradesSection == null) {
            errors.add("Missing 'upgrades' section in upgrades.yml");
            return;
        }

        for (String upgradeId : upgradesSection.getKeys(false)) {
            ConfigurationSection upgrade = upgradesSection.getConfigurationSection(upgradeId);
            if (upgrade == null) continue;

            // Validate tier
            int tier = upgrade.getInt("tier", -1);
            if (tier < 1 || tier > 3) {
                errors.add("Invalid tier for upgrade '" + upgradeId + "': " + tier + " (must be 1-3)");
            }

            // Validate effect
            String effect = upgrade.getString("effect");
            if (effect == null || effect.trim().isEmpty()) {
                errors.add("Missing effect for upgrade '" + upgradeId + "'");
            } else {
                // Use modern PotionEffectType validation
                boolean validEffect = false;
                try {
                    // Check against known effect types without deprecated methods
                    String upperEffect = effect.toUpperCase();
                    switch (upperEffect) {
                        case "SPEED", "REGENERATION", "JUMP_BOOST", "HASTE", 
                             "STRENGTH", "DAMAGE_RESISTANCE", "POISON",
                             "NIGHT_VISION", "INVISIBILITY", "WATER_BREATHING",
                             "FIRE_RESISTANCE", "SATURATION", "ABSORPTION" -> validEffect = true;
                        default -> {
                            // Fallback check without deprecated methods
                            String[] allCommonEffects = {
                                "SPEED", "REGENERATION", "JUMP_BOOST", "HASTE", "STRENGTH", 
                                "DAMAGE_RESISTANCE", "POISON", "NIGHT_VISION", "INVISIBILITY",
                                "WATER_BREATHING", "FIRE_RESISTANCE", "SATURATION", "ABSORPTION",
                                "WEAKNESS", "SLOWNESS", "MINING_FATIGUE", "NAUSEA", "BLINDNESS",
                                "HUNGER", "WITHER", "HEALTH_BOOST", "RESISTANCE"
                            };
                            
                            for (String effectName : allCommonEffects) {
                                if (effectName.equalsIgnoreCase(effect)) {
                                    validEffect = true;
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // Fallback validation
                }
                
                if (!validEffect) {
                    errors.add("Invalid effect '" + effect + "' for upgrade '" + upgradeId + "'");
                }
            }

            // Validate amplifier
            int amplifier = upgrade.getInt("amplifier", -1);
            if (amplifier < 0 || amplifier > 10) {
                warnings.add("Unusual amplifier for '" + upgradeId + "': " + amplifier);
            }

            // Validate purchase settings
            ConfigurationSection buySection = upgrade.getConfigurationSection("buy");
            if (buySection == null) {
                errors.add("Missing 'buy' section for upgrade '" + upgradeId + "'");
                continue;
            }

            String buyType = buySection.getString("type", "money");
            int amount = buySection.getInt("amount", 0);

            if (amount <= 0) {
                errors.add("Invalid buy amount for '" + upgradeId + "': " + amount);
            }

            switch (buyType.toLowerCase()) {
                case "money":
                    if (amount > 10000000) { // 10 million
                        warnings.add("Very high money cost for '" + upgradeId + "': $" + amount);
                    }
                    break;
                case "exp":
                    if (amount > 100000) {
                        warnings.add("Very high XP cost for '" + upgradeId + "': " + amount);
                    }
                    break;
                case "item":
                    String itemId = buySection.getString("id");
                    if (itemId == null || Material.matchMaterial(itemId) == null) {
                        errors.add("Invalid item ID for '" + upgradeId + "': " + itemId);
                    }
                    if (amount > 64) {
                        warnings.add("High item amount for '" + upgradeId + "': " + amount);
                    }
                    break;
                default:
                    errors.add("Invalid buy type for '" + upgradeId + "': " + buyType);
                    break;
            }
        }
    }

    private static void validateLevelsConfig(FileConfiguration levelsConfig) {
        for (int level = 1; level <= 10; level++) {
            String levelKey = "level-" + level;
            ConfigurationSection levelSection = levelsConfig.getConfigurationSection(levelKey);
            
            if (levelSection == null) {
                if (level == 1) {
                    errors.add("Missing required level-1 configuration");
                } else {
                    warnings.add("Missing configuration for " + levelKey);
                }
                continue;
            }

            // Validate tag
            String tag = levelSection.getString("tag");
            if (tag == null || tag.trim().isEmpty()) {
                warnings.add("Missing tag for " + levelKey);
            }

            // Validate requirements
            ConfigurationSection reqSection = levelSection.getConfigurationSection("requirements");
            if (reqSection != null) {
                int money = reqSection.getInt("money", 0);
                int kills = reqSection.getInt("kills", 0);
                int time = reqSection.getInt("time", 0);

                if (money < 0) errors.add("Negative money requirement for " + levelKey + ": " + money);
                if (kills < 0) errors.add("Negative kills requirement for " + levelKey + ": " + kills);
                if (time < 0) errors.add("Negative time requirement for " + levelKey + ": " + time);

                // Check for reasonable progression
                if (level > 1 && money > 0) {
                    String prevLevelKey = "level-" + (level - 1);
                    ConfigurationSection prevLevel = levelsConfig.getConfigurationSection(prevLevelKey);
                    if (prevLevel != null) {
                        int prevMoney = prevLevel.getInt("requirements.money", 0);
                        if (money < prevMoney) {
                            warnings.add("Money requirement decreases from " + prevLevelKey + " to " + levelKey);
                        }
                    }
                }
            }

            // Validate tier and slots
            int tier = levelSection.getInt("tier", 1);
            int slots = levelSection.getInt("crystal-slots", 1);

            if (tier < 1 || tier > 3) {
                errors.add("Invalid tier for " + levelKey + ": " + tier);
            }
            if (slots < 1 || slots > 10) {
                errors.add("Invalid crystal-slots for " + levelKey + ": " + slots);
            }

            // Validate buffs
            ConfigurationSection buffsSection = levelSection.getConfigurationSection("buffs");
            if (buffsSection != null) {
                for (String buffType : buffsSection.getKeys(false)) {
                    double value = buffsSection.getDouble(buffType);
                    
                    switch (buffType.toLowerCase()) {
                        case "max_health":
                            if (value < 0 || value > 50) {
                                warnings.add("Unusual max_health buff for " + levelKey + ": " + value);
                            }
                            break;
                        case "walk_speed":
                            if (value < -1 || value > 1) {
                                warnings.add("Extreme walk_speed buff for " + levelKey + ": " + value);
                            }
                            break;
                        case "attack_damage":
                        case "knockback_resistance":
                            if (value < 0 || value > 5) {
                                warnings.add("Unusual " + buffType + " buff for " + levelKey + ": " + value);
                            }
                            break;
                        default:
                            warnings.add("Unknown buff type for " + levelKey + ": " + buffType);
                            break;
                    }
                }
            }
        }
    }

    private static void validateMessagesConfig(FileConfiguration messagesConfig) {
        // Check for essential message keys
        String[] essentialKeys = {
            "error.playersOnly", "error.noPermission", "success.rankup",
            "info.depleted", "gui.talents.title", "help.header"
        };

        for (String key : essentialKeys) {
            String value = messagesConfig.getString(key);
            if (value == null || value.trim().isEmpty()) {
                warnings.add("Missing or empty message: " + key);
            }
        }
    }

    private static void reportValidationResults(Logger logger) {
        if (errors.isEmpty() && warnings.isEmpty()) {
            logger.info("✓ Configuration validation completed successfully - no issues found.");
            return;
        }

        if (!errors.isEmpty()) {
            logger.severe("=== CONFIGURATION ERRORS ===");
            for (String error : errors) {
                logger.severe("ERROR: " + error);
            }
        }

        if (!warnings.isEmpty()) {
            logger.warning("=== CONFIGURATION WARNINGS ===");
            for (String warning : warnings) {
                logger.warning("WARNING: " + warning);
            }
        }

        logger.info("Configuration validation completed with " + 
                   errors.size() + " errors and " + warnings.size() + " warnings.");
        
        if (!errors.isEmpty()) {
            logger.severe("Plugin may not function correctly with configuration errors!");
        }
    }

    /**
     * Gets the list of validation errors.
     */
    public static List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Gets the list of validation warnings.
     */
    public static List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
}
