package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.manager.LevelManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RequirementChecker {
    private final Player player;
    private final LevelManager.LevelConfig config;

    public RequirementChecker(Player player, LevelManager.LevelConfig config) {
        this.player = player;
        this.config = config;
    }

    public List<String> getMissing() {
        List<String> missing = new ArrayList<>();
        
        // Check money requirement
        int moneyRequired = config.getMoneyRequirement();
        if (moneyRequired > 0) {
            Economy economy = ArcaniteCrystals.getEconomy();
            if (economy != null && economy.getBalance(player) < moneyRequired) {
                missing.add("Need $" + String.format("%,d", moneyRequired));
            }
        }
        
        // Check experience requirement
        int expRequired = config.getKillsRequirement();
        if (expRequired > 0 && player.getTotalExperience() < expRequired) {
            missing.add("Need " + String.format("%,d", expRequired) + " XP");
        }
        
        // Check time requirement
        long timeRequired = config.getTimeRequirementMs();
        if (timeRequired > 0) {
            long playtime = player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) * 50L;
            if (playtime < timeRequired) {
                missing.add("Need " + (timeRequired / 3_600_000L) + " hours of playtime");
            }
        }
        
        return missing;
    }
} 