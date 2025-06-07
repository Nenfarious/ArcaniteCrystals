package dev.lsdmc.arcaniteCrystals.util;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Professional particle effects system with dynamic visual feedback
 * for all plugin interactions and events.
 */
public class ParticleManager {

    // Configuration cache for performance
    private static boolean particlesEnabled = true;
    private static double densityMultiplier = 1.0;
    private static int particleRange = 32;
    private static int maxParticleCount = 100;
    
    static {
        loadParticleConfig();
    }
    
    /**
     * Loads particle configuration from config.yml
     */
    private static void loadParticleConfig() {
        try {
            var config = dev.lsdmc.arcaniteCrystals.config.ConfigManager.getConfig();
            particlesEnabled = config.getBoolean("particles.enabled", true);
            densityMultiplier = config.getDouble("particles.density-multiplier", 1.0);
            particleRange = config.getInt("particles.range", 32);
            maxParticleCount = Math.max(10, config.getInt("particles.max-count", 100));
        } catch (Exception e) {
            // Use defaults if config loading fails
            particlesEnabled = true;
            densityMultiplier = 1.0;
            particleRange = 32;
            maxParticleCount = 100;
        }
    }
    
    /**
     * Checks if particles should be spawned based on configuration and performance.
     */
    private static boolean shouldSpawnParticles() {
        if (!particlesEnabled) return false;
        
        // Performance check - reduce particles on high player count servers
        int onlinePlayers = ArcaniteCrystals.getInstance().getServer().getOnlinePlayers().size();
        if (onlinePlayers > 50) {
            // Reduce particle density for servers with many players
            return Math.random() < (densityMultiplier * 0.5);
        } else if (onlinePlayers > 20) {
            return Math.random() < (densityMultiplier * 0.75);
        }
        
        return Math.random() < densityMultiplier;
    }
    
    /**
     * Calculates appropriate particle count based on performance settings.
     */
    private static int calculateParticleCount(int requestedCount) {
        if (!shouldSpawnParticles()) return 0;
        
        int onlinePlayers = ArcaniteCrystals.getInstance().getServer().getOnlinePlayers().size();
        double performanceMultiplier = 1.0;
        
        // Scale down particles based on server load
        if (onlinePlayers > 50) {
            performanceMultiplier = 0.3;
        } else if (onlinePlayers > 20) {
            performanceMultiplier = 0.6;
        }
        
        int adjustedCount = (int) (requestedCount * densityMultiplier * performanceMultiplier);
        return Math.min(adjustedCount, maxParticleCount);
    }
    
    /**
     * Spawns particles for players within range.
     */
    private static void spawnParticleForNearbyPlayers(Location center, Particle particle, int count, 
                                                     double offsetX, double offsetY, double offsetZ, double extra) {
        try {
            center.getWorld().getNearbyEntities(center, particleRange, particleRange, particleRange)
                    .stream()
                    .filter(entity -> entity instanceof Player)
                    .map(entity -> (Player) entity)
                    .forEach(player -> {
                        try {
                            player.spawnParticle(particle, center, count, offsetX, offsetY, offsetZ, extra);
                        } catch (Exception e) {
                            // Silently ignore individual player particle failures
                        }
                    });
        } catch (Exception e) {
            // Fallback to single player if nearby entity check fails
            center.getWorld().spawnParticle(particle, center, count, offsetX, offsetY, offsetZ, extra);
        }
    }
    
    /**
     * Spawns particles with data (like DustOptions) for players within range.
     */
    private static void spawnParticleForNearbyPlayers(Location center, Particle particle, int count, 
                                                     double offsetX, double offsetY, double offsetZ, double extra, Object data) {
        try {
            center.getWorld().getNearbyEntities(center, particleRange, particleRange, particleRange)
                    .stream()
                    .filter(entity -> entity instanceof Player)
                    .map(entity -> (Player) entity)
                    .forEach(player -> {
                        try {
                            player.spawnParticle(particle, center, count, offsetX, offsetY, offsetZ, extra, data);
                        } catch (Exception e) {
                            // Silently ignore individual player particle failures
                        }
                    });
        } catch (Exception e) {
            // Fallback to world spawn if nearby entity check fails
            center.getWorld().spawnParticle(particle, center, count, offsetX, offsetY, offsetZ, extra, data);
        }
    }

