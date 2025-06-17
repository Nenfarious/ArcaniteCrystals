package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.manager.CrystalIdentificationManager;
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
 * Workshop for identifying mystery crystals using Identification Catalysts.
 * Uses a 3-slot system: Crystal + Catalyst = Identified Crystal
 */
public class CrystalIdentificationGUI implements InventoryHolder, Listener {
    private final Player player;
    private final Inventory inventory;
    private boolean isOpen = false;

    // Slot positions
    private static final int CRYSTAL_SLOT = 10;
    private static final int CATALYST_SLOT = 12;
    private static final int RESULT_SLOT = 14;
    private static final int PROCESS_BUTTON = 22;

    public CrystalIdentificationGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.AQUA + "Crystal Identification Workshop");
        setupGUI();
        Bukkit.getPluginManager().registerEvents(this, ArcaniteCrystals.getInstance());
    }

    private void setupGUI() {
        // Fill with glass panes first
        fillEmptySlots(Material.CYAN_STAINED_GLASS_PANE);
        
        // Clear the working slots
        inventory.setItem(CRYSTAL_SLOT, null);
        inventory.setItem(CATALYST_SLOT, null);
        inventory.setItem(RESULT_SLOT, null);
        
        // Tutorial book
        ItemStack tutorial = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta tutorialMeta = tutorial.getItemMeta();
        tutorialMeta.setDisplayName(ChatColor.GREEN + "📖 How to Identify Crystals");
        List<String> tutorialLore = new ArrayList<>();
        tutorialLore.add(ChatColor.GRAY + "Step 1: Place a mystery crystal");
        tutorialLore.add(ChatColor.GRAY + "Step 2: Add an Identification Catalyst");
        tutorialLore.add(ChatColor.GRAY + "Step 3: Click the Process button");
        tutorialLore.add("");
        tutorialLore.add(ChatColor.YELLOW + "💡 Tip: Only mystery crystals can be identified!");
        tutorialLore.add(ChatColor.YELLOW + "💡 The catalyst will be consumed on use");
        tutorialMeta.setLore(tutorialLore);
        tutorial.setItemMeta(tutorialMeta);
        inventory.setItem(4, tutorial);
        
        // Slot indicators below the empty slots
        setupSlotIndicators();
        
        // Process button
        updateProcessButton();
        
        // Back button
        ItemStack back = createBackButton();
        inventory.setItem(18, back);
    }

    private void setupSlotIndicators() {
        // Crystal slot indicator (below slot 10)
        ItemStack crystalIndicator = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta crystalMeta = crystalIndicator.getItemMeta();
        crystalMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Mystery Crystal Slot");
        List<String> crystalLore = new ArrayList<>();
        crystalLore.add(ChatColor.GRAY + "Place unidentified crystal here");
        crystalLore.add("");
        crystalLore.add(ChatColor.YELLOW + "⬆ Drop crystal above ⬆");
        crystalMeta.setLore(crystalLore);
        crystalIndicator.setItemMeta(crystalMeta);
        inventory.setItem(19, crystalIndicator); // Below slot 10
        
        // Catalyst slot indicator (below slot 12)
        ItemStack catalystIndicator = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta catalystMeta = catalystIndicator.getItemMeta();
        catalystMeta.setDisplayName(ChatColor.GOLD + "Catalyst Slot");
        List<String> catalystLore = new ArrayList<>();
        catalystLore.add(ChatColor.GRAY + "Place Identification Catalyst here");
        catalystLore.add("");
        catalystLore.add(ChatColor.YELLOW + "⬆ Drop catalyst above ⬆");
        catalystMeta.setLore(catalystLore);
        catalystIndicator.setItemMeta(catalystMeta);
        inventory.setItem(21, catalystIndicator); // Below slot 12
        
        // Result slot indicator (below slot 14)
        ItemStack resultIndicator = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta resultMeta = resultIndicator.getItemMeta();
        resultMeta.setDisplayName(ChatColor.GREEN + "Result Slot");
        List<String> resultLore = new ArrayList<>();
        resultLore.add(ChatColor.GRAY + "Identified crystal appears here");
        resultLore.add("");
        resultLore.add(ChatColor.GREEN + "⬆ Take result above ⬆");
        resultMeta.setLore(resultLore);
        resultIndicator.setItemMeta(resultMeta);
        inventory.setItem(23, resultIndicator); // Below slot 14
    }
    
    private void updateProcessButton() {
        ItemStack crystal = inventory.getItem(CRYSTAL_SLOT);
        ItemStack catalyst = inventory.getItem(CATALYST_SLOT);
        
        boolean canProcess = crystal != null && CrystalManager.isCrystal(crystal) && 
                           !CrystalManager.isIdentified(crystal) &&
                           catalyst != null && CrystalRecipeManager.isIdentificationCatalyst(catalyst);
        
        ItemStack button;
        if (canProcess) {
            button = new ItemStack(Material.LIME_CONCRETE);
            ItemMeta meta = button.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + "✓ Process Identification");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to identify the crystal!");
            lore.add("");
            lore.add(ChatColor.YELLOW + "This will consume the catalyst");
            meta.setLore(lore);
            button.setItemMeta(meta);
        } else {
            button = new ItemStack(Material.RED_CONCRETE);
            ItemMeta meta = button.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "✗ Cannot Process");
            List<String> lore = new ArrayList<>();
            if (crystal == null) {
                lore.add(ChatColor.GRAY + "Missing: Mystery crystal");
            } else if (!CrystalManager.isCrystal(crystal)) {
                lore.add(ChatColor.GRAY + "Invalid: Not a crystal");
            } else if (CrystalManager.isIdentified(crystal)) {
                lore.add(ChatColor.GRAY + "Invalid: Crystal already identified");
            }
            if (catalyst == null) {
                lore.add(ChatColor.GRAY + "Missing: Identification Catalyst");
            } else if (!CrystalRecipeManager.isIdentificationCatalyst(catalyst)) {
                lore.add(ChatColor.GRAY + "Invalid: Wrong catalyst type");
            }
            meta.setLore(lore);
            button.setItemMeta(meta);
        }
        
        inventory.setItem(PROCESS_BUTTON, button);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        
        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        
        // Allow placing items in input slots
        if (slot == CRYSTAL_SLOT || slot == CATALYST_SLOT) {
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
        if (slot == PROCESS_BUTTON) {
            event.setCancelled(true);
            processIdentification();
            return;
        }
        
        // Handle back button
        if (slot == 18) {
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
    
    private void processIdentification() {
        ItemStack crystal = inventory.getItem(CRYSTAL_SLOT);
        ItemStack catalyst = inventory.getItem(CATALYST_SLOT);
        
        // Validate inputs
        if (crystal == null || !CrystalManager.isCrystal(crystal) || CrystalManager.isIdentified(crystal)) {
            MessageManager.sendNotification(player, "Invalid or already identified crystal!", MessageManager.NotificationType.ERROR);
            return;
        }
        
        if (catalyst == null || !CrystalRecipeManager.isIdentificationCatalyst(catalyst)) {
            MessageManager.sendNotification(player, "Invalid or missing Identification Catalyst!", MessageManager.NotificationType.ERROR);
            return;
        }
        
        // Attempt identification
        CrystalIdentificationManager.IdentificationResult result = 
            CrystalIdentificationManager.identifyCrystal(player, crystal, 
                CrystalIdentificationManager.IdentificationMethod.BASIC_SCROLL);
        
        if (result.isSuccess()) {
            // Success - create identified crystal
            ItemStack identifiedCrystal = result.getIdentifiedCrystal();
            
            // Clear input slots
            inventory.setItem(CRYSTAL_SLOT, null);
            inventory.setItem(CATALYST_SLOT, null);
            
            // Set result
            inventory.setItem(RESULT_SLOT, identifiedCrystal);
            
            // Update button
            updateProcessButton();
            
            // Effects
            SoundManager.playMenuSound(player, SoundManager.MenuSoundType.SUCCESS);
            MessageManager.sendNotification(player, "Crystal identified successfully!", MessageManager.NotificationType.SUCCESS);
            
            player.sendMessage(ChatColor.GREEN + "✨ The crystal's hidden properties have been revealed!");
        } else {
            MessageManager.sendNotification(player, result.getMessage(), MessageManager.NotificationType.ERROR);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this && isOpen) {
            // Return items to player
            ItemStack crystal = inventory.getItem(CRYSTAL_SLOT);
            ItemStack catalyst = inventory.getItem(CATALYST_SLOT);
            ItemStack result = inventory.getItem(RESULT_SLOT);
            
            if (crystal != null) player.getInventory().addItem(crystal);
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