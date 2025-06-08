package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import dev.lsdmc.arcaniteCrystals.manager.CrystalManager.CrystalType;
import dev.lsdmc.arcaniteCrystals.manager.UpgradeManager;
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
import java.util.Set;

public class CrystalOptionsGUI implements InventoryHolder, Listener {
    private static final int IDENTIFY_SLOT = 11;
    private static final int ACTIVATE_SLOT = 13;
    private static final int RECHARGE_SLOT = 15;
    private static final int BACK_SLOT = 49;
    
    private final Player player;
    private final ItemStack crystal;
    private final Inventory inventory;
    private boolean isOpen = false;
    
    public CrystalOptionsGUI(Player player, ItemStack crystal) {
        this.player = player;
        this.crystal = crystal;
        this.inventory = Bukkit.createInventory(this, 54, 
            ChatColor.DARK_PURPLE + "✦ Crystal Options ✦");
        buildOptionsMenu();
    }
    
    private void buildOptionsMenu() {
        // Fill borders
        fillBorders();
        
        // Add crystal info
        addCrystalInfo();
        
        // Add action buttons
        addActionButtons();
    }
    
    private void fillBorders() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName(" ");
        border.setItemMeta(meta);
        
        // Top and bottom borders
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(45 + i, border);
        }
        
        // Side borders
        for (int i = 0; i < 6; i++) {
            inventory.setItem(i * 9, border);
            inventory.setItem(i * 9 + 8, border);
        }
    }
    
    private void addCrystalInfo() {
        ItemStack infoItem = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = infoItem.getItemMeta();
        
        CrystalType type = CrystalManager.getCrystalType(crystal);
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + type.getDisplayName());
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Type: " + type.getDisplayName());
        lore.add(ChatColor.GRAY + "Max Effects: " + type.getMaxEffects());
        lore.add(ChatColor.GRAY + "Base Energy: " + type.getBaseEnergy());
        lore.add("");
        
        if (CrystalManager.isIdentified(crystal)) {
            List<String> effects = CrystalManager.getCrystalEffects(crystal);
            lore.add(ChatColor.LIGHT_PURPLE + "Effects:");
            for (String effect : effects) {
                lore.add(ChatColor.GRAY + "• " + beautifyEffectName(effect));
            }
        } else {
            lore.add(ChatColor.YELLOW + "This crystal needs to be identified");
            lore.add(ChatColor.YELLOW + "to reveal its effects");
        }
        
        meta.setLore(lore);
        infoItem.setItemMeta(meta);
        inventory.setItem(4, infoItem);
    }
    
    private void addActionButtons() {
        // Identify Button
        ItemStack identifyBtn = new ItemStack(Material.SPYGLASS);
        ItemMeta identifyMeta = identifyBtn.getItemMeta();
        identifyMeta.setDisplayName(ChatColor.AQUA + "Identify Crystal");
        
        List<String> identifyLore = new ArrayList<>();
        if (CrystalManager.isIdentified(crystal)) {
            identifyLore.add(ChatColor.RED + "This crystal is already identified");
        } else {
            identifyLore.add(ChatColor.GRAY + "Reveal the crystal's effects");
            identifyLore.add(ChatColor.GRAY + "based on your unlocked talents");
            identifyLore.add("");
            identifyLore.add(ChatColor.YELLOW + "Click to identify");
        }
        
        identifyMeta.setLore(identifyLore);
        identifyBtn.setItemMeta(identifyMeta);
        inventory.setItem(IDENTIFY_SLOT, identifyBtn);
        
        // Activate Button
        ItemStack activateBtn = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta activateMeta = activateBtn.getItemMeta();
        activateMeta.setDisplayName(ChatColor.GREEN + "Activate Crystal");
        
        List<String> activateLore = new ArrayList<>();
        if (!CrystalManager.isIdentified(crystal)) {
            activateLore.add(ChatColor.RED + "Crystal must be identified first");
        } else if (CrystalManager.isActivatedCrystal(crystal)) {
            activateLore.add(ChatColor.RED + "This crystal is already active");
        } else {
            activateLore.add(ChatColor.GRAY + "Place this crystal in your off-hand");
            activateLore.add(ChatColor.GRAY + "to activate its effects");
            activateLore.add("");
            activateLore.add(ChatColor.YELLOW + "Click to activate");
        }
        
        activateMeta.setLore(activateLore);
        activateBtn.setItemMeta(activateMeta);
        inventory.setItem(ACTIVATE_SLOT, activateBtn);
        
        // Recharge Button
        ItemStack rechargeBtn = new ItemStack(Material.QUARTZ);
        ItemMeta rechargeMeta = rechargeBtn.getItemMeta();
        rechargeMeta.setDisplayName(ChatColor.YELLOW + "Recharge Crystal");
        
        List<String> rechargeLore = new ArrayList<>();
        if (!CrystalManager.isIdentified(crystal)) {
            rechargeLore.add(ChatColor.RED + "Crystal must be identified first");
        } else {
            int currentEnergy = CrystalManager.getEnergy(crystal);
            int maxEnergy = CrystalManager.getMaxEnergy(crystal);
            rechargeLore.add(ChatColor.GRAY + "Current Energy: " + currentEnergy + "/" + maxEnergy);
            rechargeLore.add("");
            rechargeLore.add(ChatColor.GRAY + "Cost: 1 Quartz = 1000 Energy");
            rechargeLore.add("");
            rechargeLore.add(ChatColor.YELLOW + "Click to recharge");
        }
        
        rechargeMeta.setLore(rechargeLore);
        rechargeBtn.setItemMeta(rechargeMeta);
        inventory.setItem(RECHARGE_SLOT, rechargeBtn);
        
        // Back Button
        ItemStack backBtn = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backBtn.getItemMeta();
        backMeta.setDisplayName(ChatColor.YELLOW + "Back to Inventory");
        backBtn.setItemMeta(backMeta);
        inventory.setItem(BACK_SLOT, backBtn);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);
        
        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        switch (slot) {
            case IDENTIFY_SLOT -> {
                if (CrystalManager.getCrystalEffects(crystal).isEmpty()) {
                    identifyCrystal(clicker);
                }
            }
            case ACTIVATE_SLOT -> {
                if (!CrystalManager.isDepletedCrystal(crystal)) {
                    equipCrystal(clicker);
                } else {
                    clicker.sendMessage(ChatColor.RED + "Cannot activate a depleted crystal!");
                }
            }
            case RECHARGE_SLOT -> {
                if (CrystalManager.isDepletedCrystal(crystal)) {
                    rechargeCrystal(clicker);
                } else {
                    clicker.sendMessage(ChatColor.GREEN + "Crystal is already fully charged!");
                }
            }
            case BACK_SLOT -> {
                clicker.closeInventory();
                new CrystalInventoryGUI(clicker).open();
            }
        }
    }
    
    private void identifyCrystal(Player player) {
        Set<String> unlockedUpgrades = PlayerDataManager.getUnlockedUpgrades(player.getUniqueId());
        if (unlockedUpgrades.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You need to unlock some talents first!");
            return;
        }
        
        // Generate random effects based on unlocked upgrades
        List<String> effects = CrystalManager.generateRandomEffects(player, unlockedUpgrades, crystal);
        CrystalManager.setCrystalEffects(crystal, effects);
        
        player.sendMessage(ChatColor.GREEN + "Crystal identified! New effects revealed.");
        buildOptionsMenu(); // Refresh the menu
    }
    
    private void equipCrystal(Player player) {
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && !offHand.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "Your off-hand must be empty to equip a crystal!");
            return;
        }
        
        player.getInventory().setItemInOffHand(crystal);
        player.sendMessage(ChatColor.GREEN + "Crystal equipped to off-hand!");
    }
    
    private void rechargeCrystal(Player player) {
        int currentEnergy = CrystalManager.getEnergy(crystal);
        int maxEnergy = CrystalManager.getMaxEnergy(crystal);
        int energyNeeded = maxEnergy - currentEnergy;
        
        if (energyNeeded <= 0) {
            player.sendMessage(ChatColor.GREEN + "Crystal is already fully charged!");
            return;
        }
        
        int quartzNeeded = (int) Math.ceil(energyNeeded / 1000.0);
        if (!player.getInventory().contains(Material.QUARTZ, quartzNeeded)) {
            player.sendMessage(ChatColor.RED + "You need " + quartzNeeded + " quartz to recharge this crystal!");
            return;
        }
        
        // Remove quartz and recharge
        player.getInventory().removeItem(new ItemStack(Material.QUARTZ, quartzNeeded));
        CrystalManager.setEnergy(crystal, maxEnergy);
        
        player.sendMessage(ChatColor.GREEN + "Crystal recharged to full energy!");
        buildOptionsMenu(); // Refresh the menu
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
    
    private String beautifyEffectName(String effect) {
        String[] words = effect.replace("_", " ").toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
} 