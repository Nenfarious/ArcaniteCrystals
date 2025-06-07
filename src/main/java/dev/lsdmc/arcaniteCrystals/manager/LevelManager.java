package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LevelManager {

    private static final FileConfiguration levelsCfg   = ConfigManager.getLevelsConfig();
    private static final FileConfiguration upgradesCfg = ConfigManager.getUpgradesConfig();
    private static final Logger logger = ArcaniteCrystals.getInstance().getLogger();

    /** Checks whether the specified level exists in levels.yml. */
    public static boolean isValidLevel(int level) {
        return levelsCfg.isConfigurationSection("level-" + level);
    }

    /** Loads the configuration for the given level (requirements, tier, slots, buffs). */
    public static LevelConfig getConfigForLevel(int level) {
        ConfigurationSection sec = levelsCfg.getConfigurationSection("level-" + level);
        if (sec == null) return null;

        int moneyReq = sec.getInt("requirements.money", 0);
        int killsReq = sec.getInt("requirements.kills", 0);
        long hours = sec.getLong("requirements.time", 0L);
        long timeReqMs = hours * 3_600_000L;

        int tier = sec.getInt("tier", 1);
        int slots = sec.getInt("crystal-slots", 1);

        Map<String, Double> buffs = new HashMap<>();
        ConfigurationSection buffsSec = sec.getConfigurationSection("buffs");
        if (buffsSec != null) {
            for (String stat : buffsSec.getKeys(false)) {
                buffs.put(stat, buffsSec.getDouble(stat, 0.0));
            }
        }

        return new LevelConfig(level, moneyReq, killsReq, timeReqMs, tier, slots, buffs);
    }

    /** Returns the player's current level (from Redis). */
    public static int getPlayerLevel(UUID playerId) {
        return PlayerDataManager.getLevel(playerId);
    }

    /** Returns the maximum effect tier allowed by the player's level. */
    public static int getMaxTier(UUID playerId) {
        LevelConfig cfg = getConfigForLevel(getPlayerLevel(playerId));
        return (cfg != null) ? cfg.getTier() : 1;
    }

    /** Returns the number of crystal slots for the player's level. */
    public static int getSlots(UUID playerId) {
        LevelConfig cfg = getConfigForLevel(getPlayerLevel(playerId));
        return (cfg != null) ? cfg.getSlots() : 1;
    }

    /** Returns the set of upgrade IDs the player has unlocked. */
    public static Set<String> getAllowedUpgrades(UUID playerId) {
        return PlayerDataManager.getUnlockedUpgrades(playerId);
    }

    /**
     * Apply permanent attribute buffs to the player based on their level.
     * Uses robust modifier management to prevent duplicate modifier errors.
     */
    public static void applyBuffs(Player player, int level) {
        try {
            // Remove ALL existing Arcanite modifiers first with comprehensive cleanup
            removeAllArcaniteModifiers(player);
            
            LevelConfig cfg = getConfigForLevel(level);
            if (cfg == null) return;

            // Apply each buff with unique, session-based UUIDs
            for (Map.Entry<String, Double> entry : cfg.getBuffs().entrySet()) {
                String stat = entry.getKey();
                double value = entry.getValue();

                try {
                    applyStatModifier(player, stat, value, level);
                } catch (Exception e) {
                    // Log individual stat errors but continue processing other stats
                    logger.warning("Failed to apply " + stat + " buff to " + player.getName() + 
                                 ": " + e.getMessage());
                }
            }
            
            logger.fine("Applied level " + level + " buffs to " + player.getName());
            
        } catch (Exception e) {
            logger.severe("Critical error applying buffs to " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Removes all Arcanite modifiers from all attributes with comprehensive cleanup.
     */
    private static void removeAllArcaniteModifiers(Player player) {
        List<Attribute> attributesToCheck = Arrays.asList(
            Attribute.MAX_HEALTH,
            Attribute.MOVEMENT_SPEED,
            Attribute.ATTACK_DAMAGE,
            Attribute.KNOCKBACK_RESISTANCE
        );
        
        for (Attribute attribute : attributesToCheck) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;

            try {
                // Create a copy of modifiers to avoid concurrent modification
                List<AttributeModifier> toRemove = new ArrayList<>();
                for (AttributeModifier modifier : instance.getModifiers()) {
                    if (modifier.getName().startsWith("arcanite_")) {
                        toRemove.add(modifier);
                    }
                }

                // Remove all found modifiers
                for (AttributeModifier modifier : toRemove) {
                    try {
                        instance.removeModifier(modifier);
                    } catch (Exception e) {
                        // Log but continue - modifier might already be removed
                        logger.fine("Could not remove modifier " + modifier.getName() + 
                                  " from " + player.getName() + ": " + e.getMessage());
                    }
                }
                
            } catch (Exception e) {
                logger.warning("Error cleaning modifiers for attribute " + attribute.name() + 
                             " on player " + player.getName() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Applies a single stat modifier with proper error handling and unique UUIDs.
     */
    private static void applyStatModifier(Player player, String stat, double value, int level) {
        Attribute attribute;
        AttributeModifier.Operation operation;
        
        switch (stat.toLowerCase(Locale.ROOT)) {
            case "max_health":
                attribute = Attribute.MAX_HEALTH;
                operation = AttributeModifier.Operation.ADD_NUMBER;
                break;
            case "walk_speed":
                attribute = Attribute.MOVEMENT_SPEED;
                operation = AttributeModifier.Operation.ADD_SCALAR;
                break;
            case "attack_damage":
                attribute = Attribute.ATTACK_DAMAGE;
                operation = AttributeModifier.Operation.ADD_SCALAR;
                break;
            case "knockback_resistance":
                attribute = Attribute.KNOCKBACK_RESISTANCE;
                operation = AttributeModifier.Operation.ADD_SCALAR;
                break;
            default:
                player.sendMessage(
                        MessageManager.get("error.invalidStat")
                                .replace("{stat}", stat)
                );
                return;
        }

        AttributeInstance attrInstance = player.getAttribute(attribute);
        if (attrInstance == null) {
            logger.warning("Could not get attribute instance for " + attribute.name());
            return;
        }

        // Create unique modifier with session-based UUID to prevent conflicts
        String modifierName = "arcanite_" + stat + "_lvl" + level;
        // Use player UUID + stat + timestamp for guaranteed uniqueness
        UUID modifierUUID = UUID.nameUUIDFromBytes(
            (player.getUniqueId().toString() + "_" + stat + "_" + System.currentTimeMillis()).getBytes()
        );
        
        AttributeModifier modifier = new AttributeModifier(
                modifierUUID,
                modifierName,
                value,
                operation
        );

        // Double-check that this exact modifier doesn't already exist
        boolean alreadyExists = attrInstance.getModifiers().stream()
                .anyMatch(existing -> existing.getName().equals(modifierName));
        
        if (!alreadyExists) {
            attrInstance.addModifier(modifier);
            logger.fine("Added modifier " + modifierName + " to " + player.getName());
        } else {
            logger.warning("Modifier " + modifierName + " already exists for " + player.getName());
        }
    }

    /**
     * Determines the tier number for a given upgrade ID by reading from upgrades.yml.
     */
    public static int getTier(String upgradeId) {
        ConfigurationSection upgradesSection = upgradesCfg.getConfigurationSection("upgrades");
        if (upgradesSection == null) return 1;
        
        ConfigurationSection upgradeSection = upgradesSection.getConfigurationSection(upgradeId);
        if (upgradeSection != null) {
            return upgradeSection.getInt("tier", 1);
        }
        
        // Fallback: parse tier from the upgrade ID suffix (e.g., "speed_II" -> tier 2)
        String[] parts = upgradeId.split("_");
        if (parts.length > 1) {
            String tierStr = parts[parts.length - 1];
            switch (tierStr) {
                case "I": return 1;
                case "II": return 2;
                case "III": return 3;
                default: return 1;
            }
        }
        return 1;
    }

    /**
     * Gets all available upgrades for a given tier from upgrades.yml.
     */
    public static Set<String> getUpgradesForTier(int tier) {
        Set<String> upgrades = new HashSet<>();
        ConfigurationSection upgradesSection = upgradesCfg.getConfigurationSection("upgrades");
        if (upgradesSection == null) return upgrades;
        
        for (String upgradeId : upgradesSection.getKeys(false)) {
            ConfigurationSection upgradeSection = upgradesSection.getConfigurationSection(upgradeId);
            if (upgradeSection != null && upgradeSection.getInt("tier", 1) == tier) {
                upgrades.add(upgradeId);
            }
        }
        return upgrades;
    }

    /**
     * Gets all available upgrades up to the specified maximum tier.
     */
    public static Set<String> getUpgradesUpToTier(int maxTier) {
        Set<String> upgrades = new HashSet<>();
        for (int tier = 1; tier <= maxTier; tier++) {
            upgrades.addAll(getUpgradesForTier(tier));
        }
        return upgrades;
    }

    /** Data holder for a level's settings. */
    public static class LevelConfig {
        private final int level, moneyRequirement, killsRequirement;
        private final long timeRequirementMs;
        private final int tier, slots;
        private final Map<String, Double> buffs;

        public LevelConfig(int level,
                           int moneyReq,
                           int killsReq,
                           long timeReqMs,
                           int tier,
                           int slots,
                           Map<String, Double> buffs) {
            this.level            = level;
            this.moneyRequirement = moneyReq;
            this.killsRequirement = killsReq;
            this.timeRequirementMs= timeReqMs;
            this.tier             = tier;
            this.slots            = slots;
            this.buffs            = Collections.unmodifiableMap(buffs);
        }

        public int getLevel()             { return level; }
        public int getMoneyRequirement()  { return moneyRequirement; }
        public int getKillsRequirement()  { return killsRequirement; }
        public long getTimeRequirementMs(){ return timeRequirementMs; }
        public int getTier()              { return tier; }
        public int getSlots()             { return slots; }
        public Map<String, Double> getBuffs() { return buffs; }
    }
}
