// src/main/java/dev/lsdmc/arcaniteCrystals/listener/PlayerJoinListener.java
package dev.lsdmc.arcaniteCrystals.listener;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager;
import dev.lsdmc.arcaniteCrystals.manager.EffectApplierManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.UUID;

/**
 * Professional player management system handling join/quit events,
 * data synchronization, level restoration, and comprehensive player state management.
 */
public class PlayerJoinListener implements Listener {

    private final ArcaniteCrystals plugin = ArcaniteCrystals.getInstance();

    /**
     * Handles player join with comprehensive initialization and restoration.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        try {
            // Delayed initialization to ensure all systems are ready
            new BukkitRunnable() {
                @Override
                public void run() {
                    initializePlayerData(player);
                }
            }.runTaskLater(plugin, 20L); // 1 second delay
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error initializing player " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Comprehensive player data initialization and restoration.
     */
    private void initializePlayerData(Player player) {
        UUID playerId = player.getUniqueId();
        
        try {
            // Validate and restore player level with error recovery
            int playerLevel = validateAndRestoreLevel(player);
            
            // Apply level buffs based on current player level
            if (playerLevel > 1) {
                dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.applyBuffs(player, playerLevel);
                plugin.getLogger().fine("Applied level " + playerLevel + " buffs to " + player.getName());
            }
            
            // Synchronize and validate upgrades
            Set<String> upgrades = validateAndSyncUpgrades(player);
            
            // Check for any active crystals that need restoration
            restoreActiveCrystalState(player);
            
            // Send personalized welcome message
            sendPersonalizedWelcome(player, playerLevel, upgrades);
            
            // Log successful initialization
            plugin.getLogger().info("Successfully initialized player data for " + player.getName() + 
                                   " (Level: " + playerLevel + ", Upgrades: " + upgrades.size() + ")");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize player data for " + player.getName() + ": " + e.getMessage());
            
            // Fallback: basic initialization
            try {
                PlayerDataManager.setLevel(playerId, 1);
                player.sendMessage(ChatColor.RED + "Warning: Player data had issues and has been reset. Contact an admin if this persists.");
            } catch (Exception fallbackError) {
                plugin.getLogger().severe("Critical: Even fallback initialization failed for " + player.getName());
            }
        }
    }
    
    /**
     * Validates and restores player level with comprehensive error handling.
     */
    private int validateAndRestoreLevel(Player player) {
        UUID playerId = player.getUniqueId();
        int currentLevel = PlayerDataManager.getLevel(playerId);
        
        // Validate level is within bounds
        if (currentLevel < 1) {
            plugin.getLogger().warning("Player " + player.getName() + " had invalid level " + currentLevel + ", resetting to 1");
            currentLevel = 1;
            PlayerDataManager.setLevel(playerId, currentLevel);
        } else if (currentLevel > 10) {
            plugin.getLogger().warning("Player " + player.getName() + " had invalid level " + currentLevel + ", capping to 10");
            currentLevel = 10;
            PlayerDataManager.setLevel(playerId, currentLevel);
        }
        
        // Validate level configuration exists
                    if (!dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.isValidLevel(currentLevel)) {
            plugin.getLogger().warning("Player " + player.getName() + " had level " + currentLevel + " but no config exists, resetting to 1");
            currentLevel = 1;
            PlayerDataManager.setLevel(playerId, currentLevel);
        }
        
        return currentLevel;
    }
    
