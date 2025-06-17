package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Enhanced crystal effect manager with professional effect handling,
 * conflict resolution, and performance optimization.
 */
public class CrystalEffectManager {
    private static final Map<UUID, CrystalEffectSession> activeSessions = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitTask> effectTasks = new ConcurrentHashMap<>();

    private static final Logger logger = ArcaniteCrystals.getInstance().getLogger();
    private static volatile boolean isShutdown = false;

    // Effect application settings
    private static final int EFFECT_TICK_INTERVAL = 20; // 1 second
    private static final int EFFECT_DURATION_BUFFER = 60; // 3 seconds buffer
    private static final int MAX_EFFECT_STACKS = 3; // Maximum effect stacking

    /**
     * Represents an active crystal effect session for a player.
     */
    public static class CrystalEffectSession {
        private final UUID playerId;
        private final Map<String, EffectData> activeEffects;
        private final AtomicBoolean isActive;
        private final long startTime;
        private volatile long lastUpdate;

        public CrystalEffectSession(UUID playerId) {
            this.playerId = playerId;
            this.activeEffects = new ConcurrentHashMap<>();
            this.isActive = new AtomicBoolean(true);
            this.startTime = System.currentTimeMillis();
            this.lastUpdate = startTime;
        }

        public UUID getPlayerId() { return playerId; }
        public Map<String, EffectData> getActiveEffects() { return new HashMap<>(activeEffects); }
        public boolean isActive() { return isActive.get(); }
        public long getStartTime() { return startTime; }
        public long getLastUpdate() { return lastUpdate; }

        public void setActive(boolean active) { isActive.set(active); }
        public void updateLastUpdate() { lastUpdate = System.currentTimeMillis(); }

        public void addEffect(String effectId, EffectData data) {
            activeEffects.put(effectId, data);
            updateLastUpdate();
        }

        public void removeEffect(String effectId) {
            activeEffects.remove(effectId);
            updateLastUpdate();
        }

        public boolean hasEffect(String effectId) {
            return activeEffects.containsKey(effectId);
        }

        public EffectData getEffect(String effectId) {
            return activeEffects.get(effectId);
        }
    }

    /**
     * Represents effect data with stacking and timing information.
     */
    public static class EffectData {
        private final String effectId;
        private final PotionEffectType potionType;
        private final AtomicInteger amplifier;
        private final AtomicInteger stacks;
        private final long appliedTime;
        private volatile long lastApplied;

        public EffectData(String effectId, PotionEffectType potionType, int amplifier) {
            this.effectId = effectId;
            this.potionType = potionType;
            this.amplifier = new AtomicInteger(amplifier);
            this.stacks = new AtomicInteger(1);
            this.appliedTime = System.currentTimeMillis();
            this.lastApplied = appliedTime;
        }

        public String getEffectId() { return effectId; }
        public PotionEffectType getPotionType() { return potionType; }
        public int getAmplifier() { return amplifier.get(); }
        public int getStacks() { return stacks.get(); }
        public long getAppliedTime() { return appliedTime; }
        public long getLastApplied() { return lastApplied; }

        public void setAmplifier(int amp) { amplifier.set(amp); }
        public void addStack() { stacks.incrementAndGet(); }
        public void removeStack() { stacks.decrementAndGet(); }
        public void updateLastApplied() { lastApplied = System.currentTimeMillis(); }
    }

