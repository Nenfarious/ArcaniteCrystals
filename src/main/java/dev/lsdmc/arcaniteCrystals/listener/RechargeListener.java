// src/main/java/dev/lsdmc/arcaniteCrystals/listener/RechargeListener.java
package dev.lsdmc.arcaniteCrystals.listener;

import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles crystal recharging mechanics using quartz and other recharge materials.
 */
public class RechargeListener implements Listener {

    /**
     * Handles crystal recharging when player right-clicks with quartz on a depleted crystal.
     */
    @EventHandler
    public void onCrystalRecharge(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null) return;
        
        // Check if player is holding quartz or recharge material
        String rechargeMaterialName = ConfigManager.getConfig().getString("recharge.material", "QUARTZ");
        Material rechargeMaterial = Material.matchMaterial(rechargeMaterialName);
        if (rechargeMaterial == null) rechargeMaterial = Material.QUARTZ;
        
        if (item.getType() != rechargeMaterial) return;
        
        // Check if right-click action
        if (!event.getAction().name().contains("RIGHT_CLICK")) return;
        
        // Look for a depleted crystal in inventory
        ItemStack crystalToRecharge = findDepletedCrystal(player);
        if (crystalToRecharge == null) {
            player.sendMessage(ChatColor.RED + "No depleted crystals found in your inventory!");
            return;
        }
        
        event.setCancelled(true);
        
        // Attempt to recharge the crystal
        if (CrystalManager.rechargeCrystal(player, crystalToRecharge, item)) {
            player.sendMessage(ChatColor.GREEN + "Crystal recharged successfully!");
        }
    }
    
    /**
     * Finds the first depleted crystal in player's inventory.
     */
    private ItemStack findDepletedCrystal(Player player) {
        // Check main hand first
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (CrystalManager.isRechargeable(mainHand)) {
            return mainHand;
        }
        
        // Check off hand
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (CrystalManager.isRechargeable(offHand)) {
            return offHand;
        }
        
        // Check entire inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (CrystalManager.isRechargeable(item)) {
                return item;
            }
        }
        
        return null;
    }
}
