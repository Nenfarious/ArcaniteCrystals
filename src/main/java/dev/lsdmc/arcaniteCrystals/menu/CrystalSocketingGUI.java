package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.manager.CrystalSocketManager;
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

public class CrystalSocketingGUI implements InventoryHolder, Listener {
    private static final int ITEM_SLOT = 20;
    private static final int CRYSTAL_SLOT = 24;
    private static final int SOCKET_SLOT = 40;
    private static final int BACK_SLOT = 49;
    
    private final Player player;
    private final Inventory inventory;
    private boolean isOpen = false;

    public CrystalSocketingGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, 
            ChatColor.DARK_PURPLE + "✦ Crystal Socketing ✦");
        buildSocketingInterface();
    }

    private void buildSocketingInterface() {
        // Add item and crystal slots
        ItemStack itemSlot = createSlotItem(Material.GRAY_STAINED_GLASS_PANE, "Place Item");
        ItemStack crystalSlot = createSlotItem(Material.GRAY_STAINED_GLASS_PANE, "Place Crystal");
        inventory.setItem(ITEM_SLOT, itemSlot);
        inventory.setItem(CRYSTAL_SLOT, crystalSlot);
        
        // Add socket button
        ItemStack socketBtn = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta socketMeta = socketBtn.getItemMeta();
        socketMeta.setDisplayName(ChatColor.GREEN + "Socket Crystal");
        List<String> socketLore = new ArrayList<>();
        socketLore.add(ChatColor.GRAY + "Click to socket the crystal");
        socketMeta.setLore(socketLore);
        socketBtn.setItemMeta(socketMeta);
        inventory.setItem(SOCKET_SLOT, socketBtn);
        
        // Add back button
        ItemStack backBtn = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backBtn.getItemMeta();
        backMeta.setDisplayName(ChatColor.YELLOW + "Back");
        backBtn.setItemMeta(backMeta);
        inventory.setItem(BACK_SLOT, backBtn);
    }

    private ItemStack createSlotItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + name);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);
        
        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        switch (slot) {
            case SOCKET_SLOT -> {
                ItemStack item = inventory.getItem(ITEM_SLOT);
                ItemStack crystal = inventory.getItem(CRYSTAL_SLOT);
                
                if (item != null && crystal != null) {
                    if (CrystalSocketManager.socketCrystal(clicker, item, crystal)) {
                        inventory.setItem(CRYSTAL_SLOT, null);
                    }
                } else {
                    clicker.sendMessage(ChatColor.RED + "Please place an item and a crystal!");
                }
            }
            case BACK_SLOT -> {
                clicker.closeInventory();
                new EnhancementWorkshopGUI(clicker).open();
            }
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