package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages crystal fusion mechanics.
 */
public class CrystalFusionManager {
    private static final Map<UUID, Map<String, FusionState>> fusionStates = new ConcurrentHashMap<>();
    private static final Logger logger = ArcaniteCrystals.getInstance().getLogger();
    
    private static final NamespacedKey KEY_CRYSTAL_ID = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal_id");
    private static final NamespacedKey KEY_FUSION_LEVEL = new NamespacedKey(ArcaniteCrystals.getInstance(), "fusion_level");
    
    private static final int MAX_FUSION_LEVEL = 5;
    private static final double BASE_FUSION_SUCCESS = 0.6; // 60% base success rate
    private static final double FUSION_DESTROY_CHANCE = 0.2; // 20% chance to destroy both crystals on failure
    
    /**
     * Represents the state of a crystal fusion.
     */
    public static class FusionState {
        private final String crystalId1;
        private final String crystalId2;
        private final int level;
        private final long timestamp;
        
        public FusionState(String crystalId1, String crystalId2, int level) {
            this.crystalId1 = crystalId1;
            this.crystalId2 = crystalId2;
            this.level = level;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getCrystalId1() { return crystalId1; }
        public String getCrystalId2() { return crystalId2; }
        public int getLevel() { return level; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Attempts to fuse two crystals together.
     */
    public static boolean fuseCrystals(Player player, ItemStack crystal1, ItemStack crystal2) {
        if (!isValidCrystal(crystal1) || !isValidCrystal(crystal2)) {
            player.sendMessage("§cBoth items must be valid crystals!");
            return false;
        }
        
        String crystalId1 = getCrystalId(crystal1);
        String crystalId2 = getCrystalId(crystal2);
        
        if (crystalId1 == null || crystalId2 == null) {
            player.sendMessage("§cInvalid crystal IDs!");
            return false;
        }
        
        int level1 = getFusionLevel(crystal1);
        int level2 = getFusionLevel(crystal2);
        
        if (level1 >= MAX_FUSION_LEVEL || level2 >= MAX_FUSION_LEVEL) {
            player.sendMessage("§cOne or both crystals have reached the maximum fusion level!");
            return false;
        }
        
        // Calculate success chance
        double successChance = calculateFusionSuccess(player, crystal1, crystal2);
        
        if (Math.random() < successChance) {
            // Success
            int newLevel = Math.max(level1, level2) + 1;
            setFusionLevel(crystal1, newLevel);
            
            // Update crystal effects
            updateCrystalEffects(crystal1, crystal2);
            
            // Visual and sound effects
            ParticleManager.playCrystalFusionEffect(player);
            SoundManager.playCrystalFusionSound(player);
            
            // Record fusion
            recordFusion(player.getUniqueId(), crystalId1, crystalId2, newLevel);
            
            player.sendMessage("§aSuccessfully fused the crystals! New level: " + newLevel);
            return true;
        } else {
            // Failure
            if (Math.random() < FUSION_DESTROY_CHANCE) {
                player.sendMessage("§cBoth crystals were destroyed in the process!");
                crystal1.setAmount(0);
                crystal2.setAmount(0);
            } else {
                player.sendMessage("§cFailed to fuse the crystals!");
            }
            
            // Visual and sound effects
            ParticleManager.playCrystalFusionFailEffect(player);
            SoundManager.playCrystalFusionFailSound(player);
            
            return false;
        }
    }
    
    /**
     * Gets the fusion level of a crystal.
     */
    public static int getFusionLevel(ItemStack crystal) {
        if (!crystal.hasItemMeta()) return 0;
        
        PersistentDataContainer container = crystal.getItemMeta().getPersistentDataContainer();
        return container.getOrDefault(KEY_FUSION_LEVEL, PersistentDataType.INTEGER, 0);
    }
    
    /**
     * Sets the fusion level of a crystal.
     */
    private static void setFusionLevel(ItemStack crystal, int level) {
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) return;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(KEY_FUSION_LEVEL, PersistentDataType.INTEGER, level);
        crystal.setItemMeta(meta);
    }
    
    /**
     * Records a successful fusion.
     */
    private static void recordFusion(UUID playerId, String crystalId1, String crystalId2, int level) {
        Map<String, FusionState> states = fusionStates.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        String fusionId = crystalId1 + "_" + crystalId2;
        states.put(fusionId, new FusionState(crystalId1, crystalId2, level));
    }
    
    /**
     * Gets the fusion history for a player.
     */
    public static List<FusionState> getFusionHistory(UUID playerId) {
        Map<String, FusionState> states = fusionStates.get(playerId);
        return states != null ? new ArrayList<>(states.values()) : new ArrayList<>();
    }
    
    /**
     * Checks if an item is a valid crystal.
     */
    private static boolean isValidCrystal(ItemStack item) {
        if (item == null) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(KEY_CRYSTAL_ID, PersistentDataType.STRING);
    }
    
    /**
     * Gets the unique ID of a crystal.
     */
    private static String getCrystalId(ItemStack crystal) {
        if (!crystal.hasItemMeta()) return null;
        
        PersistentDataContainer container = crystal.getItemMeta().getPersistentDataContainer();
        return container.get(KEY_CRYSTAL_ID, PersistentDataType.STRING);
    }
    
    /**
     * Calculates the success chance for fusion.
     */
    private static double calculateFusionSuccess(Player player, ItemStack crystal1, ItemStack crystal2) {
        double chance = BASE_FUSION_SUCCESS;
        
        // Add player level bonus
        int playerLevel = player.getLevel();
        chance += playerLevel * 0.01; // 1% per level
        
        // Add crystal level bonus
        int level1 = getFusionLevel(crystal1);
        int level2 = getFusionLevel(crystal2);
        chance -= (level1 + level2) * 0.05; // -5% per level
        
        // Add crystal quality bonus
        if (crystal1.hasItemMeta() && crystal1.getItemMeta().hasEnchants()) {
            chance += 0.1; // 10% for enchanted crystals
        }
        if (crystal2.hasItemMeta() && crystal2.getItemMeta().hasEnchants()) {
            chance += 0.1; // 10% for enchanted crystals
        }
        
        return Math.min(0.95, Math.max(0.1, chance)); // Cap between 10% and 95%
    }
    
    /**
     * Updates the effects of the fused crystal.
     */
    private static void updateCrystalEffects(ItemStack crystal1, ItemStack crystal2) {
        List<String> effects1 = CrystalManager.getCrystalEffects(crystal1);
        List<String> effects2 = CrystalManager.getCrystalEffects(crystal2);
        
        // Combine effects
        Set<String> combined = new HashSet<>(effects1);
        combined.addAll(effects2);
        
        // Convert to list and shuffle
        List<String> result = new ArrayList<>(combined);
        Collections.shuffle(result);
        
        // Limit to max effects based on crystal type
        CrystalManager.CrystalType type = CrystalManager.getCrystalType(crystal1);
        if (result.size() > type.getMaxEffects()) {
            result = result.subList(0, type.getMaxEffects());
        }
        
        // Update crystal effects
        CrystalManager.setCrystalEffects(crystal1, result);
    }
    
    /**
     * Cleans up all fusion data.
     */
    public static void cleanup() {
        fusionStates.clear();
    }
} 