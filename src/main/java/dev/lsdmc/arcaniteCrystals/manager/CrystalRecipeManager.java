package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages crystal-related recipes and catalysts for workshops.
 */
public class CrystalRecipeManager {
    
    private static final ArcaniteCrystals plugin = ArcaniteCrystals.getInstance();
    private static final NamespacedKey KEY_CATALYST_TYPE = new NamespacedKey(ArcaniteCrystals.getInstance(), "catalyst_type");
    
    /**
     * Registers all catalyst recipes with the server.
     */
    public static void registerRecipes() {
        registerIdentificationCatalyst();
        registerSocketingCatalyst();
        registerFusionCatalyst();
        
        plugin.getLogger().info("Registered 3 catalyst recipes successfully");
    }
    
    /**
     * Creates the Identification Catalyst recipe (cheapest - entry level).
     */
    private static void registerIdentificationCatalyst() {
        ItemStack catalyst = createIdentificationCatalyst();
        NamespacedKey key = new NamespacedKey(plugin, "identification_catalyst");
        
        ShapedRecipe recipe = new ShapedRecipe(key, catalyst);
        recipe.shape("GPG", "PEP", "GPG");
        recipe.setIngredient('G', Material.GOLD_NUGGET);
        recipe.setIngredient('P', Material.PAPER);
        recipe.setIngredient('E', Material.EXPERIENCE_BOTTLE);
        
        Bukkit.addRecipe(recipe);
    }
    
    /**
     * Creates the Socketing Catalyst recipe (moderate cost).
     */
    private static void registerSocketingCatalyst() {
        ItemStack catalyst = createSocketingCatalyst();
        NamespacedKey key = new NamespacedKey(plugin, "socketing_catalyst");
        
        ShapedRecipe recipe = new ShapedRecipe(key, catalyst);
        recipe.shape("DGD", "GAG", "DGD");
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('A', Material.ANVIL);
        
        Bukkit.addRecipe(recipe);
    }
    
    /**
     * Creates the Fusion Catalyst recipe (expensive - high tier).
     */
    private static void registerFusionCatalyst() {
        ItemStack catalyst = createFusionCatalyst();
        NamespacedKey key = new NamespacedKey(plugin, "fusion_catalyst");
        
        ShapedRecipe recipe = new ShapedRecipe(key, catalyst);
        recipe.shape("NSN", "SES", "NSN");
        recipe.setIngredient('N', Material.NETHERITE_INGOT);
        recipe.setIngredient('S', Material.NETHER_STAR);
        recipe.setIngredient('E', Material.END_CRYSTAL);
        
        Bukkit.addRecipe(recipe);
    }
    
    /**
     * Creates an Identification Catalyst item.
     */
    public static ItemStack createIdentificationCatalyst() {
        ItemStack catalyst = new ItemStack(Material.SPYGLASS);
        ItemMeta meta = catalyst.getItemMeta();
        
        meta.setDisplayName(ChatColor.AQUA + "Identification Catalyst");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "A magical catalyst used to reveal");
        lore.add(ChatColor.GRAY + "the hidden properties of crystals");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Used in: Crystal Identification");
        lore.add(ChatColor.RED + "⚠ Consumed on use");
        
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(KEY_CATALYST_TYPE, PersistentDataType.STRING, "identification");
        
        catalyst.setItemMeta(meta);
        return catalyst;
    }
    
    /**
     * Creates a Socketing Catalyst item.
     */
    public static ItemStack createSocketingCatalyst() {
        ItemStack catalyst = new ItemStack(Material.SMITHING_TABLE);
        ItemMeta meta = catalyst.getItemMeta();
        
        meta.setDisplayName(ChatColor.GOLD + "Socketing Catalyst");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "A powerful catalyst that enables");
        lore.add(ChatColor.GRAY + "crystals to be embedded into items");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Used in: Crystal Socketing");
        lore.add(ChatColor.RED + "⚠ Consumed on use");
        
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(KEY_CATALYST_TYPE, PersistentDataType.STRING, "socketing");
        
        catalyst.setItemMeta(meta);
        return catalyst;
    }
    
    /**
     * Creates a Fusion Catalyst item.
     */
    public static ItemStack createFusionCatalyst() {
        ItemStack catalyst = new ItemStack(Material.BEACON);
        ItemMeta meta = catalyst.getItemMeta();
        
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Fusion Catalyst");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "An extremely rare catalyst that");
        lore.add(ChatColor.GRAY + "can merge two crystals into one");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Used in: Crystal Fusion");
        lore.add(ChatColor.RED + "⚠ Consumed on use");
        lore.add(ChatColor.DARK_RED + "⚠ Very expensive to craft!");
        
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(KEY_CATALYST_TYPE, PersistentDataType.STRING, "fusion");
        
        catalyst.setItemMeta(meta);
        return catalyst;
    }
    
    /**
     * Checks if an item is an Identification Catalyst.
     */
    public static boolean isIdentificationCatalyst(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        String catalystType = item.getItemMeta().getPersistentDataContainer()
            .get(KEY_CATALYST_TYPE, PersistentDataType.STRING);
        
        return "identification".equals(catalystType);
    }
    
    /**
     * Checks if an item is a Socketing Catalyst.
     */
    public static boolean isSocketingCatalyst(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        String catalystType = item.getItemMeta().getPersistentDataContainer()
            .get(KEY_CATALYST_TYPE, PersistentDataType.STRING);
        
        return "socketing".equals(catalystType);
    }
    
    /**
     * Checks if an item is a Fusion Catalyst.
     */
    public static boolean isFusionCatalyst(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        String catalystType = item.getItemMeta().getPersistentDataContainer()
            .get(KEY_CATALYST_TYPE, PersistentDataType.STRING);
        
        return "fusion".equals(catalystType);
    }
    
    /**
     * Gets the catalyst type of an item.
     */
    public static String getCatalystType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        
        return item.getItemMeta().getPersistentDataContainer()
            .get(KEY_CATALYST_TYPE, PersistentDataType.STRING);
    }
    
    /**
     * Checks if an item is any type of catalyst.
     */
    public static boolean isCatalyst(ItemStack item) {
        return isIdentificationCatalyst(item) || isSocketingCatalyst(item) || isFusionCatalyst(item);
    }
}