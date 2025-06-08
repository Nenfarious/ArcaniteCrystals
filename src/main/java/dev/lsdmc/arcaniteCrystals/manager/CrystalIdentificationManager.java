package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages crystal identification with skill progression and multiple identification methods.
 */
public class CrystalIdentificationManager {
    private static final Map<UUID, AtomicInteger> identificationSkill = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> identificationCooldowns = new ConcurrentHashMap<>();
    
    private static final int BASE_SUCCESS_CHANCE = 60;
    private static final int SKILL_BONUS_PER_LEVEL = 2;
    private static final int MAX_SKILL_LEVEL = 50;
    private static final long COOLDOWN_DURATION = 300000; // 5 minutes
    
    /**
     * Result of a crystal identification attempt.
     */
    public static class IdentificationResult {
        private final boolean success;
        private final ItemStack identifiedCrystal;
        private final String message;
        private final int quality;
        
        public IdentificationResult(boolean success, ItemStack crystal, String message, int quality) {
            this.success = success;
            this.identifiedCrystal = crystal;
            this.message = message;
            this.quality = quality;
        }
        
        public boolean isSuccess() { return success; }
        public ItemStack getIdentifiedCrystal() { return identifiedCrystal; }
        public String getMessage() { return message; }
        public int getQuality() { return quality; }
    }
    
    /**
     * Different methods of crystal identification.
     */
    public enum IdentificationMethod {
        BASIC_SCROLL("Basic Scroll", 1.0),
        EXPERT_ANALYSIS("Expert Analysis", 1.5),
        MAGICAL_INSIGHT("Magical Insight", 2.0),
        DIVINE_REVELATION("Divine Revelation", 3.0);
        
        private final String displayName;
        private final double qualityMultiplier;
        
        IdentificationMethod(String displayName, double qualityMultiplier) {
            this.displayName = displayName;
            this.qualityMultiplier = qualityMultiplier;
        }
        
        public String getDisplayName() { return displayName; }
        public double getQualityMultiplier() { return qualityMultiplier; }
    }
    
    /**
     * Attempts to identify a crystal using the specified method.
     */
    public static IdentificationResult identifyCrystal(Player player, ItemStack blankCrystal, IdentificationMethod method) {
        UUID playerId = player.getUniqueId();
        
        // Check cooldown
        if (isOnCooldown(playerId)) {
            long remaining = getRemainingCooldown(playerId);
            return new IdentificationResult(false, blankCrystal, 
                "You must wait " + formatTime(remaining) + " before identifying another crystal.", 0);
        }
        
        // Check identification skill
        int skillLevel = getIdentificationSkill(playerId);
        if (skillLevel < method.ordinal() * 10) {
            return new IdentificationResult(false, blankCrystal,
                "You need " + (method.ordinal() * 10) + " identification skill to use " + method.getDisplayName(), 0);
        }
        
        // Calculate success chance
        int successChance = calculateSuccessChance(skillLevel, method);
        boolean success = new Random().nextInt(100) < successChance;
        
        if (success) {
            // Generate identified crystal
            ItemStack identifiedCrystal = generateIdentifiedCrystal(player, method);
            
            // Award skill experience
            awardSkillExperience(playerId, method);
            
            // Set cooldown
            setCooldown(playerId);
            
            // Play success effects
            SoundManager.playCrystalActivateSound(player);
            ParticleManager.playCrystalActivationEffect(player);
            
            return new IdentificationResult(true, identifiedCrystal,
                "Successfully identified crystal using " + method.getDisplayName(), 
                calculateQuality(skillLevel, method));
        } else {
            // Handle failure
            handleIdentificationFailure(player, blankCrystal, method);
            
            // Set cooldown
            setCooldown(playerId);
            
            return new IdentificationResult(false, blankCrystal,
                "Failed to identify crystal. The crystal's energy has been disrupted.", 0);
        }
    }
    
    /**
     * Gets the player's identification skill level.
     */
    public static int getIdentificationSkill(UUID playerId) {
        return identificationSkill.computeIfAbsent(playerId, k -> new AtomicInteger(0)).get();
    }
    
