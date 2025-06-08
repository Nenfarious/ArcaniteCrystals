package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
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

public class CrystalIdentificationGUI implements InventoryHolder, Listener {
    private static final int CRYSTAL_SLOT = 20;
    private static final int IDENTIFY_SLOT = 40;
    private static final int BACK_SLOT = 49;
    
    private final Player player;
    private final Inventory inventory;
    private boolean isOpen = false;

    public CrystalIdentificationGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, 
            ChatColor.DARK_PURPLE + "✦ Crystal Identification ✦");
        buildIdentificationInterface();
    }

    private void buildIdentificationInterface() {
        // Add crystal slot
        ItemStack crystalSlot = createSlotItem(Material.GRAY_STAINED_GLASS_PANE, "Place Crystal");
        inventory.setItem(CRYSTAL_SLOT, crystalSlot);
        
        // Add identify button
        ItemStack identifyBtn = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta identifyMeta = identifyBtn.getItemMeta();
        identifyMeta.setDisplayName(ChatColor.GREEN + "Identify Crystal");
        List<String> identifyLore = new ArrayList<>();
        identifyLore.add(ChatColor.GRAY + "Click to identify the crystal");
        identifyMeta.setLore(identifyLore);
        identifyBtn.setItemMeta(identifyMeta);
        inventory.setItem(IDENTIFY_SLOT, identifyBtn);
        
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
            case IDENTIFY_SLOT -> {
                ItemStack crystal = inventory.getItem(CRYSTAL_SLOT);
                if (crystal != null && CrystalManager.isCrystal(crystal)) {
                    if (!CrystalManager.isIdentified(crystal)) {
                        List<String> effects = CrystalManager.generateRandomEffects(clicker, 
                            PlayerDataManager.getUnlockedUpgrades(clicker.getUniqueId()), crystal);
                        CrystalManager.setCrystalEffects(crystal, effects);
                        inventory.setItem(CRYSTAL_SLOT, crystal);
                        clicker.sendMessage(ChatColor.GREEN + "Crystal identified successfully!");
                    } else {
                        clicker.sendMessage(ChatColor.RED + "This crystal is already identified!");
                    }
                } else {
                    clicker.sendMessage(ChatColor.RED + "Please place a crystal to identify!");
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