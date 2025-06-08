package dev.lsdmc.arcaniteCrystals.util;

import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Professional sound management system with comprehensive audio feedback
 * for all plugin interactions and events.
 */
public class SoundManager {

    private static final ArcaniteCrystals plugin = ArcaniteCrystals.getInstance();

    /**
     * Plays crystal activation sound with enhanced audio feedback.
     */
    public static void playCrystalActivateSound(Player player) {
        try {
            String soundName = ConfigManager.getConfig().getString("sounds.activate", "ENTITY_EXPERIENCE_ORB_PICKUP");
            Sound sound = getSoundSafely(soundName);
            
            player.playSound(player.getLocation(), sound, 0.8f, 1.2f);
            // Add subtle echo effect
            Sound echoSound = getSoundSafely("ENTITY_EXPERIENCE_ORB_PICKUP");
            player.playSound(player.getLocation(), echoSound, 0.3f, 0.8f);
        } catch (Exception e) {
            // Ultimate fallback - use a sound that should exist in all versions
            playFallbackSound(player, 0.8f, 1.2f);
        }
    }

    /**
     * Plays crystal recharge sound with satisfaction feedback.
     */
    public static void playCrystalRechargeSound(Player player) {
        try {
            String soundName = ConfigManager.getConfig().getString("sounds.recharge", "BLOCK_BEACON_ACTIVATE");
            Sound sound = getSoundSafely(soundName);
            
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            // Add magical chime
            Sound chimeSound = getSoundSafely("BLOCK_NOTE_BLOCK_CHIME");
            player.playSound(player.getLocation(), chimeSound, 0.5f, 2.0f);
        } catch (Exception e) {
            // Fallback sound
            playFallbackSound(player, 1.0f, 1.0f);
        }
    }

    /**
     * Plays upgrade unlock sound with celebration audio.
     */
    public static void playUpgradeUnlockSound(Player player) {
        try {
            Sound levelUpSound = getSoundSafely("ENTITY_PLAYER_LEVELUP");
            Sound chimeSound = getSoundSafely("BLOCK_NOTE_BLOCK_CHIME");
            Sound expSound = getSoundSafely("ENTITY_EXPERIENCE_ORB_PICKUP");
            
            player.playSound(player.getLocation(), levelUpSound, 1.0f, 1.3f);
            player.playSound(player.getLocation(), chimeSound, 0.7f, 1.8f);
            player.playSound(player.getLocation(), expSound, 0.5f, 1.5f);
        } catch (Exception e) {
            // Fallback celebration
            playFallbackSound(player, 1.0f, 1.3f);
        }
    }

    /**
     * Plays menu interaction sounds with fallback handling.
     */
    public static void playMenuSound(Player player, MenuSoundType type) {
        try {
            switch (type) {
                case OPEN -> {
                    Sound openSound = getSoundSafely("BLOCK_CHEST_OPEN");
                    Sound clickSound = getSoundSafely("UI_BUTTON_CLICK");
                    player.playSound(player.getLocation(), openSound, 0.7f, 1.2f);
                    player.playSound(player.getLocation(), clickSound, 0.3f, 1.5f);
                }
                case CLOSE -> {
                    Sound closeSound = getSoundSafely("BLOCK_CHEST_CLOSE");
                    player.playSound(player.getLocation(), closeSound, 0.7f, 1.0f);
                }
                case CLICK -> {
                    Sound clickSound = getSoundSafely("UI_BUTTON_CLICK");
                    player.playSound(player.getLocation(), clickSound, 0.5f, 1.2f);
                }
                case ERROR -> {
                    Sound villagerNoSound = getSoundSafely("ENTITY_VILLAGER_NO");
                    Sound bassSound = getSoundSafely("BLOCK_NOTE_BLOCK_BASS");
                    player.playSound(player.getLocation(), villagerNoSound, 0.8f, 0.8f);
                    player.playSound(player.getLocation(), bassSound, 0.5f, 0.5f);
                }
                case SUCCESS -> {
                    Sound villagerYesSound = getSoundSafely("ENTITY_VILLAGER_YES");
                    Sound expSound = getSoundSafely("ENTITY_EXPERIENCE_ORB_PICKUP");
                    player.playSound(player.getLocation(), villagerYesSound, 0.8f, 1.2f);
                    player.playSound(player.getLocation(), expSound, 0.4f, 1.5f);
                }
            }
        } catch (Exception e) {
            // Fallback for menu sounds
            playFallbackSound(player, 0.5f, 1.0f);
        }
    }

