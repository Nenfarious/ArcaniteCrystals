package dev.lsdmc.arcaniteCrystals.recipe;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

public class CrystalRecipeManager {
    private static final ArcaniteCrystals plugin = ArcaniteCrystals.getInstance();
    
    public static void registerRecipes() {
        // Basic Crystal Recipe
        registerBasicCrystalRecipe();
        
        // Crystal Fusion Recipe
        registerFusionRecipe();
        
        // Crystal Socket Recipe
        registerSocketRecipe();
        
        // Crystal Identification Recipe
        registerIdentificationRecipe();
    }
    
    private static void registerBasicCrystalRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(
            new NamespacedKey(plugin, "basic_crystal"),
            CrystalManager.createBasicCrystal()
        );
        
        recipe.shape("AAA", "ABA", "AAA");
        recipe.setIngredient('A', Material.AMETHYST_SHARD);
        recipe.setIngredient('B', Material.END_CRYSTAL);
        
        plugin.getServer().addRecipe(recipe);
    }
    
    private static void registerFusionRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(
            new NamespacedKey(plugin, "crystal_fusion"),
            CrystalManager.fuseCrystals(CrystalManager.createBasicCrystal(), CrystalManager.createBasicCrystal())
        );
        
        recipe.shape("AAA", "BCB", "AAA");
        recipe.setIngredient('A', Material.AMETHYST_BLOCK);
        recipe.setIngredient('B', Material.NETHER_STAR);
        recipe.setIngredient('C', Material.END_CRYSTAL);
        
        plugin.getServer().addRecipe(recipe);
    }
    
    private static void registerSocketRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(
            new NamespacedKey(plugin, "crystal_socket"),
            CrystalManager.createBasicCrystal()
        );
        
        recipe.addIngredient(Material.AMETHYST_SHARD);
        recipe.addIngredient(Material.END_CRYSTAL);
        recipe.addIngredient(Material.NETHER_STAR);
        
        plugin.getServer().addRecipe(recipe);
    }
    
    private static void registerIdentificationRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(
            new NamespacedKey(plugin, "crystal_identification"),
            CrystalManager.createBlankCrystal()
        );
        
        recipe.addIngredient(Material.AMETHYST_SHARD);
        recipe.addIngredient(Material.SPYGLASS);
        recipe.addIngredient(Material.GLOWSTONE_DUST);
        
        plugin.getServer().addRecipe(recipe);
    }
    
    public static void unregisterRecipes() {
        plugin.getServer().removeRecipe(new NamespacedKey(plugin, "basic_crystal"));
        plugin.getServer().removeRecipe(new NamespacedKey(plugin, "crystal_fusion"));
        plugin.getServer().removeRecipe(new NamespacedKey(plugin, "crystal_socket"));
        plugin.getServer().removeRecipe(new NamespacedKey(plugin, "crystal_identification"));
    }
} 