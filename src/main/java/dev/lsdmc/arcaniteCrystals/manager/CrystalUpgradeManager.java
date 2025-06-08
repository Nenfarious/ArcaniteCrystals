package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CrystalUpgradeManager {
    private static final Random random = new Random();
    private static final int MAX_UPGRADE_ATTEMPTS = 3;
    private static final double BASE_SUCCESS_CHANCE = 0.5;
    private static final double TIER_MULTIPLIER = 0.1;
    
    /**
     * Attempts to upgrade a crystal.
     */
    public static boolean attemptUpgrade(Player player, ItemStack crystal, ItemStack upgradeMaterial) {
        if (!isValidUpgradeMaterial(upgradeMaterial)) {
            MessageManager.sendNotification(player, 
                "Invalid upgrade material!", 
                MessageManager.NotificationType.ERROR);
            return false;
        }
        
        int currentTier = CrystalManager.getCrystalTier(crystal);
        if (currentTier >= 4) {
            MessageManager.sendNotification(player, 
                "This crystal has reached maximum tier!", 
                MessageManager.NotificationType.ERROR);
            return false;
        }
        
        double successChance = calculateSuccessChance(currentTier);
        if (random.nextDouble() < successChance) {
            upgradeCrystal(player, crystal);
            return true;
        } else {
            handleFailedUpgrade(player, crystal);
            return false;
        }
    }
    
    /**
     * Checks if an item is a valid upgrade material.
     */
    private static boolean isValidUpgradeMaterial(ItemStack material) {
        return material != null && (
            material.getType() == Material.NETHERITE_INGOT ||
            material.getType() == Material.DIAMOND_BLOCK ||
            material.getType() == Material.EMERALD_BLOCK
        );
    }
    
    /**
     * Calculates the success chance for an upgrade attempt.
     */
    private static double calculateSuccessChance(int currentTier) {
        return BASE_SUCCESS_CHANCE - (currentTier * TIER_MULTIPLIER);
    }
    
    /**
     * Upgrades a crystal to the next tier.
     */
    private static void upgradeCrystal(Player player, ItemStack crystal) {
        int currentTier = CrystalManager.getCrystalTier(crystal);
        int newTier = currentTier + 1;
        
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) {
            return;
        }
        
        // Update tier
        meta.getPersistentDataContainer().set(
            CrystalManager.KEY_TIER,
            PersistentDataType.INTEGER,
            newTier
        );
        
        // Add new random effect
        List<String> effects = CrystalManager.getCrystalEffects(crystal);
        effects.add(CrystalManager.getRandomEffect());
        
        meta.getPersistentDataContainer().set(
            CrystalManager.KEY_ABILITIES,
            PersistentDataType.STRING,
            String.join(",", effects)
        );
        
        crystal.setItemMeta(meta);
        
        // Update crystal lore
        CrystalManager.updateCrystalLore(
            crystal,
            meta,
            effects,
            CrystalManager.getCurrentEnergy(crystal),
            CrystalManager.getMaxEnergy(crystal)
        );
        
        // Play success effects
        ParticleManager.playUpgradeEffect(player);
        SoundManager.playUpgradeUnlockSound(player);
        
        MessageManager.sendNotification(player, 
            "Crystal upgraded to tier " + newTier + "!", 
            MessageManager.NotificationType.SUCCESS);
    }
    
    /**
     * Handles a failed upgrade attempt.
     */
    private static void handleFailedUpgrade(Player player, ItemStack crystal) {
        // Reduce energy
        int currentEnergy = CrystalManager.getCurrentEnergy(crystal);
        int maxEnergy = CrystalManager.getMaxEnergy(crystal);
        int newEnergy = Math.max(0, currentEnergy - (maxEnergy / 4));
        
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) {
            return;
        }
        
        meta.getPersistentDataContainer().set(
            CrystalManager.KEY_ENERGY,
            PersistentDataType.INTEGER,
            newEnergy
        );
        
        crystal.setItemMeta(meta);
        
        // Update crystal lore
        CrystalManager.updateCrystalLore(
            crystal,
            meta,
            CrystalManager.getCrystalEffects(crystal),
            newEnergy,
            maxEnergy
        );
        
        // Play failure effects
        ParticleManager.playCrystalDepletionEffect(player);
        SoundManager.playErrorSound(player);
        
        MessageManager.sendNotification(player, 
            "Upgrade failed! Crystal energy reduced.", 
            MessageManager.NotificationType.ERROR);
    }
    
    /**
     * Gets the required materials for upgrading a crystal.
     */
    public static List<ItemStack> getRequiredMaterials(int currentTier) {
        List<ItemStack> materials = new ArrayList<>();
        
        switch (currentTier) {
            case 1 -> {
                materials.add(new ItemStack(Material.NETHERITE_INGOT, 2));
                materials.add(new ItemStack(Material.DIAMOND_BLOCK, 1));
            }
            case 2 -> {
                materials.add(new ItemStack(Material.NETHERITE_INGOT, 4));
                materials.add(new ItemStack(Material.EMERALD_BLOCK, 2));
            }
            case 3 -> {
                materials.add(new ItemStack(Material.NETHERITE_INGOT, 8));
                materials.add(new ItemStack(Material.EMERALD_BLOCK, 4));
                materials.add(new ItemStack(Material.DIAMOND_BLOCK, 2));
            }
        }
        
        return materials;
    }
} 