// src/main/java/dev/lsdmc/arcaniteCrystals/placeholder/ArcaniteExpansion.java
package dev.lsdmc.arcaniteCrystals.placeholder;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager;
import dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager;
import dev.lsdmc.arcaniteCrystals.util.RequirementChecker;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Comprehensive PlaceholderAPI integration for the server-wide leveling system.
 * Provides extensive placeholders that other plugins can easily use for displays,
 * scoreboards, chat formatting, and more.
 */
public class ArcaniteExpansion extends PlaceholderExpansion {

    private final ArcaniteCrystals plugin = ArcaniteCrystals.getInstance();

    @Override
    public @NotNull String getIdentifier() {
        return "arcanite";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";
        
        UUID playerId = player.getUniqueId();
        String[] parts = identifier.toLowerCase().split("_");
        
        // ===== LEVEL SYSTEM PLACEHOLDERS =====
        
        // Basic level information
        switch (identifier.toLowerCase()) {
            case "level":
                return String.valueOf(ServerLevelManager.getPlayerLevel(playerId));
                
            case "level_roman":
                return convertToRoman(ServerLevelManager.getPlayerLevel(playerId));
                
            case "level_tag":
                int level = ServerLevelManager.getPlayerLevel(playerId);
                ServerLevelManager.LevelConfiguration config = ServerLevelManager.getLevelConfiguration(level);
                if (config != null) {
                    return ChatColor.translateAlternateColorCodes('&', config.getTag());
                }
                return "&7[" + level + "]";
                
            case "level_display_name":
                int currentLevel = ServerLevelManager.getPlayerLevel(playerId);
                ServerLevelManager.LevelConfiguration levelConfig = ServerLevelManager.getLevelConfiguration(currentLevel);
                return levelConfig != null ? levelConfig.getDisplayName() : "Level " + currentLevel + " Player";
                
            case "level_description":
                int playerLevel = ServerLevelManager.getPlayerLevel(playerId);
                ServerLevelManager.LevelConfiguration playerConfig = ServerLevelManager.getLevelConfiguration(playerLevel);
                return playerConfig != null ? playerConfig.getDescription() : "";
                
            case "level_max":
                return String.valueOf(ServerLevelManager.getMaxLevel());
                
            case "level_progress_bar":
                return generateLevelProgressBar(player);
                
            case "level_progress_percent":
                return String.valueOf(calculateLevelProgressPercent(player));
                
            // Level requirements and progress
            case "can_levelup":
                return ServerLevelManager.canPlayerLevelUp(playerId) ? "true" : "false";
                
            case "next_level":
                int nextLvl = ServerLevelManager.getPlayerLevel(playerId) + 1;
                return nextLvl <= ServerLevelManager.getMaxLevel() ? String.valueOf(nextLvl) : "MAX";
                
            case "next_level_tag":
                int nextLevel = ServerLevelManager.getPlayerLevel(playerId) + 1;
                ServerLevelManager.LevelConfiguration nextConfig = ServerLevelManager.getLevelConfiguration(nextLevel);
                if (nextConfig != null) {
                    return ChatColor.translateAlternateColorCodes('&', nextConfig.getTag());
                }
                return "MAX LEVEL";
                
            case "requirements_missing_count":
                List<String> missing = ServerLevelManager.getMissingRequirementsForNextLevel(playerId);
                return String.valueOf(missing.size());
                
            case "requirements_missing":
                List<String> missingReqs = ServerLevelManager.getMissingRequirementsForNextLevel(playerId);
                return missingReqs.isEmpty() ? "None" : String.join(", ", missingReqs);
                
            // Crystal system placeholders (ArcaniteCrystals specific)
            case "crystal_tier_max":
                return String.valueOf(ServerLevelManager.getMaxTier(playerId));
                
            case "crystal_slots":
                return String.valueOf(ServerLevelManager.getSlots(playerId));
                
            case "crystal_cooldown":
                long now = System.currentTimeMillis();
                long last = PlayerDataManager.getCooldown(playerId);
                long cdMs = plugin.getConfig().getLong("crystal.cooldown", 300) * 1000;
                long secs = Math.max(0, (last + cdMs - now) / 1000);
                return String.valueOf(secs);
                
            case "crystal_status":
                ItemStack crystal = player.getInventory().getItemInOffHand();
                if (CrystalManager.isActivatedCrystal(crystal)) {
                    return "Active";
                } else if (CrystalManager.isDepletedCrystal(crystal)) {
                    return "Depleted";
                } else if (CrystalManager.isCrystal(crystal)) {
                    return "Inactive";
                }
                return "None";
                
            case "crystal_energy_percent":
                ItemStack offHand = player.getInventory().getItemInOffHand();
                if (CrystalManager.isActivatedCrystal(offHand)) {
                    int current = CrystalManager.getEnergy(offHand);
                    int max = CrystalManager.getMaxEnergy(offHand);
                    return String.valueOf((int) ((double) current / max * 100));
                }
                return "0";
                
            case "crystal_energy_current":
                ItemStack currentCrystal = player.getInventory().getItemInOffHand();
                if (CrystalManager.isCrystal(currentCrystal)) {
                    return String.valueOf(CrystalManager.getEnergy(currentCrystal));
                }
                return "0";
                
            case "crystal_energy_max":
                ItemStack maxCrystal = player.getInventory().getItemInOffHand();
                if (CrystalManager.isCrystal(maxCrystal)) {
                    return String.valueOf(CrystalManager.getMaxEnergy(maxCrystal));
                }
                return "0";
                
            case "crystal_effects_count":
                ItemStack effectsCrystal = player.getInventory().getItemInOffHand();
                if (CrystalManager.isCrystal(effectsCrystal)) {
                    return String.valueOf(CrystalManager.getCrystalEffects(effectsCrystal).size());
                }
                return "0";
                
            // Upgrade system placeholders
            case "upgrades_unlocked":
                Set<String> upgrades = PlayerDataManager.getUnlockedUpgrades(playerId);
                return String.valueOf(upgrades.size());
                
            case "upgrades_available":
                Set<String> availableUpgrades = ServerLevelManager.getUpgradesUpToTier(ServerLevelManager.getMaxTier(playerId));
                return String.valueOf(availableUpgrades.size());
                
            case "upgrades_progress_percent":
                Set<String> unlockedUpgrades = PlayerDataManager.getUnlockedUpgrades(playerId);
                Set<String> allAvailable = ServerLevelManager.getUpgradesUpToTier(ServerLevelManager.getMaxTier(playerId));
                if (allAvailable.isEmpty()) return "100";
                return String.valueOf((int) ((double) unlockedUpgrades.size() / allAvailable.size() * 100));
        }
        
        // ===== DYNAMIC PLACEHOLDERS WITH PARAMETERS =====
        
        // Level information for specific levels: %arcanite_level_tag_5%
        if (parts.length >= 3 && parts[0].equals("level") && parts[1].equals("tag")) {
            try {
                int targetLevel = Integer.parseInt(parts[2]);
                ServerLevelManager.LevelConfiguration targetConfig = ServerLevelManager.getLevelConfiguration(targetLevel);
                if (targetConfig != null) {
                    return ChatColor.translateAlternateColorCodes('&', targetConfig.getTag());
                }
            } catch (NumberFormatException ignored) {}
            return "";
        }
        
        // Level display name for specific levels: %arcanite_level_name_5%
        if (parts.length >= 3 && parts[0].equals("level") && parts[1].equals("name")) {
            try {
                int targetLevel = Integer.parseInt(parts[2]);
                ServerLevelManager.LevelConfiguration targetConfig = ServerLevelManager.getLevelConfiguration(targetLevel);
                if (targetConfig != null) {
                    return targetConfig.getDisplayName();
                }
            } catch (NumberFormatException ignored) {}
            return "";
        }
        
        // Level description for specific levels: %arcanite_level_desc_5%
        if (parts.length >= 3 && parts[0].equals("level") && parts[1].equals("desc")) {
            try {
                int targetLevel = Integer.parseInt(parts[2]);
                ServerLevelManager.LevelConfiguration targetConfig = ServerLevelManager.getLevelConfiguration(targetLevel);
                if (targetConfig != null) {
                    return targetConfig.getDescription();
                }
            } catch (NumberFormatException ignored) {}
            return "";
        }
        
        // Buff values: %arcanite_buff_max_health%
        if (parts.length >= 2 && parts[0].equals("buff")) {
            String buffName = identifier.substring(5); // Remove "buff_"
            return getPlayerBuffValue(player, buffName);
        }
        
        // Player statistics: %arcanite_stat_playtime_hours%
        if (parts.length >= 2 && parts[0].equals("stat")) {
            return getPlayerStatistic(player, identifier.substring(5));
        }
        
        // Requirement checking: %arcanite_req_money_next%, %arcanite_req_kills_next%
        if (parts.length >= 3 && parts[0].equals("req") && parts[2].equals("next")) {
            return getNextLevelRequirement(player, parts[1]);
        }
        
        return null; // Placeholder not found
    }
    
