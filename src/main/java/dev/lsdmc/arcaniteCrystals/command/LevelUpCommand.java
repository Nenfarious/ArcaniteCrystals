// src/main/java/dev/lsdmc/arcaniteCrystals/command/LevelUpCommand.java
package dev.lsdmc.arcaniteCrystals.command;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.manager.LevelManager;
import dev.lsdmc.arcaniteCrystals.manager.LevelManager.LevelConfig;
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
        int currentLevel = PlayerDataManager.getLevel(uid);
        int nextLevel = currentLevel + 1;
        
        // Check if already at max level
        if (!LevelManager.isValidLevel(nextLevel)) {
            MessageManager.sendNotification(player,
                ChatColor.GOLD + "🏆 " + ChatColor.BOLD + "MAX LEVEL REACHED" + 
                ChatColor.RESET + ChatColor.GOLD + " 🏆\n" + 
                ChatColor.GRAY + "You have achieved the highest level possible!\n" +
                ChatColor.GREEN + "Congratulations on your mastery of Arcanite Crystals!",
                MessageManager.NotificationType.SUCCESS);
            return true;
        }
        
        LevelConfig config = LevelManager.getConfigForLevel(nextLevel);
        if (config == null) {
            MessageManager.sendError(player, "error.noRequirements",
                "Contact an administrator - level configuration missing!");
            return true;
        }
        
        // Check requirements with detailed feedback
        RequirementChecker checker = new RequirementChecker(player, config);
        List<String> missing = checker.getMissing();
        
        if (!missing.isEmpty()) {
            // Beautiful requirements display
            StringBuilder reqMessage = new StringBuilder();
            reqMessage.append(ChatColor.RED).append("❌ ").append(ChatColor.BOLD)
                     .append("REQUIREMENTS NOT MET").append(ChatColor.RESET).append("\n")
                     .append(ChatColor.GRAY).append("To reach ").append(ChatColor.AQUA)
                     .append("Level ").append(nextLevel).append(ChatColor.GRAY)
                     .append(", you need:\n");
            
            for (String requirement : missing) {
                reqMessage.append(ChatColor.YELLOW).append("  ► ")
                         .append(ChatColor.RED).append(requirement).append("\n");
            }
            
            reqMessage.append(ChatColor.GREEN).append("💡 Keep playing to meet these requirements!");
            
            MessageManager.sendNotification(player, reqMessage.toString(), 
                MessageManager.NotificationType.WARNING);
            return true;
        }
        
        // All requirements met - process level up!
        performLevelUp(player, config, currentLevel, nextLevel);
        
        return true;
    }
    
    private void performLevelUp(Player player, LevelConfig config, int fromLevel, int toLevel) {
        UUID uid = player.getUniqueId();
        
        // Deduct money if required
        if (config.getMoneyRequirement() > 0) {
            econ.withdrawPlayer(player, config.getMoneyRequirement());
        }
        
        // Update player level
        PlayerDataManager.setLevel(uid, toLevel);
        
        // Apply new level buffs
        LevelManager.applyBuffs(player, toLevel);
        
        // Get proper level tag from configuration (must be final for lambda)
        final String levelTag = ConfigManager.getLevelsConfig().getString("level-" + toLevel + ".tag") != null 
            ? ConfigManager.getLevelsConfig().getString("level-" + toLevel + ".tag")
            : "&7[" + getRomanNumeral(toLevel) + "]"; // Fallback to Roman numeral
        
        // Epic level up sequence
        ArcaniteCrystals.getInstance().getServer().getScheduler().runTaskLater(
            ArcaniteCrystals.getInstance(), () -> {
            
            // Sound and particle effects
            SoundManager.playLevelUpSound(player);
            ParticleManager.playLevelUpEffect(player);
            
            // Professional level up message
            MessageManager.sendLevelUp(player, toLevel, levelTag);
            
            // Show new benefits
            showLevelBenefits(player, config, toLevel);
            
        }, 5L); // Small delay for dramatic effect
    }
    
    private void showLevelBenefits(Player player, LevelConfig config, int level) {
        ArcaniteCrystals.getInstance().getServer().getScheduler().runTaskLater(
            ArcaniteCrystals.getInstance(), () -> {
            
            StringBuilder benefits = new StringBuilder();
            benefits.append(ChatColor.GOLD).append("✨ ").append(ChatColor.BOLD)
                   .append("NEW LEVEL BENEFITS").append(ChatColor.RESET)
                   .append(ChatColor.GOLD).append(" ✨\n");
            
            // Show tier and slots
            benefits.append(ChatColor.AQUA).append("► Max Effect Tier: ")
                   .append(ChatColor.GREEN).append(config.getTier()).append("\n");
            benefits.append(ChatColor.AQUA).append("► Crystal Slots: ")
                   .append(ChatColor.GREEN).append(config.getSlots()).append("\n");
            
            // Show permanent buffs
            if (!config.getBuffs().isEmpty()) {
                benefits.append(ChatColor.AQUA).append("► Permanent Buffs:\n");
                for (var buff : config.getBuffs().entrySet()) {
                    String buffName = beautifyBuffName(buff.getKey());
                    benefits.append(ChatColor.LIGHT_PURPLE).append("  • ")
                           .append(buffName).append(": ").append(ChatColor.GREEN)
                           .append("+").append(formatBuffValue(buff.getValue()))
                           .append("\n");
                }
            }
            
            benefits.append(ChatColor.GREEN).append("Use ").append(ChatColor.GOLD)
                   .append("/arcanite talents").append(ChatColor.GREEN)
                   .append(" to explore new upgrades!");
            
            MessageManager.sendNotification(player, benefits.toString(),
                MessageManager.NotificationType.INFO);
                
        }, 60L); // Show benefits after celebration
    }
    
    private String beautifyBuffName(String buffKey) {
        return switch (buffKey.toLowerCase()) {
            case "max_health" -> "Max Health";
            case "walk_speed" -> "Movement Speed";
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
    
    /**
     * Converts numbers to Roman numerals for fallback tag generation.
     */
    private String getRomanNumeral(int number) {
        String[] romans = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return number > 0 && number <= romans.length ? romans[number - 1] : String.valueOf(number);
    }
}
