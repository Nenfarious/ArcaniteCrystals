package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages crystal decay and corruption mechanics with proper tracking and effects.
 */
public class CrystalDecayManager {
    private static final Map<UUID, DecayState> decayStates = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitTask> decayTasks = new ConcurrentHashMap<>();
    
    private static final NamespacedKey KEY_DECAY_LEVEL = new NamespacedKey(ArcaniteCrystals.getInstance(), "decay_level");
    private static final NamespacedKey KEY_CORRUPTION_LEVEL = new NamespacedKey(ArcaniteCrystals.getInstance(), "corruption_level");
    
    private static final Random random = new Random();
    private final FileConfiguration config;
    private final int decayInterval;
    private final double decayChance;
    private final double corruptionChance;
    private final int maxDecayLevel;
    private final int maxCorruptionLevel;
    
    public CrystalDecayManager() {
        this.config = ConfigManager.getConfig();
        this.decayInterval = config.getInt("crystal.decay.interval", 300); // 5 minutes
        this.decayChance = config.getDouble("crystal.decay.chance", 0.1); // 10%
        this.corruptionChance = config.getDouble("crystal.decay.corruption-chance", 0.05); // 5%
        this.maxDecayLevel = config.getInt("crystal.decay.max-level", 5);
        this.maxCorruptionLevel = config.getInt("crystal.decay.max-corruption", 3);
    }
    
    /**
     * Initializes decay tracking for a player's crystal.
     */
    public void initializeDecay(Player player, ItemStack crystal) {
        UUID playerId = player.getUniqueId();
        
        // Create new decay state
        DecayState state = new DecayState();
        decayStates.put(playerId, state);
        
        // Start decay task
        startDecayTask(player);
    }
    
    /**
     * Starts the decay task for a player.
     */
    private void startDecayTask(Player player) {
        UUID playerId = player.getUniqueId();
        
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(ArcaniteCrystals.getInstance(), () -> {
            DecayState state = decayStates.get(playerId);
            if (state == null) return;
            
            // Check for decay
            if (random.nextDouble() < decayChance) {
                processDecay(player);
            }
            
            // Check for corruption
            if (state.decayLevel >= maxDecayLevel && random.nextDouble() < corruptionChance) {
                processCorruption(player);
            }
        }, decayInterval * 20L, decayInterval * 20L);
        
        decayTasks.put(playerId, task);
    }
    
    /**
     * Processes decay for a player's crystal.
     */
    private void processDecay(Player player) {
        UUID playerId = player.getUniqueId();
        DecayState state = decayStates.get(playerId);
        if (state == null) return;
        
        // Increase decay level
        state.decayLevel = Math.min(state.decayLevel + 1, maxDecayLevel);
        
        // Update crystal
        updateCrystalDecay(player, state);
        
        // Play effects
        ParticleManager.playCrystalDecayEffect(player);
        SoundManager.playCrystalDecaySound(player);
        
        // Notify player
        MessageManager.sendNotification(player, 
            MessageManager.get("crystal.decay").replace("%level%", String.valueOf(state.decayLevel)),
            MessageManager.NotificationType.WARNING);
    }
    
    /**
     * Processes corruption for a player's crystal.
     */
    private void processCorruption(Player player) {
        UUID playerId = player.getUniqueId();
        DecayState state = decayStates.get(playerId);
        if (state == null) return;
        
        // Increase corruption level
        state.corruptionLevel = Math.min(state.corruptionLevel + 1, maxCorruptionLevel);
        
        // Update crystal
        updateCrystalCorruption(player, state);
        
        // Play effects
        ParticleManager.playCrystalCorruptionEffect(player);
        SoundManager.playCrystalCorruptionSound(player);
        
        // Notify player
        MessageManager.sendNotification(player, 
            MessageManager.get("crystal.corruption").replace("%level%", String.valueOf(state.corruptionLevel)),
            MessageManager.NotificationType.ERROR);
        
        // Check for crystal destruction
        if (state.corruptionLevel >= maxCorruptionLevel) {
            destroyCrystal(player);
        }
    }
    
