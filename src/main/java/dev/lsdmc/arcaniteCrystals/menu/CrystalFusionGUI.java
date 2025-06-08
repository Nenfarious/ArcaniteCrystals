package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.manager.CrystalFusionManager;
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

public class CrystalFusionGUI implements InventoryHolder, Listener {
    private static final int CRYSTAL_1_SLOT = 20;
    private static final int CRYSTAL_2_SLOT = 24;
    private static final int FUSE_SLOT = 40;
    private static final int BACK_SLOT = 49;
    
    private final Player player;
    private final Inventory inventory;
    private boolean isOpen = false;

    public CrystalFusionGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, 
            ChatColor.DARK_PURPLE + "✦ Crystal Fusion ✦");
        buildFusionInterface();
    }

    private void buildFusionInterface() {
        // Add crystal slots
        ItemStack crystal1Slot = createSlotItem(Material.GRAY_STAINED_GLASS_PANE, "Place Crystal 1");
        ItemStack crystal2Slot = createSlotItem(Material.GRAY_STAINED_GLASS_PANE, "Place Crystal 2");
        inventory.setItem(CRYSTAL_1_SLOT, crystal1Slot);
        inventory.setItem(CRYSTAL_2_SLOT, crystal2Slot);
        
        // Add fuse button
        ItemStack fuseBtn = new ItemStack(Material.ANVIL);
        ItemMeta fuseMeta = fuseBtn.getItemMeta();
        fuseMeta.setDisplayName(ChatColor.GREEN + "Fuse Crystals");
        List<String> fuseLore = new ArrayList<>();
        fuseLore.add(ChatColor.GRAY + "Click to fuse the crystals");
        fuseMeta.setLore(fuseLore);
        fuseBtn.setItemMeta(fuseMeta);
        inventory.setItem(FUSE_SLOT, fuseBtn);
        
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
            case FUSE_SLOT -> {
                ItemStack crystal1 = inventory.getItem(CRYSTAL_1_SLOT);
                ItemStack crystal2 = inventory.getItem(CRYSTAL_2_SLOT);
                
                if (crystal1 != null && crystal2 != null) {
                    if (CrystalFusionManager.fuseCrystals(clicker, crystal1, crystal2)) {
                        inventory.setItem(CRYSTAL_1_SLOT, null);
                        inventory.setItem(CRYSTAL_2_SLOT, null);
                    }
                } else {
                    clicker.sendMessage(ChatColor.RED + "Please place two crystals to fuse!");
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