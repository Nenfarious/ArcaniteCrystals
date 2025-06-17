package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.manager.CrystalRecipeManager;
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
 * Paginated Catalyst Recipe Viewer
 * Shows one recipe per page with clear ingredient layouts
 */
public class CraftingMenu implements InventoryHolder, Listener {
    private static final int MENU_SIZE = 54;
    
    // Navigation slots
    private static final int PREV_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int BACK_SLOT = 49;
    private static final int INFO_SLOT = 4;
    
    // Recipe display area (3x3 crafting grid)
    private static final int[] RECIPE_SLOTS = {19, 20, 21, 28, 29, 30, 37, 38, 39};
    private static final int RESULT_SLOT = 24;
    
    private final Player player;
    private final Inventory inventory;
    private boolean isOpen = false;
    
    // Recipe data
    private final List<RecipeData> recipes;
    private int currentPage = 0;
    
    // Recipe definitions
    private static class RecipeData {
        final String name;
        final String description;
        final Material[] ingredients;
        final ItemStack result;
        final Material displayMaterial;
        final ChatColor nameColor;
        
        RecipeData(String name, String description, Material[] ingredients, ItemStack result, Material displayMaterial, ChatColor nameColor) {
            this.name = name;
            this.description = description;
            this.ingredients = ingredients;
            this.result = result;
            this.displayMaterial = displayMaterial;
            this.nameColor = nameColor;
        }
    }

    public CraftingMenu(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, MENU_SIZE, 
            ChatColor.DARK_PURPLE + "✦ Catalyst Recipes ✦");
        
        // Initialize recipes
        this.recipes = initializeRecipes();
        
