package dev.lsdmc.arcaniteCrystals.database;

import java.util.Set;

/**
 * Immutable data class for holding player data.
 * Used for batch operations and data transfer.
 */
public class PlayerData {
    private final int level;
    private final Set<String> unlockedUpgrades;
    private final long cooldown;
    private int crystalsActivated;
    private int successfulFusions;
    private int failedFusions;
    private int successfulSockets;
    private int failedSockets;
    private long totalEnergyUsed;
    private int crystalsDecayed;
    private int crystalsCorrupted;
    private int successfulIdentifications;
    private int failedIdentifications;
    
    public PlayerData(int level, Set<String> unlockedUpgrades, long cooldown) {
        this.level = level;
        this.unlockedUpgrades = Set.copyOf(unlockedUpgrades); // Defensive copy
        this.cooldown = cooldown;
    }
    
    public int getLevel() {
        return level;
    }
    
    public Set<String> getUnlockedUpgrades() {
        return unlockedUpgrades; // Already immutable
    }
    
    public long getCooldown() {
        return cooldown;
    }
    
    public int getCrystalsActivated() { return crystalsActivated; }
    public void setCrystalsActivated(int crystalsActivated) { this.crystalsActivated = crystalsActivated; }
    
    public int getSuccessfulFusions() { return successfulFusions; }
    public void setSuccessfulFusions(int successfulFusions) { this.successfulFusions = successfulFusions; }
    
    public int getFailedFusions() { return failedFusions; }
    public void setFailedFusions(int failedFusions) { this.failedFusions = failedFusions; }
    
    public int getSuccessfulSockets() { return successfulSockets; }
    public void setSuccessfulSockets(int successfulSockets) { this.successfulSockets = successfulSockets; }
    
    public int getFailedSockets() { return failedSockets; }
    public void setFailedSockets(int failedSockets) { this.failedSockets = failedSockets; }
    
    public long getTotalEnergyUsed() { return totalEnergyUsed; }
    public void setTotalEnergyUsed(long totalEnergyUsed) { this.totalEnergyUsed = totalEnergyUsed; }
    
    public int getCrystalsDecayed() { return crystalsDecayed; }
    public void setCrystalsDecayed(int crystalsDecayed) { this.crystalsDecayed = crystalsDecayed; }
    
    public int getCrystalsCorrupted() { return crystalsCorrupted; }
    public void setCrystalsCorrupted(int crystalsCorrupted) { this.crystalsCorrupted = crystalsCorrupted; }
    
    public int getSuccessfulIdentifications() { return successfulIdentifications; }
    public void setSuccessfulIdentifications(int successfulIdentifications) { this.successfulIdentifications = successfulIdentifications; }
    
    public int getFailedIdentifications() { return failedIdentifications; }
    public void setFailedIdentifications(int failedIdentifications) { this.failedIdentifications = failedIdentifications; }
    
    @Override
    public String toString() {
        return String.format("PlayerData{level=%d, upgrades=%s, cooldown=%d}",
                level, unlockedUpgrades, cooldown);
    }
} 