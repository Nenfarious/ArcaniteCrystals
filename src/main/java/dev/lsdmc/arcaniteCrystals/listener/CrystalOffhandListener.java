package dev.lsdmc.arcaniteCrystals.listener;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dedicated listener for handling crystal effects when crystals are placed in offhand.
 * This ensures effects only work when crystals are properly equipped in the offhand slot.
 */
public class CrystalOffhandListener implements Listener {

    private static final Set<UUID> processingPlayers = ConcurrentHashMap.newKeySet();

    /**
     * Handles when player swaps items to/from offhand
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHandSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Prevent concurrent processing
        if (processingPlayers.contains(playerId)) {
            return;
        }

        processingPlayers.add(playerId);

        // Small delay to ensure inventory is properly updated
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    processOffhandChange(player);
                } finally {
                    processingPlayers.remove(playerId);
                }
            }
        }.runTaskLater(ArcaniteCrystals.getInstance(), 2L);
    }

    /**
     * Handles inventory clicks that might affect offhand
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Check if offhand slot was affected
        if (event.getSlot() == 40 || event.isShiftClick()) {
            UUID playerId = player.getUniqueId();

            // Prevent concurrent processing
            if (processingPlayers.contains(playerId)) {
                return;
            }

            processingPlayers.add(playerId);

            // Delay to ensure inventory update
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        processOffhandChange(player);
                    } finally {
                        processingPlayers.remove(playerId);
                    }
                }
            }.runTaskLater(ArcaniteCrystals.getInstance(), 2L);
        }
    }

    /**
     * Handles when player drops items (might be from offhand)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Check if a crystal was dropped from offhand
        if (CrystalManager.isCrystal(event.getItemDrop().getItemStack())) {
            processingPlayers.add(playerId);

            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        processOffhandChange(player);
                    } finally {
                        processingPlayers.remove(playerId);
                    }
                }
            }.runTaskLater(ArcaniteCrystals.getInstance(), 2L);
        }
    }

    /**
     * Clean up when player disconnects
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        CrystalManager.stopCrystalEffects(player);
        processingPlayers.remove(player.getUniqueId());
    }

    /**
     * Process offhand changes and manage crystal effects
     */
    private void processOffhandChange(Player player) {
        if (!player.isOnline()) return;

        ItemStack offhandItem = player.getInventory().getItemInOffHand();
        boolean hasActiveCrystal = CrystalManager.hasActiveCrystal(player);

        // If player has a crystal in offhand
        if (offhandItem != null && CrystalManager.isCrystal(offhandItem)) {
            if (!hasActiveCrystal) {
                // Try to start crystal effects
                startCrystalEffects(player, offhandItem);
            }
        } else {
            // No crystal in offhand, stop effects if any
            if (hasActiveCrystal) {
                CrystalManager.stopCrystalEffects(player);
            }
        }
    }

    /**
     * Attempt to start crystal effects with validation
     */
    private void startCrystalEffects(Player player, ItemStack crystal) {
        try {
            // Check if crystal is identified
            if (CrystalManager.getCrystalEffects(crystal).isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "⚠ This crystal is unidentified!");
                player.sendMessage(ChatColor.GRAY + "Use an Identification Station to reveal its effects.");
                return;
            }

            // Check if crystal is activated
            if (!CrystalManager.isActivatedCrystal(crystal)) {
                player.sendMessage(ChatColor.YELLOW + "⚠ This crystal must be activated first!");
                player.sendMessage(ChatColor.GRAY + "Right-click the crystal to activate it.");
                return;
            }

            // Check if crystal has energy
            if (CrystalManager.getEnergy(crystal) <= 0) {
                player.sendMessage(ChatColor.RED + "⚠ This crystal is depleted!");
                player.sendMessage(ChatColor.GRAY + "Use quartz to recharge it.");
                return;
            }

            // Start the effects
            if (CrystalManager.startCrystalEffects(player, crystal)) {
                // Success feedback
                MessageManager.sendNotification(player, 
                    "✨ Crystal effects activated!\nHold in offhand to maintain effects.", 
                    MessageManager.NotificationType.SUCCESS);
            }
        } catch (Exception e) {
            ArcaniteCrystals.getInstance().getLogger().warning(
                "Error starting crystal effects for " + player.getName() + ": " + e.getMessage());
        }
    }
} 