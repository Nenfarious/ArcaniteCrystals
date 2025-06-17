package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import dev.lsdmc.arcaniteCrystals.manager.CrystalSocketManager;
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
 * Workshop for socketing crystals into items using Socketing Catalysts.
 * Uses a 4-slot system: Item + Crystal + Catalyst = Socketed Item
 */
public class CrystalSocketingGUI implements InventoryHolder, Listener {
    private final Player player;
    private final Inventory inventory;
    private boolean isOpen = false;
    
    // Slot positions
    private static final int ITEM_SLOT = 9;
    private static final int CRYSTAL_SLOT = 11;
    private static final int CATALYST_SLOT = 13;
    private static final int RESULT_SLOT = 15;
    private static final int PROCESS_BUTTON = 22;
    
    public CrystalSocketingGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.GOLD + "Crystal Socketing Workshop");
        setupGUI();
        Bukkit.getPluginManager().registerEvents(this, ArcaniteCrystals.getInstance());
    }

    private void setupGUI() {
        // Fill with glass panes first
        fillEmptySlots(Material.ORANGE_STAINED_GLASS_PANE);
        
        // Clear the working slots
        inventory.setItem(ITEM_SLOT, null);
        inventory.setItem(CRYSTAL_SLOT, null);
        inventory.setItem(CATALYST_SLOT, null);
        inventory.setItem(RESULT_SLOT, null);
        
        // Tutorial book
        ItemStack tutorial = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta tutorialMeta = tutorial.getItemMeta();
        tutorialMeta.setDisplayName(ChatColor.GREEN + "📖 How to Socket Crystals");
        List<String> tutorialLore = new ArrayList<>();
        tutorialLore.add(ChatColor.GRAY + "Step 1: Place an item (tool/armor)");
        tutorialLore.add(ChatColor.GRAY + "Step 2: Add an identified crystal");
        tutorialLore.add(ChatColor.GRAY + "Step 3: Add a Socketing Catalyst");
        tutorialLore.add(ChatColor.GRAY + "Step 4: Click the Process button");
        tutorialLore.add("");
        tutorialLore.add(ChatColor.YELLOW + "💡 Tip: Only tools and armor can be socketed!");
        tutorialLore.add(ChatColor.YELLOW + "💡 Crystal effects apply when held/worn");
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
        // Item slot indicator (below slot 9)
        ItemStack itemIndicator = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        ItemMeta itemMeta = itemIndicator.getItemMeta();
        itemMeta.setDisplayName(ChatColor.BLUE + "Item Slot");
        List<String> itemLore = new ArrayList<>();
        itemLore.add(ChatColor.GRAY + "Place your tool or armor here");
        itemLore.add("");
        itemLore.add(ChatColor.YELLOW + "⬆ Drop item above ⬆");
        itemMeta.setLore(itemLore);
        itemIndicator.setItemMeta(itemMeta);
        inventory.setItem(18, itemIndicator); // Below slot 9
        
        // Crystal slot indicator (below slot 11)
        ItemStack crystalIndicator = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta crystalMeta = crystalIndicator.getItemMeta();
        crystalMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Crystal Slot");
        List<String> crystalLore = new ArrayList<>();
        crystalLore.add(ChatColor.GRAY + "Place identified crystal here");
        crystalLore.add("");
        crystalLore.add(ChatColor.YELLOW + "⬆ Drop crystal above ⬆");
        crystalMeta.setLore(crystalLore);
        crystalIndicator.setItemMeta(crystalMeta);
        inventory.setItem(20, crystalIndicator); // Below slot 11
        
        // Catalyst slot indicator (below slot 13)
        ItemStack catalystIndicator = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta catalystMeta = catalystIndicator.getItemMeta();
        catalystMeta.setDisplayName(ChatColor.GOLD + "Catalyst Slot");
        List<String> catalystLore = new ArrayList<>();
        catalystLore.add(ChatColor.GRAY + "Place Socketing Catalyst here");
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
        resultLore.add(ChatColor.GRAY + "Socketed item appears here");
        resultLore.add("");
        resultLore.add(ChatColor.GREEN + "⬆ Take result above ⬆");
        resultMeta.setLore(resultLore);
        resultIndicator.setItemMeta(resultMeta);
        inventory.setItem(24, resultIndicator); // Below slot 15
    }
    
    private void updateProcessButton() {
        ItemStack item = inventory.getItem(ITEM_SLOT);
        ItemStack crystal = inventory.getItem(CRYSTAL_SLOT);
        ItemStack catalyst = inventory.getItem(CATALYST_SLOT);
        
        boolean canProcess = item != null && isValidSocketTarget(item) &&
                           crystal != null && CrystalManager.isCrystal(crystal) && 
                           CrystalManager.isIdentified(crystal) &&
                           catalyst != null && CrystalRecipeManager.isSocketingCatalyst(catalyst);
        
        ItemStack button;
        if (canProcess) {
            button = new ItemStack(Material.LIME_CONCRETE);
            ItemMeta meta = button.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + "✓ Process Socketing");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to socket the crystal!");
            lore.add("");
            lore.add(ChatColor.YELLOW + "This will consume the catalyst");
            meta.setLore(lore);
            button.setItemMeta(meta);
        } else {
            button = new ItemStack(Material.RED_CONCRETE);
            ItemMeta meta = button.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "✗ Cannot Process");
            List<String> lore = new ArrayList<>();
            if (item == null) {
                lore.add(ChatColor.GRAY + "Missing: Tool or armor item");
            } else if (!isValidSocketTarget(item)) {
                lore.add(ChatColor.GRAY + "Invalid: Item cannot be socketed");
            }
            if (crystal == null) {
                lore.add(ChatColor.GRAY + "Missing: Identified crystal");
            } else if (!CrystalManager.isCrystal(crystal)) {
                lore.add(ChatColor.GRAY + "Invalid: Not a crystal");
            } else if (!CrystalManager.isIdentified(crystal)) {
                lore.add(ChatColor.GRAY + "Invalid: Crystal not identified");
            }
            if (catalyst == null) {
                lore.add(ChatColor.GRAY + "Missing: Socketing Catalyst");
            } else if (!CrystalRecipeManager.isSocketingCatalyst(catalyst)) {
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
        if (slot == ITEM_SLOT || slot == CRYSTAL_SLOT || slot == CATALYST_SLOT) {
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
            processSocketing();
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
    
    private void processSocketing() {
        ItemStack item = inventory.getItem(ITEM_SLOT);
        ItemStack crystal = inventory.getItem(CRYSTAL_SLOT);
        ItemStack catalyst = inventory.getItem(CATALYST_SLOT);
        
        // Validate inputs
        if (item == null || !isValidSocketTarget(item)) {
            MessageManager.sendNotification(player, "Invalid item for socketing!", MessageManager.NotificationType.ERROR);
            return;
        }
        
        if (crystal == null || !CrystalManager.isCrystal(crystal) || 
            !CrystalManager.isIdentified(crystal)) {
            MessageManager.sendNotification(player, "Invalid or unidentified crystal!", MessageManager.NotificationType.ERROR);
            return;
        }
        
        if (catalyst == null || !CrystalRecipeManager.isSocketingCatalyst(catalyst)) {
            MessageManager.sendNotification(player, "Invalid or missing Socketing Catalyst!", MessageManager.NotificationType.ERROR);
            return;
        }
        
        // Attempt socketing using CrystalSocketManager
        if (CrystalSocketManager.socketCrystal(player, item, crystal)) {
            // Success - create socketed item
            ItemStack socketedItem = item.clone();
            
            // Clear input slots
            inventory.setItem(ITEM_SLOT, null);
            inventory.setItem(CRYSTAL_SLOT, null);
            inventory.setItem(CATALYST_SLOT, null);
            
            // Set result
            inventory.setItem(RESULT_SLOT, socketedItem);
            
            // Update button
            updateProcessButton();
            
            // Effects
            SoundManager.playMenuSound(player, SoundManager.MenuSoundType.SUCCESS);
            MessageManager.sendNotification(player, "Crystal socketed successfully!", MessageManager.NotificationType.SUCCESS);
            
            player.sendMessage(ChatColor.GREEN + "✨ The crystal has been embedded into your item!");
        } else {
            MessageManager.sendNotification(player, "Socketing failed!", MessageManager.NotificationType.ERROR);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this && isOpen) {
            // Return items to player
            ItemStack item = inventory.getItem(ITEM_SLOT);
            ItemStack crystal = inventory.getItem(CRYSTAL_SLOT);
            ItemStack catalyst = inventory.getItem(CATALYST_SLOT);
            ItemStack result = inventory.getItem(RESULT_SLOT);
            
            if (item != null) player.getInventory().addItem(item);
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
    
    private boolean isValidSocketTarget(ItemStack item) {
        if (item == null) return false;
        
        Material type = item.getType();
        return type.name().endsWith("_SWORD") ||
               type.name().endsWith("_AXE") ||
               type.name().endsWith("_PICKAXE") ||
               type.name().endsWith("_SHOVEL") ||
               type.name().endsWith("_HOE") ||
               type.name().endsWith("_HELMET") ||
               type.name().endsWith("_CHESTPLATE") ||
               type.name().endsWith("_LEGGINGS") ||
               type.name().endsWith("_BOOTS");
    }
} 