    /**
     * Generates a visual progress bar for level progression
     */
    private String generateLevelProgressBar(Player player) {
        try {
            int currentLevel = ServerLevelManager.getPlayerLevel(player.getUniqueId());
            if (currentLevel >= ServerLevelManager.getMaxLevel()) {
                return "&a▓▓▓▓▓▓▓▓▓▓ &6MAX LEVEL";
            }
            
            // For simplicity, this is a basic implementation
            // You could make this more sophisticated based on actual requirements
            int progress = calculateLevelProgressPercent(player);
            int filled = progress / 10; // 10 bars total
            
            StringBuilder bar = new StringBuilder("&a");
            for (int i = 0; i < 10; i++) {
                if (i < filled) {
                    bar.append("▓");
                } else {
                    bar.append("&7░");
                }
            }
            bar.append(" &e").append(progress).append("%");
            
            return ChatColor.translateAlternateColorCodes('&', bar.toString());
        } catch (Exception e) {
            return "&7Progress Unknown";
        }
    }
    
    /**
     * Calculates level progress percentage
     */
    private int calculateLevelProgressPercent(Player player) {
        try {
            // This is a simplified calculation
            // You might want to implement this based on actual requirement completion
            List<String> missing = ServerLevelManager.getMissingRequirementsForNextLevel(player.getUniqueId());
            if (missing.isEmpty()) {
                return 100;
            } else if (missing.size() == 1) {
                return 75;
            } else if (missing.size() == 2) {
                return 50;
            } else {
                return 25;
            }
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Gets a player's current buff value
     */
    private String getPlayerBuffValue(Player player, String buffName) {
        try {
            ServerLevelManager.LevelData levelData = ServerLevelManager.getPlayerLevelData(player.getUniqueId());
            Double buffValue = levelData.getAppliedBuffs().get(buffName);
            if (buffValue != null) {
                return buffValue % 1 == 0 ? String.valueOf(buffValue.intValue()) : String.format("%.2f", buffValue);
            }
            
            // Fallback to current level config
            int level = ServerLevelManager.getPlayerLevel(player.getUniqueId());
            ServerLevelManager.LevelConfiguration config = ServerLevelManager.getLevelConfiguration(level);
            if (config != null) {
                Double configBuff = config.getBuffs().get(buffName);
                return configBuff != null ? (configBuff % 1 == 0 ? String.valueOf(configBuff.intValue()) : String.format("%.2f", configBuff)) : "0";
            }
            
            return "0";
        } catch (Exception e) {
            return "0";
        }
    }
    
    /**
     * Gets player statistics
     */
    private String getPlayerStatistic(Player player, String statType) {
        try {
            switch (statType.toLowerCase()) {
                case "playtime_hours":
                    return String.valueOf(player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) * 50L / 3_600_000L);
                case "playtime_minutes":
                    return String.valueOf(player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) * 50L / 60_000L);
                case "player_kills":
                    return String.valueOf(player.getStatistic(org.bukkit.Statistic.PLAYER_KILLS));
                case "mob_kills":
                    return String.valueOf(player.getStatistic(org.bukkit.Statistic.MOB_KILLS));
                case "deaths":
                    return String.valueOf(player.getStatistic(org.bukkit.Statistic.DEATHS));
                case "distance_walked":
                    return String.valueOf(player.getStatistic(org.bukkit.Statistic.WALK_ONE_CM));
                case "blocks_broken":
                    return String.valueOf(player.getStatistic(org.bukkit.Statistic.MINE_BLOCK));
                case "blocks_placed":
                    return String.valueOf(player.getStatistic(org.bukkit.Statistic.USE_ITEM));
                default:
                    return "0";
            }
        } catch (Exception e) {
            return "0";
        }
    }
    
