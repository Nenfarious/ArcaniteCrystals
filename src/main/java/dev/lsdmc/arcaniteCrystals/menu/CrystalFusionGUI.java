package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.manager.CrystalFusionManager;
import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import dev.lsdmc.arcaniteCrystals.manager.CrystalRecipeManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
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

/**
 * Workshop for fusing two crystals into one using Fusion Catalysts.
 * Uses a 4-slot system: Crystal 1 + Crystal 2 + Catalyst = Fused Crystal
 */
public class CrystalFusionGUI implements InventoryHolder, Listener {
    private final Player player;
    private final Inventory inventory;
    private boolean isOpen = false;

    // Slot positions
    private static final int CRYSTAL1_SLOT = 9;
    private static final int CRYSTAL2_SLOT = 11;
    private static final int CATALYST_SLOT = 13;
    private static final int RESULT_SLOT = 15;
    private static final int PROCESS_BUTTON = 22;

    public CrystalFusionGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.LIGHT_PURPLE + "Crystal Fusion Workshop");
        setupGUI();
        Bukkit.getPluginManager().registerEvents(this, ArcaniteCrystals.getInstance());
    }

    private void setupGUI() {
        // Fill with glass panes first
        fillEmptySlots(Material.MAGENTA_STAINED_GLASS_PANE);
        
        // Clear the working slots
        inventory.setItem(CRYSTAL1_SLOT, null);
        inventory.setItem(CRYSTAL2_SLOT, null);
        inventory.setItem(CATALYST_SLOT, null);
        inventory.setItem(RESULT_SLOT, null);
        
        // Tutorial book
        ItemStack tutorial = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta tutorialMeta = tutorial.getItemMeta();
        tutorialMeta.setDisplayName(ChatColor.GREEN + "📖 How to Fuse Crystals");
        List<String> tutorialLore = new ArrayList<>();
        tutorialLore.add(ChatColor.GRAY + "Step 1: Place first identified crystal");
        tutorialLore.add(ChatColor.GRAY + "Step 2: Place second identified crystal");
        tutorialLore.add(ChatColor.GRAY + "Step 3: Add a Fusion Catalyst");
        tutorialLore.add(ChatColor.GRAY + "Step 4: Click the Process button");
        tutorialLore.add("");
        tutorialLore.add(ChatColor.YELLOW + "💡 Tip: Only identified crystals can be fused!");
        tutorialLore.add(ChatColor.YELLOW + "💡 Effects will be combined into one crystal");
        tutorialLore.add(ChatColor.RED + "⚠ Fusion Catalysts are very expensive!");
        tutorialMeta.setLore(tutorialLore);
        tutorial.setItemMeta(tutorialMeta);
        inventory.setItem(4, tutorial);
        
        // Slot indicators below the empty slots
        setupSlotIndicators();
        
        // Process button
        updateProcessButton();
        
        // Back button
        ItemStack back = createBackButton();
        inventory.setItem(26, back);
    }

    private void setupSlotIndicators() {
        // Crystal 1 slot indicator (below slot 9)
        ItemStack crystal1Indicator = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta crystal1Meta = crystal1Indicator.getItemMeta();
        crystal1Meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Crystal Slot #1");
        List<String> crystal1Lore = new ArrayList<>();
        crystal1Lore.add(ChatColor.GRAY + "Place first identified crystal here");
        crystal1Lore.add("");
        crystal1Lore.add(ChatColor.YELLOW + "⬆ Drop crystal above ⬆");
        crystal1Meta.setLore(crystal1Lore);
        crystal1Indicator.setItemMeta(crystal1Meta);
        inventory.setItem(18, crystal1Indicator); // Below slot 9
        
        // Crystal 2 slot indicator (below slot 11)
        ItemStack crystal2Indicator = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta crystal2Meta = crystal2Indicator.getItemMeta();
        crystal2Meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Crystal Slot #2");
        List<String> crystal2Lore = new ArrayList<>();
        crystal2Lore.add(ChatColor.GRAY + "Place second identified crystal here");
        crystal2Lore.add("");
        crystal2Lore.add(ChatColor.YELLOW + "⬆ Drop crystal above ⬆");
        crystal2Meta.setLore(crystal2Lore);
        crystal2Indicator.setItemMeta(crystal2Meta);
        inventory.setItem(20, crystal2Indicator); // Below slot 11
        
        // Catalyst slot indicator (below slot 13)
        ItemStack catalystIndicator = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta catalystMeta = catalystIndicator.getItemMeta();
        catalystMeta.setDisplayName(ChatColor.GOLD + "Catalyst Slot");
        List<String> catalystLore = new ArrayList<>();
        catalystLore.add(ChatColor.GRAY + "Place Fusion Catalyst here");
        catalystLore.add("");
        catalystLore.add(ChatColor.YELLOW + "⬆ Drop catalyst above ⬆");
        catalystMeta.setLore(catalystLore);
        catalystIndicator.setItemMeta(catalystMeta);
        inventory.setItem(22, catalystIndicator); // Below slot 13
        
        // Result slot indicator (below slot 15)
        ItemStack resultIndicator = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta resultMeta = resultIndicator.getItemMeta();
        resultMeta.setDisplayName(ChatColor.GREEN + "Result Slot");
        List<String> resultLore = new ArrayList<>();
        resultLore.add(ChatColor.GRAY + "Fused crystal appears here");
        resultLore.add("");
        resultLore.add(ChatColor.GREEN + "⬆ Take result above ⬆");
        resultMeta.setLore(resultLore);
        resultIndicator.setItemMeta(resultMeta);
        inventory.setItem(24, resultIndicator); // Below slot 15
    }
    
    private void updateProcessButton() {
        ItemStack crystal1 = inventory.getItem(CRYSTAL1_SLOT);
        ItemStack crystal2 = inventory.getItem(CRYSTAL2_SLOT);
        ItemStack catalyst = inventory.getItem(CATALYST_SLOT);
        
        boolean canProcess = crystal1 != null && CrystalManager.isCrystal(crystal1) && CrystalManager.isIdentified(crystal1) &&
                           crystal2 != null && CrystalManager.isCrystal(crystal2) && CrystalManager.isIdentified(crystal2) &&
                           catalyst != null && CrystalRecipeManager.isFusionCatalyst(catalyst);
        
        ItemStack button;
        if (canProcess) {
            button = new ItemStack(Material.LIME_CONCRETE);
            ItemMeta meta = button.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + "✓ Process Fusion");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to fuse the crystals!");
            lore.add("");
            lore.add(ChatColor.YELLOW + "This will consume the catalyst");
            lore.add(ChatColor.RED + "⚠ Both crystals will be destroyed");
            meta.setLore(lore);
            button.setItemMeta(meta);
        } else {
            button = new ItemStack(Material.RED_CONCRETE);
            ItemMeta meta = button.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "✗ Cannot Process");
            List<String> lore = new ArrayList<>();
            if (crystal1 == null) {
                lore.add(ChatColor.GRAY + "Missing: First crystal");
            } else if (!CrystalManager.isCrystal(crystal1)) {
                lore.add(ChatColor.GRAY + "Invalid: First item not a crystal");
            } else if (!CrystalManager.isIdentified(crystal1)) {
                lore.add(ChatColor.GRAY + "Invalid: First crystal not identified");
            }
            if (crystal2 == null) {
                lore.add(ChatColor.GRAY + "Missing: Second crystal");
            } else if (!CrystalManager.isCrystal(crystal2)) {
                lore.add(ChatColor.GRAY + "Invalid: Second item not a crystal");
            } else if (!CrystalManager.isIdentified(crystal2)) {
                lore.add(ChatColor.GRAY + "Invalid: Second crystal not identified");
            }
            if (catalyst == null) {
                lore.add(ChatColor.GRAY + "Missing: Fusion Catalyst");
            } else if (!CrystalRecipeManager.isFusionCatalyst(catalyst)) {
                lore.add(ChatColor.GRAY + "Invalid: Wrong catalyst type");
            }
            meta.setLore(lore);
            button.setItemMeta(meta);
        }
        
        // Move process button to avoid conflict with indicators
        inventory.setItem(16, button);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        
        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        
        // Allow placing items in input slots
        if (slot == CRYSTAL1_SLOT || slot == CRYSTAL2_SLOT || slot == CATALYST_SLOT) {
            // Prevent taking filler blocks
            if (clickedItem != null && clickedItem.hasItemMeta() && 
                clickedItem.getItemMeta().getDisplayName().equals(" ")) {
                event.setCancelled(true);
                return;
            }
            
            // Allow normal item placement/removal in these slots
            Bukkit.getScheduler().runTaskLater(ArcaniteCrystals.getInstance(), this::updateProcessButton, 1L);
            return;
        }
        
        // Allow taking from result slot
        if (slot == RESULT_SLOT) {
            // Prevent placing filler blocks
            if (cursorItem != null && cursorItem.hasItemMeta() && 
                cursorItem.getItemMeta().getDisplayName().equals(" ")) {
                event.setCancelled(true);
            }
            return;
        }
        
        // Handle process button
        if (slot == 16) { // Updated process button position
            event.setCancelled(true);
            processFusion();
            return;
        }
        
        // Handle back button
        if (slot == 26) {
            event.setCancelled(true);
            clicker.closeInventory();
            new EnhancementWorkshopGUI(clicker).open();
            return;
        }
        
        // Allow normal inventory clicks in player's inventory
        if (slot >= inventory.getSize()) {
            return;
        }
        
        // Cancel all other clicks in the GUI (filler items, etc.)
        event.setCancelled(true);
    }
    
    private void processFusion() {
        ItemStack crystal1 = inventory.getItem(CRYSTAL1_SLOT);
        ItemStack crystal2 = inventory.getItem(CRYSTAL2_SLOT);
        ItemStack catalyst = inventory.getItem(CATALYST_SLOT);
        
        // Validate inputs
        if (crystal1 == null || !CrystalManager.isCrystal(crystal1) || !CrystalManager.isIdentified(crystal1)) {
            MessageManager.sendNotification(player, "Invalid or unidentified first crystal!", MessageManager.NotificationType.ERROR);
            return;
        }
        
        if (crystal2 == null || !CrystalManager.isCrystal(crystal2) || !CrystalManager.isIdentified(crystal2)) {
            MessageManager.sendNotification(player, "Invalid or unidentified second crystal!", MessageManager.NotificationType.ERROR);
            return;
        }
        
        if (catalyst == null || !CrystalRecipeManager.isFusionCatalyst(catalyst)) {
            MessageManager.sendNotification(player, "Invalid or missing Fusion Catalyst!", MessageManager.NotificationType.ERROR);
            return;
        }
        
        // Attempt fusion using CrystalFusionManager
        if (CrystalFusionManager.fuseCrystals(player, crystal1, crystal2)) {
            // Success - create fused crystal
            ItemStack fusedCrystal = CrystalManager.fuseCrystals(crystal1, crystal2);
            
            // Clear input slots
            inventory.setItem(CRYSTAL1_SLOT, null);
            inventory.setItem(CRYSTAL2_SLOT, null);
            inventory.setItem(CATALYST_SLOT, null);
            
            // Set result
            inventory.setItem(RESULT_SLOT, fusedCrystal);
            
            // Update button
            updateProcessButton();
            
            // Effects
            SoundManager.playMenuSound(player, SoundManager.MenuSoundType.SUCCESS);
            MessageManager.sendNotification(player, "Crystals fused successfully!", MessageManager.NotificationType.SUCCESS);
            
            player.sendMessage(ChatColor.GREEN + "✨ The crystals have been merged into one powerful crystal!");
        } else {
            MessageManager.sendNotification(player, "Fusion failed!", MessageManager.NotificationType.ERROR);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this && isOpen) {
            // Return items to player
            ItemStack crystal1 = inventory.getItem(CRYSTAL1_SLOT);
            ItemStack crystal2 = inventory.getItem(CRYSTAL2_SLOT);
            ItemStack catalyst = inventory.getItem(CATALYST_SLOT);
            ItemStack result = inventory.getItem(RESULT_SLOT);
            
            if (crystal1 != null) player.getInventory().addItem(crystal1);
            if (crystal2 != null) player.getInventory().addItem(crystal2);
            if (catalyst != null) player.getInventory().addItem(catalyst);
            if (result != null) player.getInventory().addItem(result);
            
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
    
    // Helper methods
    private void fillEmptySlots(Material material) {
        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);
        
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }
    
    private ItemStack createBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "← Back to Workshop");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Return to the enhancement workshop");
        meta.setLore(lore);
        back.setItemMeta(meta);
        return back;
    }
} 