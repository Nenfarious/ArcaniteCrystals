// src/main/java/dev/lsdmc/arcaniteCrystals/command/LevelUpCommand.java
package dev.lsdmc.arcaniteCrystals.command;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.util.RequirementChecker;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.List;
import java.util.UUID;

public class LevelUpCommand implements CommandExecutor {

    private final Economy econ;

    public LevelUpCommand() {
        RegisteredServiceProvider<Economy> rsp = ArcaniteCrystals.getInstance()
                .getServer().getServicesManager().getRegistration(Economy.class);
        econ = (rsp != null) ? rsp.getProvider() : null;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!(s instanceof Player)) {
            s.sendMessage(MessageManager.get("error.playersOnly")); 
            return true;
        }
        
        Player player = (Player) s;
        
        if (econ == null) {
            MessageManager.sendError(player, "error.noEconomy", 
                "Contact an administrator - Vault economy not found!");
            return true;
        }
        
        UUID uid = player.getUniqueId();
        int currentLevel = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getPlayerLevel(uid);
        int nextLevel = currentLevel + 1;
        
        // Check if already at max level using ServerLevelManager
        int maxLevel = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getMaxLevel();
        if (currentLevel >= maxLevel) {
            // Get current level configuration for display
            var currentConfig = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getLevelConfiguration(currentLevel);
            String levelTag = currentConfig != null ? currentConfig.getTag() : "&6[MAX]";
            
            MessageManager.sendNotification(player,
                ChatColor.GOLD + "🏆 " + ChatColor.BOLD + "MAXIMUM LEVEL ACHIEVED" + 
                ChatColor.RESET + ChatColor.GOLD + " 🏆\n" + 
                ChatColor.GRAY + "You have reached " + ChatColor.translateAlternateColorCodes('&', levelTag) + "\n" +
                ChatColor.GREEN + "Congratulations on mastering all server progression!",
                MessageManager.NotificationType.SUCCESS);
            return true;
        }
        
        // Get next level configuration using ServerLevelManager
        var nextLevelConfig = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getLevelConfiguration(nextLevel);
        if (nextLevelConfig == null) {
            MessageManager.sendError(player, "error.noRequirements",
                "Contact an administrator - level configuration missing!");
            return true;
        }
        
        // Check requirements using ServerLevelManager
        java.util.List<String> missingRequirements = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getMissingRequirementsForNextLevel(uid);
        
        if (!missingRequirements.isEmpty()) {
            // Enhanced requirements display with progress information
            StringBuilder reqMessage = new StringBuilder();
            reqMessage.append(ChatColor.RED).append("❌ ").append(ChatColor.BOLD)
                     .append("LEVEL UP REQUIREMENTS NOT MET").append(ChatColor.RESET).append("\n")
                     .append(ChatColor.GRAY).append("To become ").append(ChatColor.translateAlternateColorCodes('&', nextLevelConfig.getTag()))
                     .append(ChatColor.GRAY).append(" - ").append(ChatColor.AQUA).append(nextLevelConfig.getDisplayName())
                     .append(ChatColor.GRAY).append(":\n\n");
            
            // Show current progress for each requirement
            reqMessage.append(ChatColor.YELLOW).append("Missing Requirements:\n");
            for (String requirement : missingRequirements) {
                reqMessage.append(ChatColor.RED).append("  ► ")
                         .append(requirement).append("\n");
            }
            
            // Show current player stats for context
            reqMessage.append("\n").append(ChatColor.BLUE).append("Your Current Progress:\n");
            reqMessage.append(ChatColor.GRAY).append("  • Money: ").append(ChatColor.GREEN)
                     .append(String.format("$%.2f", econ.getBalance(player))).append("\n");
            reqMessage.append(ChatColor.GRAY).append("  • Player Kills: ").append(ChatColor.GREEN)
                     .append(player.getStatistic(org.bukkit.Statistic.PLAYER_KILLS)).append("\n");
            
            long currentHours = (player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) * 50L) / 3_600_000L;
            reqMessage.append(ChatColor.GRAY).append("  • Playtime: ").append(ChatColor.GREEN)
                     .append(currentHours).append(" hours\n");
            
            reqMessage.append("\n").append(ChatColor.GREEN).append("💡 Keep playing to meet these requirements!");
            
            MessageManager.sendNotification(player, reqMessage.toString(), 
                MessageManager.NotificationType.WARNING);
            return true;
        }
        
        // All requirements met - attempt level up using ServerLevelManager
        boolean success = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.levelUpPlayer(uid);
        
        if (success) {
            // Additional feedback for the level up
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════════");
            player.sendMessage(ChatColor.GREEN + "   🎉 " + ChatColor.BOLD + "CONGRATULATIONS!" + ChatColor.RESET + ChatColor.GREEN + " 🎉");
            player.sendMessage("");
            player.sendMessage(ChatColor.GRAY + "You are now: " + ChatColor.translateAlternateColorCodes('&', nextLevelConfig.getTag()));
            player.sendMessage(ChatColor.GRAY + "Title: " + ChatColor.AQUA + nextLevelConfig.getDisplayName());
            if (!nextLevelConfig.getDescription().isEmpty()) {
                player.sendMessage(ChatColor.GRAY + "Description: " + ChatColor.YELLOW + nextLevelConfig.getDescription());
            }
            
            // Show new benefits
            if (!nextLevelConfig.getBuffs().isEmpty()) {
                player.sendMessage("");
                player.sendMessage(ChatColor.GOLD + "New Server Benefits:");
                for (var buff : nextLevelConfig.getBuffs().entrySet()) {
                    String buffName = formatBuffName(buff.getKey());
                    String buffValue = formatBuffValue(buff.getValue());
                    player.sendMessage(ChatColor.YELLOW + "  ► " + buffName + ": " + ChatColor.GREEN + "+" + buffValue);
                }
            }
            
            player.sendMessage("");
            player.sendMessage(ChatColor.BLUE + "💎 Crystal System Updates:");
            player.sendMessage(ChatColor.GRAY + "  • Max Effect Tier: " + ChatColor.AQUA + dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getMaxTier(uid));
            player.sendMessage(ChatColor.GRAY + "  • Crystal Slots: " + ChatColor.AQUA + dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getSlots(uid));
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════════");
            
        } else {
            MessageManager.sendError(player, "error.unknown", 
                "Something went wrong during level up. Please try again or contact an administrator.");
        }
        
        return true;
    }
    
    private String formatBuffName(String buffKey) {
        return switch (buffKey.toLowerCase()) {
            case "max_health" -> "Max Health";
            case "movement_speed", "walk_speed" -> "Movement Speed";
            case "attack_damage" -> "Attack Damage";
            case "knockback_resistance" -> "Knockback Resistance";
            default -> buffKey.replace("_", " ");
        };
    }
    
    private String formatBuffValue(double value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        } else {
            return String.format("%.2f", value);
        }
    }
}
