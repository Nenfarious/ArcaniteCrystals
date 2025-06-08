// src/main/java/dev/lsdmc/arcaniteCrystals/command/ArcaniteCommand.java
package dev.lsdmc.arcaniteCrystals.command;

import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.menu.TalentMenu;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import dev.lsdmc.arcaniteCrystals.menu.CraftingMenu;
import dev.lsdmc.arcaniteCrystals.menu.ArcaniteMainMenu;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ArcaniteCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = Arrays.asList(
            "help", "info", "talents", "give", "mystery", "reload", "stats", "health", "admin", "crafting",
            "setlevel", "grant", "revoke", "view", "cooldown", "energy", "maintenance"
    );

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                new ArcaniteMainMenu((Player) sender).open();
            } else {
                sendConsoleHelp(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        
        switch (sub) {
            case "help":
                if (sender instanceof Player) {
                    sendHelp((Player) sender);
                } else {
                    sendConsoleHelp(sender);
                }
                break;
                
            case "talents":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                    return true;
                }
                if (!sender.hasPermission("arcanite.talents")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                    return true;
                }
                new TalentMenu((Player) sender).open();
                break;
                
            case "stats":
                showStats(sender);
                break;
                
            case "health":
                showHealth(sender);
                break;
                
            case "admin":
                if (!sender.hasPermission("arcanite.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                    return true;
                }
                if (args.length < 2) {
                    sendAdminHelp(sender);
                    return true;
                }
                handleAdminCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
                
            case "crafting":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                    return true;
                }
                Player playerCrafting = (Player) sender;
                if (!playerCrafting.hasPermission("arcanite.crafting")) {
                    playerCrafting.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                    return true;
                }
                new CraftingMenu(playerCrafting).open();
                break;
                
            case "give":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                    return true;
                }
                Player playerGive = (Player) sender;
                if (!playerGive.hasPermission("arcanite.give")) {
                    playerGive.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                    return true;
                }
                if (args.length < 2) {
                    playerGive.sendMessage(ChatColor.RED + "Usage: /arcanite give <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    playerGive.sendMessage(ChatColor.RED + "Player not found!");
                    return true;
                }
                CrystalManager.giveNewCrystal(target);
                playerGive.sendMessage(ChatColor.GREEN + "Gave a crystal to " + target.getName());
                break;
                
            case "mystery":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                    return true;
                }
                if (!sender.hasPermission("arcanite.mystery")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                    return true;
                }
                Player player = (Player) sender;
                ItemStack mysteryCrystal = CrystalManager.createMysteryCrystal();
                player.getInventory().addItem(mysteryCrystal);
                player.sendMessage(ChatColor.GREEN + "You received a mystery crystal!");
                break;
                
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /arcanite help for help.");
                break;
        }
        
        return true;
    }

    private void sendHelp(Player p) {
        p.sendMessage("§6§l=== ArcaniteCrystals Help ===");
        p.sendMessage("§e/arcanite talents §7- Open the Crystal Upgrades menu");
        p.sendMessage("§e/arcanite crafting §7- View crystal crafting recipes");
        p.sendMessage("§e/arcanite give <player> §7- Give a blank crystal to a player");
        p.sendMessage("§e/arcanite mystery §7- Get a mystery crystal with random effects");
        p.sendMessage("§e/arcanite info §7- Show plugin information");
        p.sendMessage("§e/levelup §7- Level up if you meet the requirements");
        
        if (p.hasPermission("arcanite.admin")) {
            p.sendMessage("§c§l=== Admin Commands ===");
            p.sendMessage("§c/arcanite reload §7- Reload configuration");
            p.sendMessage("§c/arcanite stats §7- View performance statistics");
            p.sendMessage("§c/arcanite health §7- Check system health");
            p.sendMessage("§c/arcanite admin §7- Access admin tools");
        }
        
        p.sendMessage("§6§l================================");
    }
    
    private void sendConsoleHelp(CommandSender sender) {
        sender.sendMessage("=== ArcaniteCrystals Console Commands ===");
        sender.sendMessage("/arcanite give <player> - Give a blank crystal to a player");
        sender.sendMessage("/arcanite crafting - View crystal crafting recipes");
        sender.sendMessage("/arcanite reload - Reload configuration");
        sender.sendMessage("/arcanite stats - View performance statistics");
        sender.sendMessage("/arcanite health - Check system health");
        sender.sendMessage("/arcanite admin - Access admin tools");
        sender.sendMessage("=========================================");
    }

    private void showStats(CommandSender sender) {
        sender.sendMessage("§6=== ArcaniteCrystals Statistics ===");
        sender.sendMessage("§7Effect Manager: §e" + 
                          dev.lsdmc.arcaniteCrystals.manager.EffectApplierManager.getStats());
        sender.sendMessage("§7Player Data: §e" + 
                          dev.lsdmc.arcaniteCrystals.database.PlayerDataManager.getCacheStats());
        sender.sendMessage("§7Database: §e" + 
                          dev.lsdmc.arcaniteCrystals.database.SqliteDataManager.getStats());
    }

    private void showHealth(CommandSender sender) {
        sender.sendMessage("§6=== System Health Check ===");
        
        // Database health
        boolean dbHealthy = dev.lsdmc.arcaniteCrystals.database.SqliteDataManager.isConnected();
        sender.sendMessage("§7Database: " + (dbHealthy ? "§aHealthy" : "§cUnhealthy"));
        
        // Effect manager health  
        boolean effectsRunning = dev.lsdmc.arcaniteCrystals.manager.EffectApplierManager.isRunning();
        sender.sendMessage("§7Effect Manager: " + (effectsRunning ? "§aRunning" : "§cStopped"));
        
        // Memory usage
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long total = runtime.totalMemory();
        double percentage = (double) used / total * 100;
        
        String memoryStatus = percentage > 80 ? "§c" : percentage > 60 ? "§e" : "§a";
        sender.sendMessage("§7Memory Usage: " + memoryStatus + 
                          String.format("%.1f%% (%.1fMB/%.1fMB)", 
                                       percentage, used / 1048576.0, total / 1048576.0));
        
        // Overall status
        boolean healthy = dbHealthy && effectsRunning && percentage < 90;
        sender.sendMessage("§7Overall Status: " + (healthy ? "§aHealthy" : "§cNeeds Attention"));
    }

    /**
     * Handles professional admin sub-commands.
     */
    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendAdminHelp(sender);
            return true;
        }
        
        String adminSub = args[0].toLowerCase();
        switch (adminSub) {
            case "setlevel":
                return handleSetLevel(sender, args);
            case "grant":
                return handleGrantUpgrade(sender, args);
            case "revoke":
                return handleRevokeUpgrade(sender, args);
            case "view":
                return handleViewPlayer(sender, args);
            case "resetcooldown":
                return handleResetCooldown(sender, args);
            case "resetenergy":
                return handleResetEnergy(sender, args);
            case "maintenance":
                return handleMaintenance(sender, args);
            default:
                sendAdminHelp(sender);
                return true;
        }
    }
    
    /**
     * Handles setting player levels.
     */
    private boolean handleSetLevel(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(MessageManager.get("usage.setlevel"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MessageManager.get("error.playerNotFound"));
            return true;
        }
        
        try {
            int level = Integer.parseInt(args[2]);
            if (level < 1 || level > 10) {
                sender.sendMessage("§cLevel must be between 1-10!");
                return true;
            }
            
            dev.lsdmc.arcaniteCrystals.database.PlayerDataManager.setLevel(target.getUniqueId(), level);
            dev.lsdmc.arcaniteCrystals.manager.LevelManager.applyBuffs(target, level);
            
            String message = MessageManager.getMessage("success.setLevel", 
                Map.of("player", target.getName(), "level", String.valueOf(level)));
            sender.sendMessage(message);
            
            // Notify target player
            MessageManager.sendLevelUp(target, level, 
                dev.lsdmc.arcaniteCrystals.manager.LevelManager.getConfigForLevel(level) != null ? "Level " + level : "Level " + level);
                
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageManager.get("error.invalidNumber"));
        }
        
        return true;
    }
    
    /**
     * Handles granting upgrades to players.
     */
    private boolean handleGrantUpgrade(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(MessageManager.get("usage.grant"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MessageManager.get("error.playerNotFound"));
            return true;
        }
        
        String upgradeId = args[2].toLowerCase();
        dev.lsdmc.arcaniteCrystals.database.PlayerDataManager.unlockUpgrade(target.getUniqueId(), upgradeId);
        
        String message = MessageManager.getMessage("success.grant",
            Map.of("upgrade", upgradeId, "player", target.getName()));
        sender.sendMessage(message);
        
        return true;
    }
    
    /**
     * Handles revoking upgrades from players.
     */
    private boolean handleRevokeUpgrade(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(MessageManager.get("usage.revoke"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MessageManager.get("error.playerNotFound"));
            return true;
        }
        
        String upgradeId = args[2].toLowerCase();
        dev.lsdmc.arcaniteCrystals.database.PlayerDataManager.revokeUpgrade(target.getUniqueId(), upgradeId);
        
        String message = MessageManager.getMessage("success.revoke",
            Map.of("upgrade", upgradeId, "player", target.getName()));
        sender.sendMessage(message);
        
        return true;
    }
    
    /**
     * Handles viewing player information.
     */
    private boolean handleViewPlayer(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(MessageManager.get("usage.view"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MessageManager.get("error.playerNotFound"));
            return true;
        }
        
        UUID playerId = target.getUniqueId();
        int level = dev.lsdmc.arcaniteCrystals.database.PlayerDataManager.getLevel(playerId);
        int maxTier = dev.lsdmc.arcaniteCrystals.manager.LevelManager.getMaxTier(playerId);
        int slots = dev.lsdmc.arcaniteCrystals.manager.LevelManager.getSlots(playerId);
        Set<String> upgrades = dev.lsdmc.arcaniteCrystals.database.PlayerDataManager.getUnlockedUpgrades(playerId);
        
        sender.sendMessage("§6=== Player Info: " + target.getName() + " ===");
        sender.sendMessage("§7Level: §e" + level);
        sender.sendMessage("§7Max Tier: §e" + maxTier);
        sender.sendMessage("§7Crystal Slots: §e" + slots);
        sender.sendMessage("§7Unlocked Upgrades: §e" + upgrades.size());
        sender.sendMessage("§7Upgrades: §f" + String.join(", ", upgrades));
        
        return true;
    }
    
    /**
     * Handles resetting player cooldowns.
     */
    private boolean handleResetCooldown(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(MessageManager.get("usage.resetcooldown"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MessageManager.get("error.playerNotFound"));
            return true;
        }
        
        dev.lsdmc.arcaniteCrystals.database.PlayerDataManager.setCooldown(target.getUniqueId(), 0L);
        
        String message = MessageManager.getMessage("success.resetCooldown", "player", target.getName());
        sender.sendMessage(message);
        
        return true;
    }
    
    /**
     * Handles resetting crystal energy (placeholder for future implementation).
     */
    private boolean handleResetEnergy(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(MessageManager.get("usage.resetenergy"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MessageManager.get("error.playerNotFound"));
            return true;
        }
        
        // Note: This would need implementation in CrystalManager to reset crystal energy
        String message = MessageManager.getMessage("success.resetEnergy", "player", target.getName());
        sender.sendMessage(message);
        
        return true;
    }
    
    /**
     * Handles maintenance commands.
     */
    private boolean handleMaintenance(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /arcanite admin maintenance <save|backup|cleanup>");
            return true;
        }
        
        String maintenanceType = args[1].toLowerCase();
        switch (maintenanceType) {
            case "save":
                dev.lsdmc.arcaniteCrystals.database.PlayerDataManager.saveAll();
                sender.sendMessage("§aForced save of all player data completed.");
                break;
            case "backup":
                String backupPath = dev.lsdmc.arcaniteCrystals.config.ConfigManager.backupConfig();
                if (backupPath != null) {
                    sender.sendMessage("§aConfiguration backup created: " + backupPath);
                } else {
                    sender.sendMessage("§cBackup failed!");
                }
                break;
            case "cleanup":
                dev.lsdmc.arcaniteCrystals.database.PlayerDataManager.clearCache();
                sender.sendMessage("§aCleared player data cache.");
                break;
            default:
                sender.sendMessage("§cInvalid maintenance type. Use: save, backup, or cleanup");
                break;
        }
        
        return true;
    }

    /**
     * Sends professional admin help information.
     */
    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage("§6=== ArcaniteCrystals Admin Commands ===");
        sender.sendMessage("§e/arcanite admin setlevel <player> <level> §8- Set player level");
        sender.sendMessage("§e/arcanite admin grant <player> <upgrade> §8- Grant upgrade to player");
        sender.sendMessage("§e/arcanite admin revoke <player> <upgrade> §8- Revoke upgrade from player");
        sender.sendMessage("§e/arcanite admin view <player> §8- View player information");
        sender.sendMessage("§e/arcanite admin resetcooldown <player> §8- Reset crystal cooldown");
        sender.sendMessage("§e/arcanite admin resetenergy <player> §8- Reset crystal energy");
        sender.sendMessage("§e/arcanite admin maintenance <save|backup|cleanup> §8- Maintenance tools");
        sender.sendMessage("§6=====================================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            String part = args[0].toLowerCase();
            return SUBS.stream()
                    .filter(sub -> sub.startsWith(part))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