        buildMenu();
    }
    
    private List<RecipeData> initializeRecipes() {
        List<RecipeData> recipeList = new ArrayList<>();
        
        // Identification Catalyst Recipe
        recipeList.add(new RecipeData(
            "Identification Catalyst",
            "Reveals mystery crystal effects",
            new Material[]{
                Material.GOLD_NUGGET, Material.PAPER, Material.GOLD_NUGGET,
                Material.PAPER, Material.EXPERIENCE_BOTTLE, Material.PAPER,
                Material.GOLD_NUGGET, Material.PAPER, Material.GOLD_NUGGET
            },
            CrystalRecipeManager.createIdentificationCatalyst(),
            Material.SPYGLASS,
            ChatColor.AQUA
        ));
        
        // Socketing Catalyst Recipe
        recipeList.add(new RecipeData(
            "Socketing Catalyst",
            "Embeds crystals into items",
            new Material[]{
                Material.DIAMOND, Material.GOLD_INGOT, Material.DIAMOND,
                Material.GOLD_INGOT, Material.ANVIL, Material.GOLD_INGOT,
                Material.DIAMOND, Material.GOLD_INGOT, Material.DIAMOND
            },
            CrystalRecipeManager.createSocketingCatalyst(),
            Material.ENCHANTING_TABLE,
            ChatColor.GOLD
        ));
        
        // Fusion Catalyst Recipe
        recipeList.add(new RecipeData(
            "Fusion Catalyst",
            "Combines two crystals into one",
            new Material[]{
                Material.NETHERITE_INGOT, Material.NETHER_STAR, Material.NETHERITE_INGOT,
                Material.NETHER_STAR, Material.END_CRYSTAL, Material.NETHER_STAR,
                Material.NETHERITE_INGOT, Material.NETHER_STAR, Material.NETHERITE_INGOT
            },
            CrystalRecipeManager.createFusionCatalyst(),
            Material.BEACON,
            ChatColor.LIGHT_PURPLE
        ));
        
        return recipeList;
    }

    private void buildMenu() {
        // Clear inventory
        inventory.clear();
        
        // Fill borders
        fillBorders();
        
        // Add current recipe
        displayCurrentRecipe();
        
        // Add navigation
        addNavigation();
        
        // Add info
        addInfoSection();
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
        for (int i = 1; i < 5; i++) {
            inventory.setItem(i * 9, border);
            inventory.setItem(i * 9 + 8, border);
        }
    }
    
    private void displayCurrentRecipe() {
        if (recipes.isEmpty()) return;
        
        RecipeData recipe = recipes.get(currentPage);
        
        // Clear recipe area
        for (int slot : RECIPE_SLOTS) {
            inventory.setItem(slot, null);
        }
        inventory.setItem(RESULT_SLOT, null);
        
        // Display recipe ingredients in 3x3 grid
        for (int i = 0; i < Math.min(recipe.ingredients.length, RECIPE_SLOTS.length); i++) {
            if (recipe.ingredients[i] != null) {
                ItemStack ingredient = new ItemStack(recipe.ingredients[i]);
                ItemMeta meta = ingredient.getItemMeta();
                meta.setDisplayName(ChatColor.WHITE + ingredient.getType().name().replace("_", " "));
                ingredient.setItemMeta(meta);
                inventory.setItem(RECIPE_SLOTS[i], ingredient);
            }
        }
        
        // Display result
        inventory.setItem(RESULT_SLOT, recipe.result.clone());
        
        // Add recipe title
        ItemStack title = new ItemStack(recipe.displayMaterial);
        ItemMeta titleMeta = title.getItemMeta();
        titleMeta.setDisplayName(recipe.nameColor + "✦ " + recipe.name + " ✦");
        List<String> titleLore = new ArrayList<>();
        titleLore.add("");
        titleLore.add(ChatColor.GRAY + recipe.description);
        titleLore.add("");
        titleLore.add(ChatColor.YELLOW + "💡 Craft this recipe in a crafting table");
        titleLore.add(ChatColor.YELLOW + "💡 Follow the exact pattern shown");
        titleMeta.setLore(titleLore);
        title.setItemMeta(titleMeta);
        inventory.setItem(13, title);
        
        // Add crafting arrows
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta arrowMeta = arrow.getItemMeta();
        arrowMeta.setDisplayName(ChatColor.GREEN + "→ Crafting Result →");
        arrow.setItemMeta(arrowMeta);
        inventory.setItem(23, arrow);
    }
    
    private void addNavigation() {
        // Previous page
        if (currentPage > 0) {
            ItemStack prevPage = new ItemStack(Material.SPECTRAL_ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            prevMeta.setDisplayName(ChatColor.YELLOW + "← Previous Recipe");
            List<String> prevLore = new ArrayList<>();
            prevLore.add(ChatColor.GRAY + "Go to previous recipe");
            prevLore.add(ChatColor.BLUE + "Page " + currentPage + " of " + recipes.size());
            prevMeta.setLore(prevLore);
            prevPage.setItemMeta(prevMeta);
            inventory.setItem(PREV_PAGE_SLOT, prevPage);
        } else {
            // Disabled previous
            ItemStack disabled = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta disabledMeta = disabled.getItemMeta();
            disabledMeta.setDisplayName(ChatColor.DARK_GRAY + "← No Previous Recipe");
            disabled.setItemMeta(disabledMeta);
            inventory.setItem(PREV_PAGE_SLOT, disabled);
        }
        
        // Next page
        if (currentPage < recipes.size() - 1) {
            ItemStack nextPage = new ItemStack(Material.SPECTRAL_ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            nextMeta.setDisplayName(ChatColor.YELLOW + "Next Recipe →");
            List<String> nextLore = new ArrayList<>();
            nextLore.add(ChatColor.GRAY + "Go to next recipe");
            nextLore.add(ChatColor.BLUE + "Page " + (currentPage + 2) + " of " + recipes.size());
            nextMeta.setLore(nextLore);
            nextPage.setItemMeta(nextMeta);
            inventory.setItem(NEXT_PAGE_SLOT, nextPage);
        } else {
            // Disabled next
            ItemStack disabled = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta disabledMeta = disabled.getItemMeta();
            disabledMeta.setDisplayName(ChatColor.DARK_GRAY + "No Next Recipe →");
            disabled.setItemMeta(disabledMeta);
            inventory.setItem(NEXT_PAGE_SLOT, disabled);
        }
        
        // Back button
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "← Back to Main Menu");
        List<String> backLore = new ArrayList<>();
        backLore.add(ChatColor.GRAY + "Return to the main menu");
        backMeta.setLore(backLore);
        back.setItemMeta(backMeta);
        inventory.setItem(BACK_SLOT, back);
    }
    
    private void addInfoSection() {
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ChatColor.GREEN + "📖 Catalyst Crafting Guide");
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.GRAY + "How to use catalysts:");
        infoLore.add("");
        infoLore.add(ChatColor.AQUA + "• Identification Catalyst" + ChatColor.GRAY + " - Reveals crystal effects");
        infoLore.add(ChatColor.GOLD + "• Socketing Catalyst" + ChatColor.GRAY + " - Embeds crystals in items");
        infoLore.add(ChatColor.LIGHT_PURPLE + "• Fusion Catalyst" + ChatColor.GRAY + " - Combines crystals");
        infoLore.add("");
        infoLore.add(ChatColor.YELLOW + "💡 Use navigation arrows to browse recipes");
        infoLore.add(ChatColor.YELLOW + "💡 Craft in a normal crafting table");
        infoLore.add("");
        infoLore.add(ChatColor.BLUE + "Current: " + ChatColor.WHITE + (currentPage + 1) + "/" + recipes.size());
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        inventory.setItem(INFO_SLOT, info);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        
        event.setCancelled(true); // Cancel all clicks by default
        
        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        switch (slot) {
            case PREV_PAGE_SLOT:
                if (currentPage > 0) {
                    currentPage--;
                    buildMenu();
                }
                break;
                
            case NEXT_PAGE_SLOT:
                if (currentPage < recipes.size() - 1) {
                    currentPage++;
                    buildMenu();
                }
                break;
                
            case BACK_SLOT:
                clicker.closeInventory();
                new dev.lsdmc.arcaniteCrystals.menu.ArcaniteMainMenu(clicker).open();
                break;
                
            default:
                // All other clicks are cancelled
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