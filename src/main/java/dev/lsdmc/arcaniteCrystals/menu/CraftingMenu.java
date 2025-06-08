package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
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

public class CraftingMenu implements InventoryHolder, Listener {
    private static final int MENU_SIZE = 54;
    private final Player player;
    private final Inventory inventory;
    private boolean isOpen = false;

    public CraftingMenu(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, MENU_SIZE, 
            ChatColor.DARK_PURPLE + "✦ Crystal Crafting ✦");
        buildMenu();
    }

    private void buildMenu() {
        // Fill borders
        fillBorders();
        
        // Add recipe items
        addBasicCrystalRecipe();
        addFusionCrystalRecipe();
        addSocketCrystalRecipe();
        addIdentificationCrystalRecipe();
        
        // Add help button
        addHelpButton();
        
        // Add close button
        addCloseButton();
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

    private void addBasicCrystalRecipe() {
        ItemStack recipe = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = recipe.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Basic Crystal Recipe");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Required Materials:");
        lore.add(ChatColor.YELLOW + "• 8x Amethyst Shard");
        lore.add(ChatColor.YELLOW + "• 1x End Crystal");
        lore.add("");
        lore.add(ChatColor.GRAY + "Pattern:");
        lore.add(ChatColor.DARK_GRAY + "AAA");
        lore.add(ChatColor.DARK_GRAY + "ABA");
        lore.add(ChatColor.DARK_GRAY + "AAA");
        lore.add("");
        lore.add(ChatColor.GRAY + "A = Amethyst Shard");
        lore.add(ChatColor.GRAY + "B = End Crystal");
        
        meta.setLore(lore);
        recipe.setItemMeta(meta);
        inventory.setItem(20, recipe);
    }

    private void addFusionCrystalRecipe() {
        ItemStack recipe = new ItemStack(Material.AMETHYST_BLOCK);
        ItemMeta meta = recipe.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Fusion Crystal Recipe");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Required Materials:");
        lore.add(ChatColor.YELLOW + "• 8x Amethyst Block");
        lore.add(ChatColor.YELLOW + "• 2x Nether Star");
        lore.add(ChatColor.YELLOW + "• 1x End Crystal");
        lore.add("");
        lore.add(ChatColor.GRAY + "Pattern:");
        lore.add(ChatColor.DARK_GRAY + "AAA");
        lore.add(ChatColor.DARK_GRAY + "BCB");
        lore.add(ChatColor.DARK_GRAY + "AAA");
        lore.add("");
        lore.add(ChatColor.GRAY + "A = Amethyst Block");
        lore.add(ChatColor.GRAY + "B = Nether Star");
        lore.add(ChatColor.GRAY + "C = End Crystal");
        
        meta.setLore(lore);
        recipe.setItemMeta(meta);
        inventory.setItem(22, recipe);
    }

    private void addSocketCrystalRecipe() {
        ItemStack recipe = new ItemStack(Material.END_CRYSTAL);
        ItemMeta meta = recipe.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Socket Crystal Recipe");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Required Materials:");
        lore.add(ChatColor.YELLOW + "• 4x End Crystal");
        lore.add(ChatColor.YELLOW + "• 4x Netherite Ingot");
        lore.add(ChatColor.YELLOW + "• 1x Dragon Egg");
        lore.add("");
        lore.add(ChatColor.GRAY + "Pattern:");
        lore.add(ChatColor.DARK_GRAY + "NNN");
        lore.add(ChatColor.DARK_GRAY + "NEN");
        lore.add(ChatColor.DARK_GRAY + "NDN");
        lore.add("");
        lore.add(ChatColor.GRAY + "N = Netherite Ingot");
        lore.add(ChatColor.GRAY + "E = End Crystal");
        lore.add(ChatColor.GRAY + "D = Dragon Egg");
        
        meta.setLore(lore);
        recipe.setItemMeta(meta);
        inventory.setItem(24, recipe);
    }

    private void addIdentificationCrystalRecipe() {
        ItemStack recipe = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = recipe.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Identification Crystal Recipe");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Required Materials:");
        lore.add(ChatColor.YELLOW + "• 4x Nether Star");
        lore.add(ChatColor.YELLOW + "• 4x Diamond Block");
        lore.add(ChatColor.YELLOW + "• 1x Dragon Egg");
        lore.add("");
        lore.add(ChatColor.GRAY + "Pattern:");
        lore.add(ChatColor.DARK_GRAY + "DDD");
        lore.add(ChatColor.DARK_GRAY + "DND");
        lore.add(ChatColor.DARK_GRAY + "DED");
        lore.add("");
        lore.add(ChatColor.GRAY + "D = Diamond Block");
        lore.add(ChatColor.GRAY + "N = Nether Star");
        lore.add(ChatColor.GRAY + "E = Dragon Egg");
        
        meta.setLore(lore);
        recipe.setItemMeta(meta);
        inventory.setItem(40, recipe);
    }

    private void addHelpButton() {
        ItemStack help = new ItemStack(Material.BOOK);
        ItemMeta meta = help.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Help");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "How to craft crystals:");
        lore.add(ChatColor.YELLOW + "• Place materials in the pattern shown");
        lore.add(ChatColor.YELLOW + "• Higher tier crystals require better materials");
        lore.add(ChatColor.YELLOW + "• Some recipes may require special items");
        
        meta.setLore(lore);
        help.setItemMeta(meta);
        inventory.setItem(49, help);
    }

    private void addCloseButton() {
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta meta = close.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Close Menu");
        close.setItemMeta(meta);
        inventory.setItem(53, close);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);
        
        if (event.getRawSlot() == 53) { // Close button
            player.closeInventory();
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