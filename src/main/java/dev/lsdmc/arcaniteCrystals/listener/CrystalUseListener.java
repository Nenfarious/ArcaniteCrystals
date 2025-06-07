// src/main/java/dev/lsdmc/arcaniteCrystals/listener/CrystalUseListener.java
package dev.lsdmc.arcaniteCrystals.listener;

import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified crystal usage handler with proper race condition prevention,
 * cooldown management, and consolidated activation logic.
 */
public class CrystalUseListener implements Listener {

    // Track players currently activating crystals to prevent race conditions
    private static final Set<UUID> activatingPlayers = ConcurrentHashMap.newKeySet();

    /**
     * Handles crystal activation when right-clicked with proper synchronization.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCrystalUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || !CrystalManager.isCrystal(item)) return;
        
        // Check if right-click action
        if (!event.getAction().name().contains("RIGHT_CLICK")) return;
        
        event.setCancelled(true);
        
        // Prevent concurrent activation attempts
        UUID playerId = player.getUniqueId();
        if (activatingPlayers.contains(playerId)) {
            player.sendMessage(ChatColor.YELLOW + "Please wait, processing crystal activation...");
            return;
        }
        
        // Lock activation for this player
        activatingPlayers.add(playerId);
        
        try {
            // Handle crystal identification for blank crystals
            if (CrystalManager.getCrystalEffects(item).isEmpty()) {
                handleCrystalIdentification(player, item);
                return;
            }
            
            // Check cooldown before activation
            if (CrystalManager.isOnCooldown(player)) {
                long remaining = CrystalManager.getRemainingCooldown(player);
                player.sendMessage(ChatColor.RED + "Crystal is on cooldown for " + formatTime(remaining / 1000) + "!");
                return;
            }
            
            // Handle crystal activation
            if (CrystalManager.activateCrystal(player, item)) {
                player.sendMessage(ChatColor.GREEN + "Crystal activated! Hold in off-hand to use effects.");
                MessageManager.sendNotification(player, "Crystal activated successfully!", MessageManager.NotificationType.CRYSTAL_ACTIVATE);
            }
        } finally {
            // Always release lock
            activatingPlayers.remove(playerId);
        }
    }
    
    /**
     * Handles crystal identification for blank crystals.
     */
    private void handleCrystalIdentification(Player player, ItemStack crystal) {
        // Transform blank crystal into mystery crystal
        ItemStack mysteryCrystal = CrystalManager.createMysteryCrystal(player);
        
        // Replace the crystal in player's hand
        if (player.getInventory().getItemInMainHand().equals(crystal)) {
            player.getInventory().setItemInMainHand(mysteryCrystal);
        } else if (player.getInventory().getItemInOffHand().equals(crystal)) {
            player.getInventory().setItemInOffHand(mysteryCrystal);
        }
        
        player.sendMessage(ChatColor.GREEN + "Crystal identified! Effects have been revealed.");
        MessageManager.sendNotification(player, "Crystal successfully identified!", MessageManager.NotificationType.SUCCESS);
    }
    
    /**
     * Handles automatic crystal activation when moved to off-hand with cooldown checks.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onHandSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack offhandItem = event.getOffHandItem();
        
        // Check if crystal is being moved to off-hand
        if (offhandItem != null && CrystalManager.isCrystal(offhandItem)) {
            // Check cooldown first
            if (CrystalManager.isOnCooldown(player)) {
                long remaining = CrystalManager.getRemainingCooldown(player);
                player.sendMessage(ChatColor.RED + "Crystal is on cooldown for " + formatTime(remaining / 1000) + "!");
                event.setCancelled(true);
                return;
            }
            
            handleCrystalEquip(player, offhandItem);
        }
        
        // Check if crystal is being removed from off-hand
        ItemStack mainhandItem = event.getMainHandItem();
        if (mainhandItem != null && CrystalManager.isCrystal(mainhandItem)) {
            handleCrystalUnequip(player);
        }
    }
    
    /**
     * Handles crystal changes in off-hand slot with proper synchronization.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        
        // Small delay to ensure inventory is updated
        dev.lsdmc.arcaniteCrystals.ArcaniteCrystals.getInstance().getServer().getScheduler().runTaskLater(
            dev.lsdmc.arcaniteCrystals.ArcaniteCrystals.getInstance(), () -> {
            
            // Check if off-hand crystal state changed
            ItemStack offhandItem = player.getInventory().getItemInOffHand();
            if (CrystalManager.isCrystal(offhandItem)) {
                if (!CrystalManager.hasActiveCrystal(player)) {
                    // Check cooldown before auto-activation
                    if (!CrystalManager.isOnCooldown(player)) {
                        handleCrystalEquip(player, offhandItem);
                    }
                }
            } else if (CrystalManager.hasActiveCrystal(player)) {
                handleCrystalUnequip(player);
            }
            
        }, 1L);
    }
    
    /**
     * Handles crystal being equipped in off-hand with comprehensive validation.
     */
    private void handleCrystalEquip(Player player, ItemStack crystal) {
        if (!CrystalManager.isCrystal(crystal)) return;
        
        UUID playerId = player.getUniqueId();
        
        // Prevent concurrent equipping
        if (activatingPlayers.contains(playerId)) {
            return;
        }
        
        activatingPlayers.add(playerId);
        
        try {
            // Check if crystal has effects
            if (CrystalManager.getCrystalEffects(crystal).isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "This crystal is unidentified. Right-click to reveal its effects!");
                return;
            }
            
            // Check if crystal has energy
            if (CrystalManager.getEnergy(crystal) <= 0) {
                player.sendMessage(ChatColor.RED + "This crystal is depleted. Use quartz to recharge it!");
                return;
            }
            
            // Check cooldown (double-check for safety)
            if (CrystalManager.isOnCooldown(player)) {
                long remaining = CrystalManager.getRemainingCooldown(player);
                player.sendMessage(ChatColor.RED + "Crystal is on cooldown for " + formatTime(remaining / 1000) + "!");
                return;
            }
            
            // Activate crystal automatically when equipped
            if (CrystalManager.activateCrystal(player, crystal)) {
                MessageManager.sendNotification(player, "Crystal equipped and activated!", MessageManager.NotificationType.CRYSTAL_ACTIVATE);
            }
        } finally {
            activatingPlayers.remove(playerId);
        }
    }
    
    /**
     * Handles crystal being unequipped from off-hand.
     */
    private void handleCrystalUnequip(Player player) {
        if (CrystalManager.hasActiveCrystal(player)) {
            CrystalManager.deactivateCrystal(player);
            player.sendMessage(ChatColor.GRAY + "Crystal effects deactivated.");
            
            // Synchronize crystal state to prevent desync
            CrystalManager.synchronizeCrystalState(player);
        }
    }
    
    /**
     * Formats time in seconds to readable format.
     */
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }
}
