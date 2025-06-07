// src/main/java/dev/lsdmc/arcaniteCrystals/placeholder/ArcaniteExpansion.java
package dev.lsdmc.arcaniteCrystals.placeholder;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import dev.lsdmc.arcaniteCrystals.manager.LevelManager;
import dev.lsdmc.arcaniteCrystals.util.RequirementChecker;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

/**
 * Essential PlaceholderAPI integration for ArcaniteCrystals.
 * Provides the most commonly used placeholders for scoreboards and displays.
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
        
        switch (identifier.toLowerCase()) {
            // Basic Level Information
            case "level":
                return String.valueOf(PlayerDataManager.getLevel(playerId));
                
            case "level_roman":
                return convertToRoman(PlayerDataManager.getLevel(playerId));
                
            case "level_tag":
                int level = PlayerDataManager.getLevel(playerId);
                String tag = ConfigManager.getLevelsConfig().getString("level-" + level + ".tag", "Level " + level);
                return ChatColor.translateAlternateColorCodes('&', tag);
                
            case "max_tier":
                return String.valueOf(LevelManager.getMaxTier(playerId));
                
            case "crystal_slots":
                return String.valueOf(LevelManager.getSlots(playerId));
                
            // Crystal Information
            case "cooldown":
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
                    int max = plugin.getConfig().getInt("crystal.energy", 18000);
                    return String.valueOf((int) ((double) current / max * 100));
                }
                return "0";
                
            // Upgrade Information
            case "upgrades_unlocked":
                Set<String> upgrades = PlayerDataManager.getUnlockedUpgrades(playerId);
                return String.valueOf(upgrades.size());
                
            // Progress Information
            case "can_levelup":
                int currentLevel = PlayerDataManager.getLevel(playerId);
                int nextLevel = currentLevel + 1;
                if (!LevelManager.isValidLevel(nextLevel)) return "false";
                
                var config = LevelManager.getConfigForLevel(nextLevel);
                if (config == null) return "false";
                
                RequirementChecker checker = new RequirementChecker(player, config);
                return checker.getMissing().isEmpty() ? "true" : "false";
                
            default:
                return null;
        }
    }
    
    /**
     * Converts integer to Roman numerals.
     */
    private String convertToRoman(int number) {
        String[] romans = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return number > 0 && number <= romans.length ? romans[number - 1] : String.valueOf(number);
    }
}