    /**
     * Applies crystal effects to a player with enhanced management.
     */
    public static boolean applyEffects(Player player, List<String> effectIds) {
        if (isShutdown || player == null || effectIds == null || effectIds.isEmpty()) {
            return false;
        }

        UUID playerId = player.getUniqueId();

        try {
            // Cancel existing effect session if any
            cancelEffects(player);

            // Create new effect session
            CrystalEffectSession session = new CrystalEffectSession(playerId);

            // Process and validate effects
            Map<String, EffectData> processedEffects = processEffectList(effectIds);
            if (processedEffects.isEmpty()) {
                logger.warning("No valid effects found for player " + player.getName());
                return false;
            }

            // Add effects to session
            for (Map.Entry<String, EffectData> entry : processedEffects.entrySet()) {
                session.addEffect(entry.getKey(), entry.getValue());
            }

            // Store session and start effect task
            activeSessions.put(playerId, session);
            startEffectTask(player);

            // Play activation effects
            ParticleManager.playCrystalActivationEffect(player);
            SoundManager.playCrystalActivateSound(player);

            // Notify player
            String message = MessageManager.get("crystal.effects-activated")
                    .replace("{count}", String.valueOf(processedEffects.size()));
            MessageManager.sendNotification(player, message, MessageManager.NotificationType.SUCCESS);

            logger.fine("Applied " + processedEffects.size() + " effects to " + player.getName());
            return true;

        } catch (Exception e) {
            logger.severe("Error applying effects to " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Processes a list of effect IDs into validated effect data.
     */
    private static Map<String, EffectData> processEffectList(List<String> effectIds) {
        Map<String, EffectData> processedEffects = new HashMap<>();
        ConfigurationSection upgradesConfig = ConfigManager.getUpgradesConfig()
                .getConfigurationSection("upgrades");

        if (upgradesConfig == null) {
            logger.warning("Upgrades configuration not found");
            return processedEffects;
        }

        for (String effectId : effectIds) {
            try {
                ConfigurationSection upgradeSection = upgradesConfig.getConfigurationSection(effectId);
                if (upgradeSection == null) {
                    logger.warning("Effect configuration not found: " + effectId);
                    continue;
                }

                String effectName = upgradeSection.getString("effect");
                int amplifier = upgradeSection.getInt("amplifier", 0);

                PotionEffectType potionType = getPotionEffectType(effectName);
                if (potionType == null) {
                    logger.warning("Invalid potion effect type: " + effectName + " for " + effectId);
                    continue;
                }

                // Check for conflicts and stacking
                EffectData existingEffect = findConflictingEffect(processedEffects, potionType);
                if (existingEffect != null) {
                    // Handle effect stacking or replacement
                    if (existingEffect.getStacks() < MAX_EFFECT_STACKS) {
                        existingEffect.addStack();
                        existingEffect.setAmplifier(Math.max(existingEffect.getAmplifier(), amplifier));
                        logger.fine("Stacked effect " + effectName + " for better amplifier");
                    } else {
                        logger.fine("Max stacks reached for " + effectName + ", skipping");
                    }
                } else {
                    // Add new effect
                    EffectData effectData = new EffectData(effectId, potionType, amplifier);
                    processedEffects.put(effectId, effectData);
                }

            } catch (Exception e) {
                logger.warning("Error processing effect " + effectId + ": " + e.getMessage());
            }
        }

        return processedEffects;
    }

    /**
     * Finds conflicting effects (same potion type).
     */
    private static EffectData findConflictingEffect(Map<String, EffectData> effects, PotionEffectType type) {
        return effects.values().stream()
                .filter(data -> data.getPotionType().equals(type))
                .findFirst()
                .orElse(null);
    }

    /**
     * Starts the effect application task for a player.
     */
    private static void startEffectTask(Player player) {
        UUID playerId = player.getUniqueId();

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (isShutdown || !player.isOnline()) {
                        cancelEffects(player);
                        return;
                    }

                    CrystalEffectSession session = activeSessions.get(playerId);
                    if (session == null || !session.isActive()) {
                        cancelEffects(player);
                        return;
                    }

                    // Apply all active effects
                    boolean anyEffectApplied = false;
                    for (EffectData effectData : session.getActiveEffects().values()) {
                        if (applyEffectTick(player, effectData)) {
                            anyEffectApplied = true;
                        }
                    }

                    // Update session
                    session.updateLastUpdate();

                    // Play ambient effects occasionally
                    if (anyEffectApplied && Math.random() < 0.1) { // 10% chance
                        ParticleManager.playActiveAuraEffect(player);
                    }

                } catch (Exception e) {
                    logger.warning("Error in effect task for " + player.getName() + ": " + e.getMessage());
                }
            }
        }.runTaskTimer(ArcaniteCrystals.getInstance(), 0L, EFFECT_TICK_INTERVAL);

        effectTasks.put(playerId, task);
    }