    /**
     * Plays crystal activation particle effect with mystical aura and performance optimization.
     */
    public static void playCrystalActivationEffect(Player player) {
        int baseCount = calculateParticleCount(30);
        if (baseCount == 0) return;
        
        Location loc = player.getLocation().add(0, 1, 0);
        
        // Central burst
        player.spawnParticle(Particle.ENCHANT, loc, baseCount, 0.5, 0.5, 0.5, 0.1);
        player.spawnParticle(Particle.ENCHANTED_HIT, loc, calculateParticleCount(15), 0.3, 0.3, 0.3, 0.05);
        
        // Delayed spiral effect (only if performance allows)
        if (shouldSpawnParticles()) {
            new BukkitRunnable() {
                double angle = 0;
                int ticks = 0;
                
                @Override
                public void run() {
                    if (ticks >= 20 || !shouldSpawnParticles()) {
                        cancel();
                        return;
                    }
                    
                    for (int i = 0; i < 3; i++) {
                        double x = Math.cos(angle + i * 2.0944) * 1.5; // 120 degrees apart
                        double z = Math.sin(angle + i * 2.0944) * 1.5;
                        Location spiralLoc = loc.clone().add(x, 0.5, z);
                        
                        player.spawnParticle(Particle.ENCHANT, spiralLoc, 2, 0.1, 0.1, 0.1, 0.01);
                    }
                    
                    angle += 0.3;
                    ticks++;
                }
            }.runTaskTimer(ArcaniteCrystals.getInstance(), 5L, 1L);
        }
    }

    /**
     * Plays upgrade unlock particle effect with celebration particles.
     */
    public static void playUpgradeEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        
        // Celebration burst
        player.spawnParticle(Particle.FIREWORK, loc, 50, 1.0, 1.0, 1.0, 0.1);
        player.spawnParticle(Particle.HAPPY_VILLAGER, loc, 20, 0.8, 0.8, 0.8, 0.05);
        
