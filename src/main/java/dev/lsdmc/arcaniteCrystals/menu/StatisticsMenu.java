package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.manager.PlayerStatisticsManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class StatisticsMenu {
    private static final String TITLE = "Crystal Statistics";
    private static final int SIZE = 54;
    
    public static void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, MessageManager.colorize(TITLE));
        
        // Get player stats
        PlayerStatisticsManager.PlayerStats stats = PlayerStatisticsManager.getInstance().getStats(player.getUniqueId());
        
        // Crystal Activation Stats
        inv.setItem(10, createStatItem(Material.AMETHYST_SHARD, "Crystal Activations",
            "Total: " + stats.crystalsActivated.get()));
        
        // Fusion Stats
        inv.setItem(12, createStatItem(Material.NETHER_STAR, "Crystal Fusions",
            "Successful: " + stats.successfulFusions.get(),
            "Failed: " + stats.failedFusions.get()));
        
        // Socket Stats
        inv.setItem(14, createStatItem(Material.END_CRYSTAL, "Crystal Sockets",
            "Successful: " + stats.successfulSockets.get(),
            "Failed: " + stats.failedSockets.get()));
        
        // Energy Stats
        inv.setItem(16, createStatItem(Material.GLOWSTONE_DUST, "Energy Usage",
            "Total: " + stats.totalEnergyUsed.get()));
        
        // Decay Stats
        inv.setItem(28, createStatItem(Material.SOUL_SAND, "Crystal Decay",
            "Decayed: " + stats.crystalsDecayed.get(),
            "Corrupted: " + stats.crystalsCorrupted.get()));
        
        // Identification Stats
        inv.setItem(30, createStatItem(Material.SPYGLASS, "Crystal Identification",
            "Successful: " + stats.successfulIdentifications.get(),
            "Failed: " + stats.failedIdentifications.get()));
        
        // Fill empty slots with glass panes
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        
        for (int i = 0; i < SIZE; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
        
        player.openInventory(inv);
    }
    
    private static ItemStack createStatItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageManager.colorize("&b" + name));
        
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(MessageManager.colorize("&7" + line));
        }
        meta.setLore(coloredLore);
        
        item.setItemMeta(meta);
        return item;
    }
} 