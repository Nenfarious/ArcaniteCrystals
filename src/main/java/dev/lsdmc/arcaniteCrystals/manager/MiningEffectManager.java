package dev.lsdmc.arcaniteCrystals.manager;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks and manages temporary mining-related effects (auto-smelt & fortune)
 * granted by activated Arcanite Crystals.
 */
public class MiningEffectManager {

    private static final Map<UUID, Long> autoSmeltExpiry = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> fortuneExpiry   = new ConcurrentHashMap<>();

    /**
     * Grants auto-smelt effect to the player for the specified duration in ticks.
     *
     * @param player        the player to grant the effect to
     * @param durationTicks how long the effect should last, in game ticks
     */
    public static void applyAutoSmelt(Player player, long durationTicks) {
        long expiryMillis = System.currentTimeMillis() + (durationTicks * 50L);
        autoSmeltExpiry.put(player.getUniqueId(), expiryMillis);
    }

    /**
     * Grants fortune effect to the player for the specified duration in ticks.
     *
     * @param player        the player to grant the effect to
     * @param durationTicks how long the effect should last, in game ticks
     */
    public static void applyFortune(Player player, long durationTicks) {
        long expiryMillis = System.currentTimeMillis() + (durationTicks * 50L);
        fortuneExpiry.put(player.getUniqueId(), expiryMillis);
    }

    /**
     * Checks if the player currently has auto-smelt active.
     *
     * @param uuid the UUID of the player
     * @return true if auto-smelt is still active, false otherwise
     */
    public static boolean hasAutoSmelt(UUID uuid) {
        return autoSmeltExpiry.getOrDefault(uuid, 0L) > System.currentTimeMillis();
    }

    /**
     * Checks if the player currently has fortune active.
     *
     * @param uuid the UUID of the player
     * @return true if fortune is still active, false otherwise
     */
    public static boolean hasFortune(UUID uuid) {
        return fortuneExpiry.getOrDefault(uuid, 0L) > System.currentTimeMillis();
    }
}