    /**
     * Safely gets a Sound enum by name with fallback handling.
     */
    private static Sound getSoundSafely(String soundName) {
        try {
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try common fallback mappings
            return switch (soundName.toUpperCase()) {
                case "ENTITY_EXPERIENCE_ORB_PICKUP", "ENTITY_ORB_PICKUP" -> 
                    tryGetSound("ENTITY_EXPERIENCE_ORB_PICKUP", "ENTITY_ORB_PICKUP");
                case "BLOCK_BEACON_ACTIVATE", "BEACON_ACTIVATE" -> 
                    tryGetSound("BLOCK_BEACON_ACTIVATE", "BEACON_ACTIVATE");
                case "ENTITY_PLAYER_LEVELUP", "LEVEL_UP" -> 
                    tryGetSound("ENTITY_PLAYER_LEVELUP", "LEVEL_UP");
                case "UI_BUTTON_CLICK", "CLICK" -> 
                    tryGetSound("UI_BUTTON_CLICK", "CLICK");
                case "BLOCK_CHEST_OPEN", "CHEST_OPEN" -> 
                    tryGetSound("BLOCK_CHEST_OPEN", "CHEST_OPEN");
                case "BLOCK_CHEST_CLOSE", "CHEST_CLOSE" -> 
                    tryGetSound("BLOCK_CHEST_CLOSE", "CHEST_CLOSE");
                case "ENTITY_VILLAGER_NO", "VILLAGER_NO" -> 
                    tryGetSound("ENTITY_VILLAGER_NO", "VILLAGER_NO");
                case "ENTITY_VILLAGER_YES", "VILLAGER_YES" -> 
                    tryGetSound("ENTITY_VILLAGER_YES", "VILLAGER_YES");
                case "BLOCK_NOTE_BLOCK_CHIME", "NOTE_PLING" -> 
                    tryGetSound("BLOCK_NOTE_BLOCK_CHIME", "NOTE_PLING");
                case "BLOCK_NOTE_BLOCK_BASS", "NOTE_BASS" -> 
                    tryGetSound("BLOCK_NOTE_BLOCK_BASS", "NOTE_BASS");
                default -> getUltimateFallbackSound();
            };
        }
    }
    
