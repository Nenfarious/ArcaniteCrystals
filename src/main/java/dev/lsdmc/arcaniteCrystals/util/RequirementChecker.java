// src/main/java/dev/lsdmc/arcaniteCrystals/util/RequirementChecker.java
package dev.lsdmc.arcaniteCrystals.util;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Checks a player's progress against level requirements using ServerLevelManager.
 */
public class RequirementChecker {

    private final Player player;
    private final Economy econ;

    public RequirementChecker(Player player) {
        this.player = player;
        RegisteredServiceProvider<Economy> rsp = ArcaniteCrystals.getInstance()
                .getServer().getServicesManager()
                .getRegistration(Economy.class);
        this.econ = (rsp != null) ? rsp.getProvider() : null;
    }

    /**
     * Returns a list of human-readable reasons why the player cannot level up to the next level.
     */
    public List<String> getMissingForNextLevel() {
        UUID playerId = player.getUniqueId();
        return ServerLevelManager.getMissingRequirementsForNextLevel(playerId);
    }

    /**
     * Returns a list of human-readable reasons why the player cannot reach a specific level.
     */
    public List<String> getMissingForLevel(int targetLevel) {
        ServerLevelManager.LevelConfiguration config = ServerLevelManager.getLevelConfiguration(targetLevel);
        if (config == null) {
            return List.of("Invalid level: " + targetLevel);
        }

        return checkRequirements(config.getRequirements());
    }

    /**
     * Checks if player can level up to the next level.
     */
    public boolean canLevelUp() {
        return ServerLevelManager.canPlayerLevelUp(player.getUniqueId());
    }

    /**
     * Checks requirements against a requirement set.
     */
    private List<String> checkRequirements(ServerLevelManager.RequirementSet requirements) {
        List<String> missing = new ArrayList<>();

        // Money requirement
        if (requirements.getMoney() > 0) {
            if (econ == null) {
                missing.add("Economy system not available");
            } else if (!econ.has(player, requirements.getMoney())) {
                missing.add(String.format("$%.2f required", requirements.getMoney()));
            }
        }

        // Player kills requirement
        if (requirements.getPlayerKills() > 0) {
            int currentKills = player.getStatistic(Statistic.PLAYER_KILLS);
            if (currentKills < requirements.getPlayerKills()) {
                missing.add(String.format("%d player kills required", requirements.getPlayerKills()));
            }
        }

        // Mob kills requirement
        if (requirements.getMobKills() > 0) {
            int currentKills = player.getStatistic(Statistic.MOB_KILLS);
            if (currentKills < requirements.getMobKills()) {
                missing.add(String.format("%d mob kills required", requirements.getMobKills()));
            }
        }

        // Playtime requirement
        if (requirements.getPlaytimeHours() > 0) {
            long currentTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
            long currentHours = (currentTicks * 50L) / 3_600_000L; // Convert ticks to hours
            if (currentHours < requirements.getPlaytimeHours()) {
                missing.add(String.format("%d hours playtime required", requirements.getPlaytimeHours()));
            }
        }

        // Experience requirement
        if (requirements.getExperience() > 0) {
            if (player.getTotalExperience() < requirements.getExperience()) {
                missing.add(String.format("%d experience required", requirements.getExperience()));
            }
        }

        return missing;
    }
}
