package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.database.DatabaseManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central registry for tracking all crystals in circulation.
 * Provides crystal ownership, history, and management functionality.
 */
public class CrystalRegistry {
    private static final Map<String, CrystalInfo> crystalRegistry = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> playerCrystals = new ConcurrentHashMap<>();
    private static final Logger logger = ArcaniteCrystals.getInstance().getLogger();
    
    private static final NamespacedKey KEY_CRYSTAL_ID = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal_id");
    
    /**
     * Information about a registered crystal.
     */
    public static class CrystalInfo {
        private final String crystalId;
        private final UUID creator;
        private final Instant creationTime;
        private UUID currentOwner;
        private final List<OwnershipRecord> ownershipHistory;
        private final List<UsageRecord> usageHistory;
        private boolean destroyed;
        private String destructionReason;
        
        public CrystalInfo(String crystalId, UUID creator) {
            this.crystalId = crystalId;
            this.creator = creator;
            this.creationTime = Instant.now();
            this.currentOwner = creator;
            this.ownershipHistory = new ArrayList<>();
            this.usageHistory = new ArrayList<>();
            this.destroyed = false;
            
            // Add initial ownership record
            this.ownershipHistory.add(new OwnershipRecord(creator, creationTime, "Creation"));
        }
        
        public String getCrystalId() { return crystalId; }
        public UUID getCreator() { return creator; }
        public Instant getCreationTime() { return creationTime; }
        public UUID getCurrentOwner() { return currentOwner; }
        public List<OwnershipRecord> getOwnershipHistory() { return Collections.unmodifiableList(ownershipHistory); }
        public List<UsageRecord> getUsageHistory() { return Collections.unmodifiableList(usageHistory); }
        public boolean isDestroyed() { return destroyed; }
        public String getDestructionReason() { return destructionReason; }
    }
    
    /**
     * Record of crystal ownership transfer.
     */
    public static class OwnershipRecord {
        private final UUID owner;
        private final Instant timestamp;
        private final String reason;
        
        public OwnershipRecord(UUID owner, Instant timestamp, String reason) {
            this.owner = owner;
            this.timestamp = timestamp;
            this.reason = reason;
        }
        
        public UUID getOwner() { return owner; }
        public Instant getTimestamp() { return timestamp; }
        public String getReason() { return reason; }
    }
    
    /**
     * Record of crystal usage.
     */
    public static class UsageRecord {
        private final String action;
        private final Instant timestamp;
        private final Map<String, Object> details;
        
        public UsageRecord(String action, Map<String, Object> details) {
            this.action = action;
            this.timestamp = Instant.now();
            this.details = new HashMap<>(details);
        }
        
        public String getAction() { return action; }
        public Instant getTimestamp() { return timestamp; }
        public Map<String, Object> getDetails() { return Collections.unmodifiableMap(details); }
    }
    
    /**
     * Registers a new crystal in the system.
     */
    public static String registerCrystal(ItemStack crystal, UUID creator) {
        String crystalId = generateCrystalId();
        
        // Create crystal info
        CrystalInfo info = new CrystalInfo(crystalId, creator);
        crystalRegistry.put(crystalId, info);
        
        // Add to player's crystals
        playerCrystals.computeIfAbsent(creator, k -> ConcurrentHashMap.newKeySet())
            .add(crystalId);
        
        // Store crystal ID in item
        ItemMeta meta = crystal.getItemMeta();
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(KEY_CRYSTAL_ID, PersistentDataType.STRING, crystalId);
            crystal.setItemMeta(meta);
        }
        
        logger.info("Registered new crystal " + crystalId + " created by " + creator);
        return crystalId;
    }
    
    /**
     * Looks up information about a crystal.
     */
    public static CrystalInfo lookupCrystal(String crystalId) {
        return crystalRegistry.get(crystalId);
    }
    
    /**
     * Transfers crystal ownership.
     */
    public static void transferOwnership(String crystalId, UUID fromPlayer, UUID toPlayer, String reason) {
        CrystalInfo info = crystalRegistry.get(crystalId);
        if (info == null) {
            logger.warning("Attempted to transfer non-existent crystal: " + crystalId);
            return;
        }
        
        if (!fromPlayer.equals(info.getCurrentOwner())) {
            logger.warning("Invalid ownership transfer attempt for crystal " + crystalId);
            return;
        }
        
        // Update ownership
        info.currentOwner = toPlayer;
        info.ownershipHistory.add(new OwnershipRecord(toPlayer, Instant.now(), reason));
        
        // Update player crystal sets
        Set<String> fromCrystals = playerCrystals.get(fromPlayer);
        if (fromCrystals != null) {
            fromCrystals.remove(crystalId);
        }
        
        playerCrystals.computeIfAbsent(toPlayer, k -> ConcurrentHashMap.newKeySet())
            .add(crystalId);
        
        logger.info("Transferred crystal " + crystalId + " from " + fromPlayer + " to " + toPlayer);
    }
    
    /**
     * Gets all crystals owned by a player.
     */
    public static Set<String> getPlayerCrystals(UUID playerId) {
        return Collections.unmodifiableSet(
            playerCrystals.getOrDefault(playerId, Collections.emptySet())
        );
    }
    
    /**
     * Records crystal usage.
     */
    public static void recordUsage(String crystalId, String action, Map<String, Object> details) {
        CrystalInfo info = crystalRegistry.get(crystalId);
        if (info == null) {
            logger.warning("Attempted to record usage for non-existent crystal: " + crystalId);
            return;
        }
        
        info.usageHistory.add(new UsageRecord(action, details));
    }
    
    /**
     * Marks a crystal as destroyed.
     */
    public static void destroyCrystal(String crystalId, String reason) {
        CrystalInfo info = crystalRegistry.get(crystalId);
        if (info == null) {
            logger.warning("Attempted to destroy non-existent crystal: " + crystalId);
            return;
        }
        
        info.destroyed = true;
        info.destructionReason = reason;
        
        // Remove from player's crystals
        Set<String> playerCrystalSet = playerCrystals.get(info.getCurrentOwner());
        if (playerCrystalSet != null) {
            playerCrystalSet.remove(crystalId);
        }
        
        logger.info("Destroyed crystal " + crystalId + " owned by " + info.getCurrentOwner() + 
            " for reason: " + reason);
    }
    
    /**
     * Gets crystal ID from an item stack.
     */
    public static String getCrystalId(ItemStack crystal) {
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) return null;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(KEY_CRYSTAL_ID, PersistentDataType.STRING);
    }
    
    /**
     * Generates a unique crystal ID.
     */
    private static String generateCrystalId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
    
    /**
     * Cleans up registry data.
     */
    public static void cleanup() {
        crystalRegistry.clear();
        playerCrystals.clear();
    }
} 