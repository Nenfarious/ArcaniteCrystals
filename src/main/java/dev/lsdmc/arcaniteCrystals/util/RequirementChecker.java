// src/main/java/dev/lsdmc/arcaniteCrystals/util/RequirementChecker.java
package dev.lsdmc.arcaniteCrystals.util;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.manager.LevelManager.LevelConfig;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks a player's progress against level requirements.
 */
public class RequirementChecker {

    private final Player player;
    private final LevelConfig config;
    private final Economy econ;

    public RequirementChecker(Player player, LevelConfig config) {
        this.player = player;
        this.config = config;
        RegisteredServiceProvider<Economy> rsp = ArcaniteCrystals.getInstance()
                .getServer().getServicesManager()
                .getRegistration(Economy.class);
        this.econ = (rsp != null) ? rsp.getProvider() : null;
    }

    /**
     * Returns a list of human-readable reasons why the player cannot level up.
     */
    public List<String> getMissing() {
        List<String> missing = new ArrayList<>();

        // Money requirement
        int moneyReq = config.getMoneyRequirement();
        if (moneyReq > 0) {
            if (econ == null) {
                missing.add("No economy provider available");
            } else if (!econ.has(player, moneyReq)) {
                missing.add("Balance of at least $" + moneyReq + " required");
            }
        }

        // Player kills requirement
        int killsReq = config.getKillsRequirement();
        if (killsReq > 0 && player.getStatistic(Statistic.PLAYER_KILLS) < killsReq) {
            missing.add(killsReq + " player kills required");
        }

        // Playtime requirement (in hours)
        long timeMsReq = config.getTimeRequirementMs();
        if (timeMsReq > 0) {
            long ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
            long msPlayed = ticks * 50L;
            if (msPlayed < timeMsReq) {
                long hoursReq = timeMsReq / 3_600_000L;
                missing.add(hoursReq + " hours of playtime required");
            }
        }

        return missing;
    }
}