    /**
     * Tries to get a sound from a list of possible names.
     */
    private static Sound tryGetSound(String... soundNames) {
        for (String soundName : soundNames) {
            try {
                return Sound.valueOf(soundName.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Continue to next option
            }
        }
        return getUltimateFallbackSound();
    }
    
    /**
     * Gets a sound that should exist in all Minecraft versions.
     */
    private static Sound getUltimateFallbackSound() {
        // Try sounds that have existed for a very long time
        String[] universalSounds = {
            "CLICK", "NOTE_PLING", "LEVEL_UP", "ORB_PICKUP"
        };
        
        for (String soundName : universalSounds) {
            try {
                return Sound.valueOf(soundName);
            } catch (IllegalArgumentException e) {
                // Continue to next option
            }
        }
        
        // If all else fails, use the first sound available (should never happen)
        Sound[] allSounds = Sound.values();
        return allSounds.length > 0 ? allSounds[0] : null;
    }
    
    /**
     * Plays a safe fallback sound that should work in all versions.
     */
    private static void playFallbackSound(Player player, float volume, float pitch) {
        try {
            Sound fallbackSound = getUltimateFallbackSound();
            if (fallbackSound != null) {
                player.playSound(player.getLocation(), fallbackSound, volume, pitch);
            }
        } catch (Exception e) {
            // If even fallback fails, silently continue
            ArcaniteCrystals.getInstance().getLogger().fine("Could not play any sound for " + player.getName());
        }
    }

    /**
     * Plays crystal depletion warning sound.
     */
    public static void playCrystalDepletionSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.6f, 0.8f);
        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.3f, 1.2f);
    }

    /**
     * Plays level up celebration sound sequence.
     */
    public static void playLevelUpSound(Player player) {
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.7f, 1.3f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
    }

    /**
     * Plays cooldown warning sound.
     */
    public static void playCooldownSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.4f, 0.7f);
    }

    /**
     * Plays mining effect sounds based on the type.
     */
    public static void playMiningSound(Location blockLocation, boolean autoSmelt) {
        if (autoSmelt) {
            // Fire crackling sounds for auto-smelt
            blockLocation.getWorld().playSound(blockLocation, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.2f);
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                blockLocation.getWorld().playSound(blockLocation, Sound.ITEM_FIRECHARGE_USE, 0.4f, 1.0f);
            }, 2L);
        } else {
            // Lucky chimes for fortune
            blockLocation.getWorld().playSound(blockLocation, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.8f);
            blockLocation.getWorld().playSound(blockLocation, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.3f, 1.5f);
        }
    }

    /**
     * Plays ambient crystal energy sounds.
     */
    public static void playAmbientCrystalSound(Player player) {
        Location loc = player.getLocation();
        
        // Subtle magical humming
        float randomPitch = 0.8f + ThreadLocalRandom.current().nextFloat() * 0.4f;
        float randomVolume = 0.1f + ThreadLocalRandom.current().nextFloat() * 0.1f;
        
        player.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, randomVolume, randomPitch);
    }

    /**
     * Plays error notification sound.
     */
    public static void playErrorSound(Player player) {
        Location loc = player.getLocation();
        
        player.playSound(loc, Sound.ENTITY_VILLAGER_NO, 0.6f, 0.8f);
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 0.4f, 0.5f);
        }, 3L);
    }

    /**
     * Plays success notification sound.
     */
    public static void playSuccessSound(Player player) {
        Location loc = player.getLocation();
        
        player.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.4f, 1.5f);
        }, 2L);
    }

    /**
     * Plays warning notification sound.
     */
    public static void playWarningSound(Player player) {
        Location loc = player.getLocation();
        
        player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.0f);
        player.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 0.8f);
    }

    /**
     * Plays a sequence of ascending musical notes.
     */
    public static void playMusicalSequence(Player player, float[] pitches, long delayTicks) {
        Location loc = player.getLocation();
        
        for (int i = 0; i < pitches.length; i++) {
            final float pitch = pitches[i];
            final long delay = delayTicks * i;
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, pitch);
            }, delay);
        }
    }

    /**
     * Plays decay sound effects for a player.
     */
    public static void playCrystalDecaySound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.5f, 0.8f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.3f, 1.0f);
    }

    /**
     * Plays corruption sound effects for a player.
     */
    public static void playCrystalCorruptionSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 0.5f, 0.7f);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.3f, 1.2f);
    }

    /**
     * Plays crystal destruction sound effects for a player.
     */
    public static void playCrystalDestructionSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.8f);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 0.5f, 0.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.3f, 1.0f);
    }

    /**
     * Plays socket sound effects for a player.
     */
    public static void playCrystalSocketSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 1.2f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.0f);
    }
    
    /**
     * Plays socket removal sound effects for a player.
     */
    public static void playCrystalSocketRemoveSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 0.5f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.3f, 0.8f);
    }

    /**
     * Plays fusion sound effect for a player.
     */
    public static void playCrystalFusionSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.2f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
    }
    
    /**
     * Plays fusion failure sound effect for a player.
     */
    public static void playCrystalFusionFailSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 0.8f);
    }

    /**
     * Menu sound types for different interactions.
     */
    public enum MenuSoundType {
        OPEN, CLOSE, CLICK, ERROR, SUCCESS
    }

    /**
     * Plays regeneration sound effects for a player.
     */
    public static void playCrystalRegenSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.2f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.0f);
    }
} 