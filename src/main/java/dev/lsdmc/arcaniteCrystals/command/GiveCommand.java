// src/main/java/dev/lsdmc/arcaniteCrystals/command/GiveCommand.java
package dev.lsdmc.arcaniteCrystals.command;

import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Command to give blank Arcanite Crystals to players with proper security and validation.
 */
public class GiveCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("arcanite.give")) {
            sender.sendMessage(MessageManager.get("error.noPermission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(MessageManager.get("usage.give"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(MessageManager.get("error.playerNotFound"));
            return true;
        }

        // Create a blank crystal using CrystalManager
        ItemStack blankCrystal = CrystalManager.createBlankCrystal();
        
        // Check if player has inventory space
        if (target.getInventory().firstEmpty() == -1) {
            // Inventory is full, drop at player's location
            target.getWorld().dropItemNaturally(target.getLocation(), blankCrystal);
            target.sendMessage("§6Your inventory was full, so the crystal was dropped at your feet!");
        } else {
            // Add to inventory
            target.getInventory().addItem(blankCrystal);
        }

        // Success messages
        String successMessage = MessageManager.getMessage("success.give", "player", target.getName());
        sender.sendMessage(successMessage);
        
        target.sendMessage("§6You received a blank Arcanite Crystal!");
        target.sendMessage("§7Right-click to identify it and reveal its powers!");
        
        // Notification for target
        MessageManager.sendNotification(target, "You received an Arcanite Crystal!", MessageManager.NotificationType.SUCCESS);

        return true;
    }
}