    /**
     * Updates a crystal's decay level.
     */
    private void updateCrystalDecay(Player player, DecayState state) {
        ItemStack crystal = getPlayerCrystal(player);
        if (crystal == null) return;
        
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) return;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(KEY_DECAY_LEVEL, PersistentDataType.INTEGER, state.decayLevel);
        
        // Update lore
        updateCrystalLore(meta, state);
        
        crystal.setItemMeta(meta);
    }
    
    /**
     * Updates a crystal's corruption level.
     */
    private void updateCrystalCorruption(Player player, DecayState state) {
        ItemStack crystal = getPlayerCrystal(player);
        if (crystal == null) return;
        
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) return;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(KEY_CORRUPTION_LEVEL, PersistentDataType.INTEGER, state.corruptionLevel);
        
        // Update lore
        updateCrystalLore(meta, state);
        
        crystal.setItemMeta(meta);
    }
    
    /**
     * Updates a crystal's lore to show decay and corruption levels.
     */
    private void updateCrystalLore(ItemMeta meta, DecayState state) {
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        
        // Remove old decay/corruption lore
        lore.removeIf(line -> line.contains("Decay:") || line.contains("Corruption:"));
        
        // Add new decay lore
        lore.add(ChatColor.GRAY + "Decay: " + getDecayColor(state.decayLevel) + 
                String.valueOf(state.decayLevel) + "/" + maxDecayLevel);
        
        // Add corruption lore if any
        if (state.corruptionLevel > 0) {
            lore.add(ChatColor.DARK_RED + "Corruption: " + 
                    String.valueOf(state.corruptionLevel) + "/" + maxCorruptionLevel);
        }
        
        meta.setLore(lore);
    }
    
    /**
     * Gets the color for decay level display.
     */
    private ChatColor getDecayColor(int level) {
        return switch (level) {
            case 0 -> ChatColor.GREEN;
            case 1, 2 -> ChatColor.YELLOW;
            case 3, 4 -> ChatColor.RED;
            default -> ChatColor.DARK_RED;
        };
    }
    
    /**
     * Destroys a corrupted crystal.
     */
    private void destroyCrystal(Player player) {
        ItemStack crystal = getPlayerCrystal(player);
        if (crystal == null) return;
        
        // Remove crystal
        player.getInventory().remove(crystal);
        
        // Play effects
        ParticleManager.playCrystalDestructionEffect(player);
        SoundManager.playCrystalDestructionSound(player);
        
        // Notify player
        MessageManager.sendNotification(player, 
            MessageManager.get("crystal.destroyed"),
            MessageManager.NotificationType.ERROR);
        
        // Clean up decay state
        cleanupDecay(player);
    }
    
    /**
     * Gets a player's crystal.
     */
    private ItemStack getPlayerCrystal(Player player) {
        Material crystalMaterial = Material.valueOf(config.getString("crystal.material", "DIAMOND"));
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == crystalMaterial && CrystalManager.isCrystal(item)) {
                return item;
            }
        }
        return null;
    }
    
    /**
     * Cleans up decay tracking for a player.
     */
    public void cleanupDecay(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel decay task
        BukkitTask task = decayTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        
        // Remove decay state
        decayStates.remove(playerId);
    }
    
    /**
     * Gets the decay level of a crystal.
     */
    public static int getDecayLevel(ItemStack crystal) {
        if (!CrystalManager.isCrystal(crystal)) return 0;
        
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) return 0;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.getOrDefault(KEY_DECAY_LEVEL, PersistentDataType.INTEGER, 0);
    }
    
    /**
     * Gets the corruption level of a crystal.
     */
    public static int getCorruptionLevel(ItemStack crystal) {
        if (!CrystalManager.isCrystal(crystal)) return 0;
        
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) return 0;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.getOrDefault(KEY_CORRUPTION_LEVEL, PersistentDataType.INTEGER, 0);
    }
    
    /**
     * Cleans up all decay tracking.
     * Should be called on plugin disable.
     */
    public void cleanup() {
        // Cancel all decay tasks
        decayTasks.values().forEach(BukkitTask::cancel);
        decayTasks.clear();
        
        // Clear all decay states
        decayStates.clear();
    }
    
    /**
     * Holds the decay state for a crystal.
     */
    private static class DecayState {
        int decayLevel = 0;
        int corruptionLevel = 0;
    }
} 