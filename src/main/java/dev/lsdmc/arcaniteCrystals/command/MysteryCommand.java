package dev.lsdmc.arcaniteCrystals.command;

import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * Command to give mystery crystals to players with effects based on their unlocked upgrades.
 */
public class MysteryCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageManager.get("error.playersOnly"));
            return true;
        }

        Player player = (Player) sender;

        if (!sender.hasPermission("arcanite.mystery")) {
            sender.sendMessage(MessageManager.get("error.noPermission"));
            return true;
        }

        // Check if player has any unlocked upgrades
        Set<String> unlockedUpgrades = PlayerDataManager.getUnlockedUpgrades(player.getUniqueId());
        if (unlockedUpgrades.isEmpty()) {
            player.sendMessage("§cYou don't have any unlocked upgrades yet!");
            player.sendMessage("§7Use §e/arcanite talents §7to unlock upgrades first, then get mystery crystals.");
            return true;
        }

        // Create mystery crystal based on player's upgrades
        ItemStack mysteryCrystal = CrystalManager.createMysteryCrystal(player);

        // Check if player has inventory space
        if (player.getInventory().firstEmpty() == -1) {
            // Inventory is full, drop at player's location
            player.getWorld().dropItemNaturally(player.getLocation(), mysteryCrystal);
            player.sendMessage("§6Your inventory was full, so the crystal was dropped at your feet!");
        } else {
            // Add to inventory
            player.getInventory().addItem(mysteryCrystal);
        }

        // Success feedback
        player.sendMessage("§6You received a Mystery Arcanite Crystal!");
        player.sendMessage("§7It contains random effects from your unlocked upgrades!");
        player.sendMessage("§7Hold it in your off-hand to activate its powers!");
        
        // Notification
        MessageManager.sendNotification(player, "You received a Mystery Crystal!", MessageManager.NotificationType.SUCCESS);

        return true;
    }
} 