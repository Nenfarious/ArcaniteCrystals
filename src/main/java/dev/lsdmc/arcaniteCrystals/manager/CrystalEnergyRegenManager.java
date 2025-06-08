package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages crystal energy regeneration mechanics.
 */
public class CrystalEnergyRegenManager {
    private static final Map<UUID, Map<String, RegenState>> regenStates = new ConcurrentHashMap<>();
    private static final Map<String, BukkitTask> regenTasks = new ConcurrentHashMap<>();
    private static final Logger logger = ArcaniteCrystals.getInstance().getLogger();
    
    private static final NamespacedKey KEY_CRYSTAL_ID = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal_id");
    private static final NamespacedKey KEY_ENERGY = new NamespacedKey(ArcaniteCrystals.getInstance(), "energy");
    private static final NamespacedKey KEY_MAX_ENERGY = new NamespacedKey(ArcaniteCrystals.getInstance(), "max_energy");
    
    private static final int REGEN_CHECK_INTERVAL = 20 * 5; // 5 seconds
    private static final int BASE_REGEN_AMOUNT = 1;
    private static final double REGEN_MULTIPLIER = 1.0;
    
    /**
     * Represents the regeneration state of a crystal.
     */
    public static class RegenState {
        private final String crystalId;
        private final int maxEnergy;
        private int currentEnergy;
        private long lastRegen;
        private boolean isRegenerating;
        
        public RegenState(String crystalId, int maxEnergy) {
            this.crystalId = crystalId;
            this.maxEnergy = maxEnergy;
            this.currentEnergy = maxEnergy;
            this.lastRegen = System.currentTimeMillis();
            this.isRegenerating = false;
        }
        
        public String getCrystalId() { return crystalId; }
        public int getMaxEnergy() { return maxEnergy; }
        public int getCurrentEnergy() { return currentEnergy; }
        public long getLastRegen() { return lastRegen; }
        public boolean isRegenerating() { return isRegenerating; }
        
        public void setCurrentEnergy(int energy) {
            this.currentEnergy = Math.min(maxEnergy, Math.max(0, energy));
        }
        
        public void setLastRegen(long time) {
            this.lastRegen = time;
        }
        
        public void setRegenerating(boolean regenerating) {
            this.isRegenerating = regenerating;
        }
    }
    
    /**
     * Initializes regeneration tracking for a crystal.
     */
    public static void initializeCrystal(UUID playerId, String crystalId, int maxEnergy) {
        Map<String, RegenState> states = regenStates.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        states.put(crystalId, new RegenState(crystalId, maxEnergy));
        
        // Start regen task if not already running
        if (!regenTasks.containsKey(crystalId)) {
            startRegenTask(playerId, crystalId);
        }
    }
    
    /**
     * Starts the regeneration task for a crystal.
     */
    private static void startRegenTask(UUID playerId, String crystalId) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(ArcaniteCrystals.getInstance(), () -> {
            processRegeneration(playerId, crystalId);
        }, REGEN_CHECK_INTERVAL, REGEN_CHECK_INTERVAL);
        
        regenTasks.put(crystalId, task);
    }
    
    /**
     * Processes energy regeneration for a crystal.
     */
    private static void processRegeneration(UUID playerId, String crystalId) {
        Map<String, RegenState> states = regenStates.get(playerId);
        if (states == null) return;
        
        RegenState state = states.get(crystalId);
        if (state == null) return;
        
        if (state.getCurrentEnergy() >= state.getMaxEnergy()) {
            state.setRegenerating(false);
            return;
        }
        
        // Calculate regeneration amount
        int regenAmount = calculateRegenAmount(state);
        state.setCurrentEnergy(state.getCurrentEnergy() + regenAmount);
        state.setLastRegen(System.currentTimeMillis());
        
        // Update crystal item
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            updateCrystalEnergy(player, crystalId, state.getCurrentEnergy());
            
            // Visual and sound effects
            if (regenAmount > 0) {
                ParticleManager.playCrystalRegenEffect(player);
                SoundManager.playCrystalRegenSound(player);
            }
        }
    }
    
    /**
     * Calculates regeneration amount based on crystal state.
     */
    private static int calculateRegenAmount(RegenState state) {
        double amount = BASE_REGEN_AMOUNT * REGEN_MULTIPLIER;
        
        // Add some randomness
        amount *= (0.8 + Math.random() * 0.4);
        
        return (int) Math.ceil(amount);
    }
    
    /**
     * Updates the energy level of a crystal item.
     */
    private static void updateCrystalEnergy(Player player, String crystalId, int energy) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            
            PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
            String itemCrystalId = container.get(KEY_CRYSTAL_ID, PersistentDataType.STRING);
            
            if (crystalId.equals(itemCrystalId)) {
                ItemMeta meta = item.getItemMeta();
                container = meta.getPersistentDataContainer();
                container.set(KEY_ENERGY, PersistentDataType.INTEGER, energy);
                item.setItemMeta(meta);
                break;
            }
        }
    }
    
    /**
     * Gets the regeneration state of a crystal.
     */
    public static RegenState getRegenState(UUID playerId, String crystalId) {
        Map<String, RegenState> states = regenStates.get(playerId);
        return states != null ? states.get(crystalId) : null;
    }
    
    /**
     * Sets the regeneration state of a crystal.
     */
    public static void setRegenState(UUID playerId, String crystalId, boolean regenerating) {
        Map<String, RegenState> states = regenStates.get(playerId);
        if (states == null) return;
        
        RegenState state = states.get(crystalId);
        if (state == null) return;
        
        state.setRegenerating(regenerating);
    }
    
    /**
     * Cleans up regeneration tracking for a crystal.
     */
    public static void cleanupCrystal(UUID playerId, String crystalId) {
        Map<String, RegenState> states = regenStates.get(playerId);
        if (states != null) {
            states.remove(crystalId);
        }
        
        BukkitTask task = regenTasks.remove(crystalId);
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Cleans up all regeneration tracking.
     */
    public static void cleanup() {
        regenStates.clear();
        regenTasks.values().forEach(BukkitTask::cancel);
        regenTasks.clear();
    }
} 