package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
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

public class EnhancementWorkshopGUI implements InventoryHolder, Listener {
    private static final int FUSION_SLOT = 11;
    private static final int SOCKET_SLOT = 13;
    private static final int IDENTIFY_SLOT = 15;
    private static final int CLOSE_SLOT = 49;
    
    private final Player player;
    private final Inventory inventory;
    private boolean isOpen = false;

    public EnhancementWorkshopGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, 
            ChatColor.DARK_PURPLE + "✦ Enhancement Workshop ✦");
        buildWorkshop();
    }

    private void buildWorkshop() {
        // Add fusion option
        ItemStack fusionItem = new ItemStack(Material.ANVIL);
        ItemMeta fusionMeta = fusionItem.getItemMeta();
        fusionMeta.setDisplayName(ChatColor.AQUA + "Crystal Fusion");
        List<String> fusionLore = new ArrayList<>();
        fusionLore.add(ChatColor.GRAY + "Combine two crystals to create");
        fusionLore.add(ChatColor.GRAY + "a more powerful one");
        fusionMeta.setLore(fusionLore);
        fusionItem.setItemMeta(fusionMeta);
        inventory.setItem(FUSION_SLOT, fusionItem);
        
        // Add socket option
        ItemStack socketItem = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta socketMeta = socketItem.getItemMeta();
        socketMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Crystal Socketing");
        List<String> socketLore = new ArrayList<>();
        socketLore.add(ChatColor.GRAY + "Add effects to your crystals");
        socketLore.add(ChatColor.GRAY + "using enhancement gems");
        socketMeta.setLore(socketLore);
        socketItem.setItemMeta(socketMeta);
        inventory.setItem(SOCKET_SLOT, socketItem);
        
        // Add identification option
        ItemStack identifyItem = new ItemStack(Material.SPYGLASS);
        ItemMeta identifyMeta = identifyItem.getItemMeta();
        identifyMeta.setDisplayName(ChatColor.GOLD + "Crystal Identification");
        List<String> identifyLore = new ArrayList<>();
        identifyLore.add(ChatColor.GRAY + "Identify unknown crystals");
        identifyLore.add(ChatColor.GRAY + "to reveal their effects");
        identifyMeta.setLore(identifyLore);
        identifyItem.setItemMeta(identifyMeta);
        inventory.setItem(IDENTIFY_SLOT, identifyItem);
        
        // Add close button
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Close");
        closeBtn.setItemMeta(closeMeta);
        inventory.setItem(CLOSE_SLOT, closeBtn);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        
        event.setCancelled(true); // Cancel all clicks by default
        
        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        switch (slot) {
            case IDENTIFY_SLOT: // Crystal Identification
                clicker.closeInventory();
                new CrystalIdentificationGUI(clicker).open();
                break;
                
            case SOCKET_SLOT: // Crystal Socketing
                clicker.closeInventory();
                new CrystalSocketingGUI(clicker).open();
                break;
                
            case FUSION_SLOT: // Crystal Fusion
                clicker.closeInventory();
                new CrystalFusionGUI(clicker).open();
                break;
                
            case 16: // Crystal Enhancement (future feature)
                MessageManager.sendNotification(clicker, "Crystal Enhancement coming soon!", MessageManager.NotificationType.INFO);
                break;
                
            case CLOSE_SLOT: // Close button
                clicker.closeInventory();
                break;
                
            default:
                // All other clicks are cancelled (including filler items)
                break;
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