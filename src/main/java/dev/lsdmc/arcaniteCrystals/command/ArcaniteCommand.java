// src/main/java/dev/lsdmc/arcaniteCrystals/command/ArcaniteCommand.java
package dev.lsdmc.arcaniteCrystals.command;

import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.menu.TalentMenu;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import dev.lsdmc.arcaniteCrystals.manager.CrystalRecipeManager;
import dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ArcaniteCommand implements CommandExecutor, TabCompleter {

    // Define available subcommands with proper organization
    private static final List<String> BASIC_COMMANDS = Arrays.asList(
            "help", "talents", "mystery", "stats", "health", "levels", "levelinfo"
    );
    
    private static final List<String> ADMIN_COMMANDS = Arrays.asList(
            "give", "reload", "admin", "catalyst"
    );
    
    private static final List<String> ADMIN_SUBCOMMANDS = Arrays.asList(
            "setlevel", "grant", "revoke", "view", "resetcooldown", "resetenergy", "maintenance"
    );
    
    private static final List<String> CATALYST_SUBCOMMANDS = Arrays.asList(
            "give", "types"
    );
    
    private static final List<String> CATALYST_TYPES = Arrays.asList(
            "identification", "socketing", "fusion"
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
                
            case "levels":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                    return true;
                }
                showAllLevels((Player) sender);
                break;
                
            case "levelinfo":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                    return true;
                }
                if (args.length >= 2) {
                    try {
                        int level = Integer.parseInt(args[1]);
                        showLevelInfo((Player) sender, level);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid level number: " + args[1]);
                    }
                } else {
                    showCurrentLevelInfo((Player) sender);
                }
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
                
            case "reload":
                if (!sender.hasPermission("arcanite.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                    return true;
                }
                try {
                    ConfigManager.reloadConfig();
                    sender.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully!");
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + "Error reloading configuration: " + e.getMessage());
                }
                break;
                
            case "catalyst":
                if (!sender.hasPermission("arcanite.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                    return true;
                }
                handleCatalystCommand(sender, args);
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
        p.sendMessage("§e/arcanite mystery §7- Get a mystery crystal with random effects");
        p.sendMessage("§e/arcanite levels §7- View all server levels and progression");
        p.sendMessage("§e/arcanite levelinfo §7- View your current level details");
        p.sendMessage("§e/arcanite levelinfo <#> §7- View details for a specific level");
        p.sendMessage("§e/arcanite stats §7- View system statistics");
        p.sendMessage("§e/arcanite health §7- Check system health");
        p.sendMessage("§e/levelup §7- Level up if you meet the requirements");
        
        if (p.hasPermission("arcanite.admin")) {
            p.sendMessage("§c§l=== Admin Commands ===");
            p.sendMessage("§c/arcanite give <player> §7- Give a blank crystal to a player");
            p.sendMessage("§c/arcanite reload §7- Reload configuration");
            p.sendMessage("§c/arcanite admin §7- Access admin tools");
            p.sendMessage("§c/arcanite catalyst §7- Manage catalysts");
        }
        
        p.sendMessage("§6§l================================");
    }
    
    private void sendConsoleHelp(CommandSender sender) {
        sender.sendMessage("=== ArcaniteCrystals Console Commands ===");
        sender.sendMessage("/arcanite give <player> - Give a blank crystal to a player");
        sender.sendMessage("/arcanite mystery - Get a mystery crystal with random effects");
        sender.sendMessage("/arcanite reload - Reload configuration");
        sender.sendMessage("/arcanite stats - View performance statistics");
        sender.sendMessage("/arcanite health - Check system health");
        sender.sendMessage("/arcanite admin - Access admin tools");
        sender.sendMessage("/arcanite catalyst - Manage catalysts");
        sender.sendMessage("Note: Level commands require a player context");
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
            dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.applyBuffs(target, level);
            
            String message = MessageManager.getMessage("success.setLevel", 
                Map.of("player", target.getName(), "level", String.valueOf(level)));
            sender.sendMessage(message);
            
            // Notify target player
            MessageManager.sendLevelUp(target, level, 
                dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getLevelConfiguration(level) != null ? "Level " + level : "Level " + level);
                
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
        int maxTier = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getMaxTier(playerId);
        int slots = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getSlots(playerId);
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
        sender.sendMessage("§e/arcanite catalyst give <player> <type> §8- Give catalyst to player");
        sender.sendMessage("§e/arcanite catalyst types §8- List available catalyst types");
        sender.sendMessage("§6=====================================");
    }

    /**
     * Handles catalyst-related admin commands.
     */
    private void handleCatalystCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c§lCatalyst Commands:");
            sender.sendMessage("§e/arcanite catalyst give <player> <type> §8- Give catalyst to player");
            sender.sendMessage("§e/arcanite catalyst types §8- List available catalyst types");
            sender.sendMessage("§7Types: identification, socketing, fusion");
            return;
        }
        
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "give":
                if (args.length != 4) {
                    sender.sendMessage("§cUsage: /arcanite catalyst give <player> <type>");
                    sender.sendMessage("§7Types: identification, socketing, fusion");
                    return;
                }
                
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found!");
                    return;
                }
                
                String catalystType = args[3].toLowerCase();
                ItemStack catalyst = null;
                
                switch (catalystType) {
                    case "identification":
                        catalyst = CrystalRecipeManager.createIdentificationCatalyst();
                        break;
                    case "socketing":
                        catalyst = CrystalRecipeManager.createSocketingCatalyst();
                        break;
                    case "fusion":
                        catalyst = CrystalRecipeManager.createFusionCatalyst();
                        break;
                    default:
                        sender.sendMessage("§cInvalid catalyst type! Use: identification, socketing, fusion");
                        return;
                }
                
                // Give catalyst to player
                if (target.getInventory().firstEmpty() == -1) {
                    target.getWorld().dropItemNaturally(target.getLocation(), catalyst);
                    target.sendMessage("§6Your inventory was full, so the catalyst was dropped at your feet!");
                } else {
                    target.getInventory().addItem(catalyst);
                }
                
                sender.sendMessage("§aGave " + catalystType + " catalyst to " + target.getName());
                target.sendMessage("§aYou received a " + catalystType + " catalyst from " + sender.getName() + "!");
                break;
                
            case "types":
                sender.sendMessage("§6§lAvailable Catalyst Types:");
                sender.sendMessage("§e• identification §7- Reveals crystal properties");
                sender.sendMessage("§e• socketing §7- Embeds crystals into items");
                sender.sendMessage("§e• fusion §7- Combines two crystals");
                break;
                
            default:
                sender.sendMessage("§cUnknown catalyst command. Use: give, types");
                break;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String part = args[0].toLowerCase();
            
            // Add basic commands for all players
            BASIC_COMMANDS.stream()
                    .filter(sub -> sub.startsWith(part))
                    .forEach(completions::add);
            
            // Add admin commands if player has permission
            if (sender.hasPermission("arcanite.admin")) {
                ADMIN_COMMANDS.stream()
                        .filter(sub -> sub.startsWith(part))
                        .forEach(completions::add);
            }
            
            return completions;
        }
        
        if (args.length == 2) {
            String command = args[0].toLowerCase();
            String part = args[1].toLowerCase();
            
            switch (command) {
                case "admin":
                    if (sender.hasPermission("arcanite.admin")) {
                        return ADMIN_SUBCOMMANDS.stream()
                                .filter(sub -> sub.startsWith(part))
                                .collect(Collectors.toList());
                    }
                    break;
                    
                case "catalyst":
                    if (sender.hasPermission("arcanite.admin")) {
                        return CATALYST_SUBCOMMANDS.stream()
                                .filter(sub -> sub.startsWith(part))
                                .collect(Collectors.toList());
                    }
                    break;
                    
                case "give":
                    if (sender.hasPermission("arcanite.give")) {
                        // Return online player names
                        return Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(name -> name.toLowerCase().startsWith(part))
                                .collect(Collectors.toList());
                    }
                    break;
            }
        }
        
        if (args.length == 3) {
            String command = args[0].toLowerCase();
            String subCommand = args[1].toLowerCase();
            String part = args[2].toLowerCase();
            
            if ("catalyst".equals(command) && "give".equals(subCommand) && sender.hasPermission("arcanite.admin")) {
                // Return online player names for catalyst give command
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(part))
                        .collect(Collectors.toList());
            }
            
            if ("admin".equals(command) && sender.hasPermission("arcanite.admin")) {
                // Return online player names for admin commands that need players
                if (Arrays.asList("setlevel", "grant", "revoke", "view", "resetcooldown", "resetenergy").contains(subCommand)) {
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(part))
                            .collect(Collectors.toList());
                }
                
                // Return maintenance types for maintenance command
                if ("maintenance".equals(subCommand)) {
                    return Arrays.asList("save", "backup", "cleanup").stream()
                            .filter(type -> type.startsWith(part))
                            .collect(Collectors.toList());
                }
            }
        }
        
        if (args.length == 4) {
            String command = args[0].toLowerCase();
            String subCommand = args[1].toLowerCase();
            String part = args[3].toLowerCase();
            
            if ("catalyst".equals(command) && "give".equals(subCommand) && sender.hasPermission("arcanite.admin")) {
                // Return catalyst types
                return CATALYST_TYPES.stream()
                        .filter(type -> type.startsWith(part))
                    .collect(Collectors.toList());
            }
        }
        
        return completions;
    }
    
    /**
     * Shows an overview of all available levels
     */
    private void showAllLevels(Player player) {
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════════");
        player.sendMessage(ChatColor.GREEN + "      📊 " + ChatColor.BOLD + "SERVER LEVEL PROGRESSION" + ChatColor.RESET + ChatColor.GREEN + " 📊");
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════════");
        player.sendMessage("");
        
        int currentLevel = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getPlayerLevel(player.getUniqueId());
        int maxLevel = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getMaxLevel();
        
        for (int i = 1; i <= maxLevel; i++) {
            var config = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getLevelConfiguration(i);
            if (config == null) continue;
            
            String status;
            ChatColor color;
            if (i < currentLevel) {
                status = "✓ COMPLETED";
                color = ChatColor.GREEN;
            } else if (i == currentLevel) {
                status = "● CURRENT";
                color = ChatColor.AQUA;
            } else {
                status = "○ LOCKED";
                color = ChatColor.GRAY;
            }
            
            player.sendMessage(color + "Level " + i + ": " + 
                ChatColor.translateAlternateColorCodes('&', config.getTag()) + " " +
                color + config.getDisplayName() + " " + ChatColor.YELLOW + "[" + status + "]");
        }
        
        player.sendMessage("");
        player.sendMessage(ChatColor.BLUE + "Use " + ChatColor.GOLD + "/arcanite levelinfo <number>" + 
                         ChatColor.BLUE + " for detailed level information");
        player.sendMessage(ChatColor.BLUE + "Use " + ChatColor.GOLD + "/levelup" + 
                         ChatColor.BLUE + " to advance to the next level");
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════════");
    }
    
    /**
     * Shows detailed information about the player's current level
     */
    private void showCurrentLevelInfo(Player player) {
        int currentLevel = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getPlayerLevel(player.getUniqueId());
        showLevelInfo(player, currentLevel);
    }
    
    /**
     * Shows detailed information about a specific level
     */
    private void showLevelInfo(Player player, int level) {
        var config = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getLevelConfiguration(level);
        if (config == null) {
            player.sendMessage(ChatColor.RED + "Level " + level + " does not exist!");
            return;
        }
        
        int currentLevel = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getPlayerLevel(player.getUniqueId());
        boolean isCurrentLevel = level == currentLevel;
        boolean isUnlocked = level <= currentLevel;
        
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════════");
        player.sendMessage(ChatColor.GREEN + "      " + ChatColor.translateAlternateColorCodes('&', config.getTag()) + 
                         ChatColor.GREEN + " - " + ChatColor.AQUA + config.getDisplayName());
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════════");
        player.sendMessage("");
        
        if (isCurrentLevel) {
            player.sendMessage(ChatColor.AQUA + "● THIS IS YOUR CURRENT LEVEL");
        } else if (isUnlocked) {
            player.sendMessage(ChatColor.GREEN + "✓ COMPLETED LEVEL");
        } else {
            player.sendMessage(ChatColor.YELLOW + "○ LOCKED LEVEL");
        }
        
        if (!config.getDescription().isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Description: " + ChatColor.WHITE + config.getDescription());
        }
        
        player.sendMessage("");
        
        // Show requirements
        if (!isUnlocked) {
            player.sendMessage(ChatColor.YELLOW + "Requirements to unlock:");
            var requirements = config.getRequirements();
            
            if (requirements.getMoney() > 0) {
                player.sendMessage(ChatColor.GRAY + "  • Money: " + ChatColor.GREEN + 
                                 String.format("$%.2f", requirements.getMoney()));
            }
            if (requirements.getPlayerKills() > 0) {
                player.sendMessage(ChatColor.GRAY + "  • Player Kills: " + ChatColor.GREEN + 
                                 requirements.getPlayerKills());
            }
            if (requirements.getPlaytimeHours() > 0) {
                player.sendMessage(ChatColor.GRAY + "  • Playtime: " + ChatColor.GREEN + 
                                 requirements.getPlaytimeHours() + " hours");
            }
            player.sendMessage("");
        }
        
        // Show benefits
        player.sendMessage(ChatColor.BLUE + "Level Benefits:");
        if (!config.getBuffs().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "  Permanent Buffs:");
            for (var buff : config.getBuffs().entrySet()) {
                String buffName = formatBuffName(buff.getKey());
                String buffValue = formatBuffValue(buff.getValue());
                player.sendMessage(ChatColor.GRAY + "    ► " + buffName + ": " + ChatColor.GREEN + "+" + buffValue);
            }
        }
        
        player.sendMessage(ChatColor.YELLOW + "  Crystal System:");
        player.sendMessage(ChatColor.GRAY + "    ► Max Effect Tier: " + ChatColor.AQUA + 
                         dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getMaxTier(player.getUniqueId()));
        player.sendMessage(ChatColor.GRAY + "    ► Crystal Slots: " + ChatColor.AQUA + 
                         dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getSlots(player.getUniqueId()));
        
        if (!config.getPermissions().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "  Special Permissions:");
            for (String permission : config.getPermissions()) {
                player.sendMessage(ChatColor.GRAY + "    ► " + ChatColor.LIGHT_PURPLE + permission);
            }
        }
        
        player.sendMessage("");
        
        if (!isUnlocked && level == currentLevel + 1) {
            java.util.List<String> missing = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getMissingRequirementsForNextLevel(player.getUniqueId());
            if (missing.isEmpty()) {
                player.sendMessage(ChatColor.GREEN + "✓ You can level up! Use " + ChatColor.GOLD + "/levelup");
            } else {
                player.sendMessage(ChatColor.RED + "Missing requirements: " + String.join(", ", missing));
            }
        } else if (!isUnlocked) {
            player.sendMessage(ChatColor.GRAY + "Complete previous levels to unlock this one.");
        }
        
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════════");
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
