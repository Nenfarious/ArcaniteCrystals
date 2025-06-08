package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class HelpSystemGUI implements InventoryHolder, Listener {
    private static final int BASICS_SLOT = 11;
    private static final int CRYSTALS_SLOT = 13;
    private static final int UPGRADES_SLOT = 15;
    private static final int CLOSE_SLOT = 49;
    
    private final Player player;
    private final Inventory inventory;
    private boolean isOpen = false;

    public HelpSystemGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, 
            ChatColor.DARK_PURPLE + "✦ Help & Guides ✦");
        buildHelpMenu();
    }

    private void buildHelpMenu() {
        // Basics guide
        ItemStack basicsItem = new ItemStack(Material.BOOK);
        ItemMeta basicsMeta = basicsItem.getItemMeta();
        basicsMeta.setDisplayName(ChatColor.AQUA + "Getting Started");
        List<String> basicsLore = new ArrayList<>();
        basicsLore.add(ChatColor.GRAY + "Learn the basics of");
        basicsLore.add(ChatColor.GRAY + "Arcanite Crystals");
        basicsMeta.setLore(basicsLore);
        basicsItem.setItemMeta(basicsMeta);
        inventory.setItem(BASICS_SLOT, basicsItem);
        
        // Crystal guide
        ItemStack crystalsItem = new ItemStack(Material.DIAMOND);
        ItemMeta crystalsMeta = crystalsItem.getItemMeta();
        crystalsMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Crystal Guide");
        List<String> crystalsLore = new ArrayList<>();
        crystalsLore.add(ChatColor.GRAY + "Learn about different");
        crystalsLore.add(ChatColor.GRAY + "types of crystals");
        crystalsMeta.setLore(crystalsLore);
        crystalsItem.setItemMeta(crystalsMeta);
        inventory.setItem(CRYSTALS_SLOT, crystalsItem);
        
        // Upgrades guide
        ItemStack upgradesItem = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta upgradesMeta = upgradesItem.getItemMeta();
        upgradesMeta.setDisplayName(ChatColor.GOLD + "Upgrades Guide");
        List<String> upgradesLore = new ArrayList<>();
        upgradesLore.add(ChatColor.GRAY + "Learn about crystal");
        upgradesLore.add(ChatColor.GRAY + "enhancements and upgrades");
        upgradesMeta.setLore(upgradesLore);
        upgradesItem.setItemMeta(upgradesMeta);
        inventory.setItem(UPGRADES_SLOT, upgradesItem);
        
        // Close button
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Close");
        closeBtn.setItemMeta(closeMeta);
        inventory.setItem(CLOSE_SLOT, closeBtn);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);
        
        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        switch (slot) {
            case BASICS_SLOT -> {
                clicker.closeInventory();
                clicker.sendMessage(ChatColor.AQUA + "=== Getting Started ===");
                clicker.sendMessage(ChatColor.GRAY + "• Use /arcanite to open the main menu");
                clicker.sendMessage(ChatColor.GRAY + "• Collect crystals from various sources");
                clicker.sendMessage(ChatColor.GRAY + "• Level up to unlock new features");
            }
            case CRYSTALS_SLOT -> {
                clicker.closeInventory();
                clicker.sendMessage(ChatColor.LIGHT_PURPLE + "=== Crystal Guide ===");
                clicker.sendMessage(ChatColor.GRAY + "• Basic crystals can be enhanced");
                clicker.sendMessage(ChatColor.GRAY + "• Higher tier crystals have better effects");
                clicker.sendMessage(ChatColor.GRAY + "• Use the workshop to upgrade crystals");
            }
            case UPGRADES_SLOT -> {
                clicker.closeInventory();
                clicker.sendMessage(ChatColor.GOLD + "=== Upgrades Guide ===");
                clicker.sendMessage(ChatColor.GRAY + "• Purchase upgrades in the talents menu");
                clicker.sendMessage(ChatColor.GRAY + "• Higher tiers unlock better upgrades");
                clicker.sendMessage(ChatColor.GRAY + "• Some upgrades require specific levels");
            }
            case CLOSE_SLOT -> clicker.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this && isOpen) {
            isOpen = false;
            HandlerList.unregisterAll(this);
        }
    }

    public void open() {
        if (!isOpen) {
            player.openInventory(inventory);
            isOpen = true;
            Bukkit.getPluginManager().registerEvents(this, ArcaniteCrystals.getInstance());
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
} 