    /**
     * Validates and synchronizes player upgrades.
     */
    private Set<String> validateAndSyncUpgrades(Player player) {
        UUID playerId = player.getUniqueId();
        Set<String> upgrades = PlayerDataManager.getUnlockedUpgrades(playerId);
        
        // Validate each upgrade still exists in configuration
        Set<String> validUpgrades = new java.util.HashSet<>();
        int removedCount = 0;
        
        for (String upgradeId : upgrades) {
            if (isValidUpgrade(upgradeId)) {
                validUpgrades.add(upgradeId);
            } else {
                plugin.getLogger().warning("Removed invalid upgrade '" + upgradeId + "' from player " + player.getName());
                PlayerDataManager.revokeUpgrade(playerId, upgradeId);
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            player.sendMessage(ChatColor.YELLOW + "Note: " + removedCount + " outdated upgrades were removed from your account.");
        }
        
        return validUpgrades;
    }
    
    /**
     * Validates if an upgrade ID exists in the current configuration.
     */
    private boolean isValidUpgrade(String upgradeId) {
        try {
            var upgradesConfig = dev.lsdmc.arcaniteCrystals.config.ConfigManager.getUpgradesConfig();
            return upgradesConfig.getConfigurationSection("upgrades." + upgradeId) != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Restores active crystal state if the player had one when they logged off.
     */
    private void restoreActiveCrystalState(Player player) {
        try {
            // Check if player has an active crystal in off-hand that needs restoration
            var offHandItem = player.getInventory().getItemInOffHand();
            
            if (CrystalManager.isActivatedCrystal(offHandItem)) {
                // Add slight delay to ensure player is fully loaded
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            // Re-activate the effect manager for this player
                            EffectApplierManager.addActive(player.getUniqueId());
                            plugin.getLogger().fine("Restored active crystal state for " + player.getName());
                        }
                    }
                }.runTaskLater(plugin, 40L); // 2 second delay
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error restoring crystal state for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Sends personalized welcome message based on player progress.
     */
    private void sendPersonalizedWelcome(Player player, int level, Set<String> upgrades) {
        try {
            // Determine welcome message based on progress
            if (level == 1 && upgrades.isEmpty()) {
                // New player welcome
                sendNewPlayerWelcome(player);
            } else if (level <= 3) {
                // Beginning player welcome
                sendBeginnerWelcome(player, level, upgrades.size());
            } else if (level <= 7) {
                // Intermediate player welcome
                sendIntermediateWelcome(player, level, upgrades.size());
            } else {
                // Advanced player welcome
                sendAdvancedWelcome(player, level, upgrades.size());
            }
            
            // Check for any special achievements or milestones
            checkForMilestones(player, level, upgrades.size());
            
        } catch (Exception e) {
            // Fallback to simple message
            player.sendMessage(ChatColor.GOLD + "Welcome back to ArcaniteCrystals!");
        }
    }
    
    /**
     * Sends welcome message for new players.
     */
    private void sendNewPlayerWelcome(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                
                player.sendMessage("");
                player.sendMessage(ChatColor.GOLD + "✨ " + ChatColor.BOLD + "Welcome to ArcaniteCrystals! " + ChatColor.GOLD + "✨");
                player.sendMessage(ChatColor.GRAY + "You're about to embark on a mystical journey!");
                player.sendMessage("");
                player.sendMessage(ChatColor.YELLOW + "🎯 Getting Started:");
                player.sendMessage(ChatColor.AQUA + "  ► Use " + ChatColor.WHITE + "/arcanite talents" + ChatColor.AQUA + " to view upgrades");
                player.sendMessage(ChatColor.AQUA + "  ► Use " + ChatColor.WHITE + "/mysterycrystal" + ChatColor.AQUA + " to get your first crystal");
                player.sendMessage(ChatColor.AQUA + "  ► Use " + ChatColor.WHITE + "/levelup" + ChatColor.AQUA + " when you meet requirements");
                player.sendMessage("");
                player.sendMessage(ChatColor.GREEN + "Good luck on your journey, " + player.getName() + "!");
                
                // New player effects
                SoundManager.playSuccessSound(player);
                ParticleManager.playUpgradeEffect(player);
            }
        }.runTaskLater(plugin, 60L); // 3 second delay
    }
    
    /**
     * Sends welcome message for beginning players.
     */
    private void sendBeginnerWelcome(Player player, int level, int upgradeCount) {
        String message = ChatColor.BLUE + "Welcome back, " + ChatColor.GOLD + player.getName() + ChatColor.BLUE + "!";
        player.sendMessage(message);
        
        if (upgradeCount > 0) {
            player.sendMessage(ChatColor.GRAY + "You have " + ChatColor.GREEN + upgradeCount + 
                             ChatColor.GRAY + " upgrades unlocked at level " + ChatColor.YELLOW + level + ChatColor.GRAY + ".");
            player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.AQUA + "/arcanite talents" + 
                             ChatColor.GRAY + " to view them or " + ChatColor.AQUA + "/mysterycrystal" + 
                             ChatColor.GRAY + " to get a crystal!");
        } else {
            player.sendMessage(ChatColor.GRAY + "You're level " + ChatColor.YELLOW + level + 
                             ChatColor.GRAY + " but haven't unlocked any upgrades yet!");
            player.sendMessage(ChatColor.GRAY + "Visit " + ChatColor.AQUA + "/arcanite talents" + 
                             ChatColor.GRAY + " to start unlocking crystal powers!");
        }
    }
    