    /**
     * Applies a single effect tick to a player.
     */
    private static boolean applyEffectTick(Player player, EffectData effectData) {
        try {
            PotionEffectType type = effectData.getPotionType();
            int amplifier = effectData.getAmplifier();
            int stacks = effectData.getStacks();

            // Calculate effective amplifier with stacking
            int effectiveAmplifier = amplifier + (stacks - 1);
            effectiveAmplifier = Math.min(effectiveAmplifier, 4); // Cap at level 5

            // Apply potion effect with buffer duration
            PotionEffect effect = new PotionEffect(
                    type,
                    EFFECT_DURATION_BUFFER,
                    effectiveAmplifier,
                    true, // ambient (reduced particles)
                    false // show particles (we handle this ourselves)
            );

            player.addPotionEffect(effect, true);
            effectData.updateLastApplied();

            return true;

        } catch (Exception e) {
            logger.warning("Error applying effect " + effectData.getEffectId() +
                    " to " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Cancels all active effects for a player.
     */
    public static void cancelEffects(Player player) {
        if (player == null) return;

        UUID playerId = player.getUniqueId();

        try {
            // Get session before removing
            CrystalEffectSession session = activeSessions.remove(playerId);

            // Cancel effect task
            BukkitTask task = effectTasks.remove(playerId);
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }

            // Remove potion effects
            if (session != null) {
                for (EffectData effectData : session.getActiveEffects().values()) {
                    try {
                        player.removePotionEffect(effectData.getPotionType());
                    } catch (Exception e) {
                        logger.fine("Could not remove effect " + effectData.getEffectId() +
                                " from " + player.getName() + ": " + e.getMessage());
                    }
                }

                session.setActive(false);
            }

            // Play deactivation effects
            if (player.isOnline()) {
                ParticleManager.playCrystalDepletionEffect(player);
                SoundManager.playCrystalDepletionSound(player);

                String message = MessageManager.get("crystal.effects-deactivated");
                MessageManager.sendNotification(player, message, MessageManager.NotificationType.INFO);
            }

            logger.fine("Cancelled effects for player " + player.getName());

        } catch (Exception e) {
            logger.warning("Error cancelling effects for " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Gets the current active effects for a player.
     */
    public static Map<String, Integer> getActiveEffects(Player player) {
        if (player == null) return new HashMap<>();

        CrystalEffectSession session = activeSessions.get(player.getUniqueId());
        if (session == null || !session.isActive()) {
            return new HashMap<>();
        }

        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, EffectData> entry : session.getActiveEffects().entrySet()) {
            EffectData data = entry.getValue();
            result.put(entry.getKey(), data.getAmplifier() + data.getStacks() - 1);
        }

        return result;
    }

    /**
     * Checks if a player has a specific effect active.
     */
    public static boolean hasEffect(Player player, String effectId) {
        if (player == null || effectId == null) return false;

        CrystalEffectSession session = activeSessions.get(player.getUniqueId());
        return session != null && session.isActive() && session.hasEffect(effectId);
    }

    /**
     * Gets the level of a specific effect for a player.
     */
    public static int getEffectLevel(Player player, String effectId) {
        if (player == null || effectId == null) return 0;

        CrystalEffectSession session = activeSessions.get(player.getUniqueId());
        if (session == null || !session.isActive()) return 0;

        EffectData data = session.getEffect(effectId);
        return data != null ? data.getAmplifier() + data.getStacks() - 1 : 0;
    }

    /**
     * Adds a single effect to an existing session.
     */
    public static boolean addEffect(Player player, String effectId) {
        if (player == null || effectId == null) return false;

        UUID playerId = player.getUniqueId();
        CrystalEffectSession session = activeSessions.get(playerId);

        if (session == null || !session.isActive()) {
            // No active session, apply as new session
            return applyEffects(player, Collections.singletonList(effectId));
        }

        // Process the new effect
        Map<String, EffectData> processedEffects = processEffectList(Collections.singletonList(effectId));
        if (processedEffects.isEmpty()) return false;

        // Add to existing session
        EffectData newEffect = processedEffects.values().iterator().next();
        session.addEffect(effectId, newEffect);

        logger.fine("Added effect " + effectId + " to existing session for " + player.getName());
        return true;
    }

    /**
     * Removes a single effect from an existing session.
     */
    public static boolean removeEffect(Player player, String effectId) {
        if (player == null || effectId == null) return false;

        UUID playerId = player.getUniqueId();
        CrystalEffectSession session = activeSessions.get(playerId);

        if (session == null || !session.isActive()) return false;

        EffectData effectData = session.getEffect(effectId);
        if (effectData == null) return false;

        // Remove potion effect from player
        try {
            player.removePotionEffect(effectData.getPotionType());
        } catch (Exception e) {
            logger.fine("Could not remove potion effect for " + effectId + ": " + e.getMessage());
        }

        // Remove from session
        session.removeEffect(effectId);

        // If no effects remain, cancel the entire session
        if (session.getActiveEffects().isEmpty()) {
            cancelEffects(player);
        }

        logger.fine("Removed effect " + effectId + " from " + player.getName());
        return true;
    }

    /**
     * Gets a PotionEffectType from a string name.
     */
    private static PotionEffectType getPotionEffectType(String effectName) {
        if (effectName == null) return null;

        return switch (effectName.toLowerCase()) {
            case "strength" -> PotionEffectType.STRENGTH;
            case "speed" -> PotionEffectType.SPEED;
            case "jump_boost", "jump" -> PotionEffectType.JUMP_BOOST;
            case "regeneration", "regen" -> PotionEffectType.REGENERATION;
            case "resistance", "damage_resistance" -> PotionEffectType.RESISTANCE;
            case "fire_resistance" -> PotionEffectType.FIRE_RESISTANCE;
            case "water_breathing" -> PotionEffectType.WATER_BREATHING;
            case "night_vision" -> PotionEffectType.NIGHT_VISION;
            case "invisibility" -> PotionEffectType.INVISIBILITY;
            case "glowing" -> PotionEffectType.GLOWING;
            case "haste" -> PotionEffectType.HASTE;
            case "mining_fatigue" -> PotionEffectType.MINING_FATIGUE;
            case "poison" -> PotionEffectType.POISON;
            case "wither" -> PotionEffectType.WITHER;
            case "health_boost" -> PotionEffectType.HEALTH_BOOST;
            case "absorption" -> PotionEffectType.ABSORPTION;
            case "saturation" -> PotionEffectType.SATURATION;
            case "luck" -> PotionEffectType.LUCK;
            case "unluck", "bad_luck" -> PotionEffectType.UNLUCK;
            case "slow_falling" -> PotionEffectType.SLOW_FALLING;
            case "conduit_power" -> PotionEffectType.CONDUIT_POWER;
            case "dolphins_grace" -> PotionEffectType.DOLPHINS_GRACE;
            default -> {
                // Try direct lookup
                try {
                    yield PotionEffectType.getByName(effectName.toUpperCase());
                } catch (Exception e) {
                    logger.warning("Unknown potion effect type: " + effectName);
                    yield null;
                }
            }
        };
    }

    /**
     * Gets session information for a player.
     */
    public static CrystalEffectSession getSession(Player player) {
        if (player == null) return null;
        return activeSessions.get(player.getUniqueId());
    }

    /**
     * Gets all active sessions.
     */
    public static Map<UUID, CrystalEffectSession> getAllSessions() {
        return new HashMap<>(activeSessions);
    }

    /**
     * Cleans up expired sessions.
     */
    public static void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        long maxSessionAge = 30 * 60 * 1000; // 30 minutes

        Iterator<Map.Entry<UUID, CrystalEffectSession>> iterator = activeSessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, CrystalEffectSession> entry = iterator.next();
            CrystalEffectSession session = entry.getValue();

            if (currentTime - session.getLastUpdate() > maxSessionAge) {
                logger.fine("Cleaning up expired session for " + entry.getKey());

                // Cancel task
                BukkitTask task = effectTasks.remove(entry.getKey());
                if (task != null && !task.isCancelled()) {
                    task.cancel();
                }

                // Remove session
                session.setActive(false);
                iterator.remove();
            }
        }
    }

    /**
     * Gets statistics about the effect system.
     */
    public static String getStats() {
        int sessionCount = CrystalEffectManager.activeSessions.size();
        int activeTasks = effectTasks.size();
        int totalEffects = activeSessions.values().stream()
                .mapToInt(session -> session.getActiveEffects().size())
                .sum();

        return String.format("Active sessions: %d, Active tasks: %d, Total effects: %d",
                sessionCount, activeTasks, totalEffects);
    }

    /**
     * Forces cleanup of all effects and sessions.
     * Should be called on plugin disable.
     */
    public static void cleanup() {
        isShutdown = true;

        logger.info("Cleaning up crystal effect manager...");

        // Cancel all tasks
        for (BukkitTask task : effectTasks.values()) {
            if (task != null && !task.isCancelled()) {
                try {
                    task.cancel();
                } catch (Exception e) {
                    logger.warning("Error cancelling effect task during cleanup: " + e.getMessage());
                }
            }
        }

        // Deactivate all sessions
        for (CrystalEffectSession session : activeSessions.values()) {
            try {
                session.setActive(false);
            } catch (Exception e) {
                logger.warning("Error deactivating session during cleanup: " + e.getMessage());
            }
        }

        // Clear all data
        effectTasks.clear();
        activeSessions.clear();

        logger.info("Crystal effect manager cleanup complete");
    }

    /**
     * Emergency stop for all effects (admin command).
     */
    public static void emergencyStop() {
        logger.warning("Emergency stop initiated for crystal effects");

        for (UUID playerId : new HashSet<>(activeSessions.keySet())) {
            Player player = ArcaniteCrystals.getInstance().getServer().getPlayer(playerId);
            if (player != null) {
                cancelEffects(player);
            }
        }

        logger.warning("Emergency stop completed");
    }
}