        // Rising golden particles
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 30) {
                    cancel();
                    return;
                }
                
                for (int i = 0; i < 5; i++) {
                    double x = (Math.random() - 0.5) * 2;
                    double z = (Math.random() - 0.5) * 2;
                    double y = Math.random() * 3;
                    
                    Location particleLoc = loc.clone().add(x, y, z);
                    player.spawnParticle(Particle.ENCHANTED_HIT, particleLoc, 1, 0, 0, 0, 0);
                }
                
                ticks++;
            }
        }.runTaskTimer(ArcaniteCrystals.getInstance(), 0L, 2L);
    }

    /**
     * Plays crystal recharge particle effect with energy restoration.
     */
    public static void playCrystalRechargeEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        
        // Energy restoration burst
        player.spawnParticle(Particle.SOUL, loc, 25, 0.5, 0.5, 0.5, 0.05);
        player.spawnParticle(Particle.ENCHANT, loc, 20, 0.3, 0.3, 0.3, 0.02);
        
        // Healing particles
        player.spawnParticle(Particle.HEART, loc.clone().add(0, 0.5, 0), 8, 0.5, 0.2, 0.5, 0.01);
    }

    /**
     * Plays crystal depletion particle effect with warning visuals.
     */
    public static void playCrystalDepletionEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        
        // Warning smoke and sparks
        player.spawnParticle(Particle.SMOKE, loc, 15, 0.3, 0.3, 0.3, 0.02);
        player.spawnParticle(Particle.CRIT, loc, 10, 0.4, 0.4, 0.4, 0.05);
        
        // Red warning particles
        Particle.DustOptions redDust = new Particle.DustOptions(Color.RED, 1.0f);
        player.spawnParticle(Particle.DUST, loc, 20, 0.5, 0.5, 0.5, 0.01, redDust);
    }

    /**
     * Plays level up particle effect with dramatic celebration.
     */
    public static void playLevelUpEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        
        // Dramatic celebration burst
        player.spawnParticle(Particle.FIREWORK, loc, 100, 2.0, 2.0, 2.0, 0.15);
        player.spawnParticle(Particle.END_ROD, loc, 30, 1.0, 1.0, 1.0, 0.1);
        
        // Golden shower effect
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 60) {
                    cancel();
                    return;
                }
                
                // Golden rain
                for (int i = 0; i < 3; i++) {
                    double x = (Math.random() - 0.5) * 4;
                    double z = (Math.random() - 0.5) * 4;
                    Location rainLoc = loc.clone().add(x, 3, z);
                    
                    Particle.DustOptions goldDust = new Particle.DustOptions(Color.YELLOW, 1.5f);
                    player.spawnParticle(Particle.DUST, rainLoc, 1, 0, 0, 0, 0, goldDust);
                }
                
                ticks++;
            }
        }.runTaskTimer(ArcaniteCrystals.getInstance(), 10L, 1L);
    }

    /**
     * Plays notification particle effect for MessageManager types.
     */
    public static void playNotificationEffect(Player player, MessageManager.NotificationType type) {
        Location loc = player.getLocation().add(0, 1.5, 0);
        
        switch (type) {
            case SUCCESS -> {
                player.spawnParticle(Particle.HAPPY_VILLAGER, loc, 10, 0.5, 0.3, 0.5, 0.02);
                Particle.DustOptions greenDust = new Particle.DustOptions(Color.GREEN, 1.0f);
                player.spawnParticle(Particle.DUST, loc, 8, 0.3, 0.3, 0.3, 0.01, greenDust);
            }
            case ERROR -> {
                player.spawnParticle(Particle.ANGRY_VILLAGER, loc, 8, 0.3, 0.3, 0.3, 0.02);
                Particle.DustOptions redDust = new Particle.DustOptions(Color.RED, 1.0f);
                player.spawnParticle(Particle.DUST, loc, 12, 0.4, 0.4, 0.4, 0.01, redDust);
            }
            case UPGRADE -> playUpgradeEffect(player);
            case LEVEL_UP -> playLevelUpEffect(player);
            case CRYSTAL_ACTIVATE -> playCrystalActivationEffect(player);
            default -> {
                // Generic notification
                player.spawnParticle(Particle.ENCHANT, loc, 5, 0.3, 0.3, 0.3, 0.01);
            }
        }
    }

    /**
     * Creates a continuous aura effect for active crystals.
     */
    public static void playActiveAuraEffect(Player player) {
        Location loc = player.getLocation().add(0, 0.2, 0);
        
        // Subtle continuous aura
        double angle = System.currentTimeMillis() * 0.003;
        
        for (int i = 0; i < 3; i++) {
            double x = Math.cos(angle + i * 2.0944) * 0.8;
            double z = Math.sin(angle + i * 2.0944) * 0.8;
            Location auraLoc = loc.clone().add(x, 0, z);
            
            player.spawnParticle(Particle.ENCHANT, auraLoc, 1, 0.05, 0.05, 0.05, 0.001);
        }
    }

    /**
     * Plays mining enhancement particle effect.
     */
    public static void playMiningEnhanceEffect(Player player, Location blockLoc) {
        // Enhanced mining particles
        player.spawnParticle(Particle.ENCHANTED_HIT, blockLoc.add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0.05);
        player.spawnParticle(Particle.ENCHANT, blockLoc, 5, 0.2, 0.2, 0.2, 0.02);
        
        // Gold sparkles for fortune effect
        Particle.DustOptions goldDust = new Particle.DustOptions(Color.YELLOW, 0.8f);
        player.spawnParticle(Particle.DUST, blockLoc, 8, 0.4, 0.4, 0.4, 0.01, goldDust);
    }

    /**
     * Plays mining effect particles at a location with performance optimization.
     */
    public static void playMiningEffect(Location blockLoc, boolean isAutoSmelt) {
        if (!shouldSpawnParticles()) return;
        
        if (isAutoSmelt) {
            // Auto-smelt fire effects
            int flameCount = calculateParticleCount(8);
            int smokeCount = calculateParticleCount(5);
            
            spawnParticleForNearbyPlayers(blockLoc.clone().add(0.5, 0.5, 0.5), 
                Particle.FLAME, flameCount, 0.3, 0.3, 0.3, 0.02);
            spawnParticleForNearbyPlayers(blockLoc.clone().add(0.5, 0.5, 0.5), 
                Particle.SMOKE, smokeCount, 0.2, 0.2, 0.2, 0.01);
            
            // Orange dust for heat
            Particle.DustOptions orangeDust = new Particle.DustOptions(Color.ORANGE, 1.0f);
            spawnParticleForNearbyPlayers(blockLoc.clone().add(0.5, 0.5, 0.5), 
                Particle.DUST, calculateParticleCount(6), 0.3, 0.3, 0.3, 0.01, orangeDust);
        } else {
            // Fortune luck effects
            int enchantCount = calculateParticleCount(10);
            int villagerCount = calculateParticleCount(3);
            
            spawnParticleForNearbyPlayers(blockLoc.clone().add(0.5, 0.5, 0.5), 
                Particle.ENCHANT, enchantCount, 0.4, 0.4, 0.4, 0.05);
            spawnParticleForNearbyPlayers(blockLoc.clone().add(0.5, 0.5, 0.5), 
                Particle.HAPPY_VILLAGER, villagerCount, 0.2, 0.2, 0.2, 0.01);
            
            // Gold sparkles for fortune
            Particle.DustOptions goldDust = new Particle.DustOptions(Color.YELLOW, 0.8f);
            spawnParticleForNearbyPlayers(blockLoc.clone().add(0.5, 0.5, 0.5), 
                Particle.DUST, calculateParticleCount(8), 0.4, 0.4, 0.4, 0.01, goldDust);
        }
    }

    /**
     * Plays cooldown warning particle effect.
     */
    public static void playCooldownWarningEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        
        // Warning indicators
        Particle.DustOptions orangeDust = new Particle.DustOptions(Color.ORANGE, 1.2f);
        player.spawnParticle(Particle.DUST, loc, 15, 0.5, 0.3, 0.5, 0.01, orangeDust);
        player.spawnParticle(Particle.SMOKE, loc, 5, 0.2, 0.2, 0.2, 0.01);
    }
} 