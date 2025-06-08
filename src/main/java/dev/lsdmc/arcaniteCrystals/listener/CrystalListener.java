package dev.lsdmc.arcaniteCrystals.listener;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles crystal-related events including activation, deactivation, and cleanup.
 */
public class CrystalListener implements Listener {
    private final Set<UUID> activatingPlayers = ConcurrentHashMap.newKeySet();
    
    public CrystalListener() {
        // Initialize listener
    }
    
    @EventHandler
    public void onCrystalInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || !CrystalManager.isCrystal(item)) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        // Prevent concurrent activation
        if (!activatingPlayers.add(playerId)) {
            return;
        }
        
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
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        CrystalManager.handlePlayerDisconnect(player);
    }
    
    private void handleCrystalIdentification(Player player, ItemStack crystal) {
        // Create a new crystal with random effects
        ItemStack identifiedCrystal = CrystalManager.createMysteryCrystal(player);
        if (identifiedCrystal == null) {
            player.sendMessage(ChatColor.RED + "Failed to identify crystal!");
            return;
        }
        
        // Replace the blank crystal with the identified one
        player.getInventory().setItemInMainHand(identifiedCrystal);
        
        player.sendMessage(ChatColor.GREEN + "Crystal identified! Right-click again to activate.");
        SoundManager.playCrystalActivateSound(player);
        ParticleManager.playCrystalActivationEffect(player);
    }
    
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return minutes + "m " + seconds + "s";
    }
} 