    /**
     * Gets requirement for next level
     */
    private String getNextLevelRequirement(Player player, String reqType) {
        try {
            int nextLevel = ServerLevelManager.getPlayerLevel(player.getUniqueId()) + 1;
            ServerLevelManager.LevelConfiguration config = ServerLevelManager.getLevelConfiguration(nextLevel);
            if (config == null) return "MAX LEVEL";
            
            ServerLevelManager.RequirementSet requirements = config.getRequirements();
            
            switch (reqType.toLowerCase()) {
                case "money":
                    return requirements.getMoney() > 0 ? String.format("%.2f", requirements.getMoney()) : "0";
                case "kills":
                    return String.valueOf(requirements.getPlayerKills());
                case "mob_kills":
                    return String.valueOf(requirements.getMobKills());
                case "time":
                case "playtime":
                    return String.valueOf(requirements.getPlaytimeHours());
                case "experience":
                case "exp":
                    return String.valueOf(requirements.getExperience());
                default:
                    return "0";
            }
        } catch (Exception e) {
            return "0";
        }
    }
    
    /**
     * Converts numbers to Roman numerals with extended support
     */
    private String convertToRoman(int number) {
        if (number <= 0) return "0";
        if (number > 50) return String.valueOf(number);
        
        String[] romanNumerals = {
            "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
            "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX",
            "XXI", "XXII", "XXIII", "XXIV", "XXV", "XXVI", "XXVII", "XXVIII", "XXIX", "XXX",
            "XXXI", "XXXII", "XXXIII", "XXXIV", "XXXV", "XXXVI", "XXXVII", "XXXVIII", "XXXIX", "XL",
            "XLI", "XLII", "XLIII", "XLIV", "XLV", "XLVI", "XLVII", "XLVIII", "XLIX", "L"
        };
        
        return romanNumerals[number];
    }
}