    /**
     * Awards skill experience for successful identification.
     */
    private static void awardSkillExperience(UUID playerId, IdentificationMethod method) {
        AtomicInteger skill = identificationSkill.computeIfAbsent(playerId, k -> new AtomicInteger(0));
        int currentLevel = skill.get();
        
        if (currentLevel < MAX_SKILL_LEVEL) {
            int expGain = method.ordinal() + 1;
            int newLevel = Math.min(currentLevel + expGain, MAX_SKILL_LEVEL);
            skill.set(newLevel);
        }
    }
    
    /**
     * Calculates success chance based on skill and method.
     */
    private static int calculateSuccessChance(int skillLevel, IdentificationMethod method) {
        int baseChance = BASE_SUCCESS_CHANCE + (skillLevel * SKILL_BONUS_PER_LEVEL);
        return Math.min(baseChance, 95); // Cap at 95%
    }
    
    /**
     * Calculates crystal quality based on skill and method.
     */
    private static int calculateQuality(int skillLevel, IdentificationMethod method) {
        int baseQuality = 50 + (skillLevel / 2);
        return (int) (baseQuality * method.getQualityMultiplier());
    }
    
    /**
     * Generates an identified crystal with random effects.
     */
    private static ItemStack generateIdentifiedCrystal(Player player, IdentificationMethod method) {
        // Get available upgrades based on player's level
        Set<String> availableUpgrades = PlayerDataManager.getUnlockedUpgrades(player.getUniqueId());
        if (availableUpgrades.isEmpty()) {
            availableUpgrades = Set.of("speed", "jump_boost", "haste"); // Default effects
        }
        
        // Select random effects based on method quality
        int effectCount = 1 + method.ordinal();
        List<String> selectedEffects = new ArrayList<>(availableUpgrades);
        Collections.shuffle(selectedEffects);
        selectedEffects = selectedEffects.subList(0, Math.min(effectCount, selectedEffects.size()));
        
        // Create crystal with selected effects
        return CrystalManager.createMysteryCrystal(player, selectedEffects);
    }
    
    /**
     * Handles identification failure.
     */
    private static void handleIdentificationFailure(Player player, ItemStack crystal, IdentificationMethod method) {
        // Chance to destroy crystal based on method
        if (new Random().nextInt(100) < (method.ordinal() * 10)) {
            crystal.setAmount(0);
            MessageManager.sendNotification(player, "The crystal shattered during identification!", 
                MessageManager.NotificationType.ERROR);
        } else {
            // Reduce crystal energy
            int currentEnergy = CrystalManager.getEnergy(crystal);
            int newEnergy = currentEnergy / 2;
            CrystalManager.setEnergy(crystal, newEnergy);
        }
        
        // Play failure effects
        SoundManager.playErrorSound(player);
        ParticleManager.playErrorEffect(player);
    }
    
    /**
     * Checks if player is on identification cooldown.
     */
    private static boolean isOnCooldown(UUID playerId) {
        Long cooldown = identificationCooldowns.get(playerId);
        return cooldown != null && cooldown > System.currentTimeMillis();
    }
    
    /**
     * Gets remaining cooldown time in milliseconds.
     */
    private static long getRemainingCooldown(UUID playerId) {
        Long cooldown = identificationCooldowns.get(playerId);
        if (cooldown == null) return 0;
        return Math.max(0, cooldown - System.currentTimeMillis());
    }
    
    /**
     * Sets identification cooldown for player.
     */
    private static void setCooldown(UUID playerId) {
        identificationCooldowns.put(playerId, System.currentTimeMillis() + COOLDOWN_DURATION);
    }
    
    /**
     * Formats time in milliseconds to readable string.
     */
    private static String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        if (seconds < 60) return seconds + " seconds";
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return minutes + " minutes and " + seconds + " seconds";
    }
    
    /**
     * Cleans up all cooldowns and skill data.
     */
    public static void cleanup() {
        identificationCooldowns.clear();
        identificationSkill.clear();
    }
} 