    /**
     * Sends welcome message for intermediate players.
     */
    private void sendIntermediateWelcome(Player player, int level, int upgradeCount) {
        player.sendMessage(ChatColor.DARK_PURPLE + "⚡ " + ChatColor.BOLD + player.getName() + 
                         " the Crystal Wielder returns! " + ChatColor.DARK_PURPLE + "⚡");
        player.sendMessage(ChatColor.GRAY + "Level " + ChatColor.GOLD + level + ChatColor.GRAY + 
                         " • " + ChatColor.GREEN + upgradeCount + " upgrades" + ChatColor.GRAY + 
                         " • Ready for adventure!");
        
        // Suggest next steps
        if (level < 5) {
            player.sendMessage(ChatColor.YELLOW + "💡 Tip: Reach level 5 to unlock Rare crystal crafting!");
        } else {
            player.sendMessage(ChatColor.YELLOW + "💡 Tip: Try crafting rare crystals for better effects!");
        }
    }
    
    /**
     * Sends welcome message for advanced players.
     */
    private void sendAdvancedWelcome(Player player, int level, int upgradeCount) {
        player.sendMessage(ChatColor.LIGHT_PURPLE + "★ " + ChatColor.BOLD + "Master " + player.getName() + 
                         " has returned! " + ChatColor.LIGHT_PURPLE + "★");
        player.sendMessage(ChatColor.GRAY + "Level " + ChatColor.GOLD + level + ChatColor.GRAY + 
                         " • " + ChatColor.GREEN + upgradeCount + " upgrades" + ChatColor.GRAY + 
                         " • Crystal mastery achieved!");
        
        if (level >= 8) {
            player.sendMessage(ChatColor.GOLD + "🏆 You can now craft Legendary crystals - the ultimate power!");
        }
        
        // Advanced player particle effect
        ParticleManager.playLevelUpEffect(player);
    }
    
    /**
     * Checks for achievement milestones and celebrates them.
     */
    private void checkForMilestones(Player player, int level, int upgradeCount) {
        // Check for perfect upgrade collection
        if (level >= 5 && upgradeCount >= 15) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        player.sendMessage(ChatColor.GOLD + "🏆 " + ChatColor.BOLD + "ACHIEVEMENT: Crystal Collector!");
                        player.sendMessage(ChatColor.YELLOW + "You've unlocked a significant number of upgrades!");
                        SoundManager.playLevelUpSound(player);
                    }
                }
            }.runTaskLater(plugin, 100L);
        }
        
        // Check for max level achievement
        if (level == 10) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        player.sendMessage(ChatColor.RED + "👑 " + ChatColor.BOLD + "LEGENDARY: Crystal Master!");
                        player.sendMessage(ChatColor.GOLD + "You have reached the pinnacle of crystal mastery!");
                        SoundManager.playLevelUpSound(player);
                        ParticleManager.playLevelUpEffect(player);
                    }
                }
            }.runTaskLater(plugin, 120L);
        }
    }

    /**
     * Handles player quit with comprehensive cleanup and data saving.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        handlePlayerDisconnect(event.getPlayer(), "quit");
    }
    
    /**
     * Handles player kick with comprehensive cleanup and data saving.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        handlePlayerDisconnect(event.getPlayer(), "kick");
    }
    
    /**
     * Comprehensive player disconnect handling.
     */
    private void handlePlayerDisconnect(Player player, String reason) {
        UUID playerId = player.getUniqueId();
        
        try {
            // Clean up any active crystal state
            CrystalManager.handlePlayerDisconnect(player);
            
            // Remove from effect manager
            EffectApplierManager.removeActive(playerId);
            
            // Force save player data to prevent loss
            PlayerDataManager.saveAll();
            
            plugin.getLogger().fine("Cleaned up player data for " + player.getName() + " (" + reason + ")");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error during disconnect cleanup for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
