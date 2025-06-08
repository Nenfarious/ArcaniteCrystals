// src/main/java/dev/lsdmc/arcaniteCrystals/util/MessageManager.java
package dev.lsdmc.arcaniteCrystals.util;

import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Professional messaging system with rich text formatting, interactive elements,
 * comprehensive notifications, and robust error handling.
 */
public class MessageManager {
    
    // Professional color scheme with consistent branding
    public static final String PRIMARY = ChatColor.BLUE.toString();
    public static final String SECONDARY = ChatColor.DARK_PURPLE.toString();
    public static final String SUCCESS = ChatColor.GREEN.toString();
    public static final String WARNING = ChatColor.GOLD.toString();
    public static final String ERROR = ChatColor.RED.toString();
    public static final String ACCENT = ChatColor.AQUA.toString();
    public static final String MUTED = ChatColor.GRAY.toString();
    public static final String HIGHLIGHT = ChatColor.YELLOW.toString();

    /**
     * Gets a message with color code translation and robust error handling.
     */
    public static String get(String path) {
        try {
            FileConfiguration msgs = ConfigManager.getMessagesConfig();
            String msg = msgs.getString(path);
            if (msg == null) {
                return path; // Return path as fallback
            }
            return ChatColor.translateAlternateColorCodes('&', msg);
        } catch (Exception e) {
            return path; // Safe fallback
        }
    }

    /**
     * Creates rich messages with comprehensive placeholder replacement.
     */
    public static String getMessage(String path, Map<String, String> placeholders) {
        try {
            String message = get(path);
            
            if (placeholders != null) {
                for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                    message = message.replace("{" + entry.getKey() + "}", entry.getValue());
                }
            }
            
            return message;
        } catch (Exception e) {
            return path; // Safe fallback
        }
    }

    /**
     * Gets a message with a single placeholder.
     */
    public static String getMessage(String path, String placeholder, String value) {
        return getMessage(path, Map.of(placeholder, value));
    }

    /**
     * Sends professional notifications with immersive sound and visual effects.
     */
    public static void sendNotification(Player player, String message, NotificationType type) {
        try {
            // Send styled message with professional formatting
            player.sendMessage(formatProfessionalMessage(message, type));
            
            // Play layered sound effects for immersion
            player.playSound(player.getLocation(), type.getSound(), 0.7f, type.getPitch());
            
            // Add particle effect sequences for important notifications
            if (type.hasParticles()) {
                ParticleManager.playNotificationEffect(player, type);
            }
        } catch (Exception e) {
            // Robust fallback - always ensure message delivery
            player.sendMessage(type.getFallbackPrefix() + message);
        }
    }

    /**
     * Sends interactive messages with hover suggestions and rich formatting.
     */
    public static void sendInteractiveMessage(Player player, String baseText, String suggestion) {
        try {
            String message = PRIMARY + ChatColor.BOLD + baseText + 
                           ChatColor.RESET + MUTED + " " + ChatColor.ITALIC + "(" + suggestion + ")";
            player.sendMessage(message);
        } catch (Exception e) {
            player.sendMessage(baseText); // Fallback
        }
    }

    /**
     * Displays professional titles with dramatic subtitle effects.
     */
    public static void showTitle(Player player, String titleText, String subtitleText) {
        try {
            player.sendTitle(
                PRIMARY + ChatColor.BOLD + titleText,
                SECONDARY + subtitleText,
                10, 40, 10
            );
        } catch (Exception e) {
            // Fallback to chat message with formatting
            player.sendMessage(PRIMARY + "▌ " + titleText + " - " + subtitleText);
        }
    }

    /**
     * Creates epic upgrade unlocked notifications with celebration effects.
     */
    public static void sendUpgradeUnlocked(Player player, String upgradeName, int tier) {
        try {
            String message = WARNING + "✨ " + SUCCESS + ChatColor.BOLD + "UPGRADE UNLOCKED" + 
                           WARNING + " ✨\n" +
                           ACCENT + "► " + PRIMARY + ChatColor.BOLD + upgradeName + " " + 
                           SECONDARY + getRomanNumeral(tier) + "\n" +
                           MUTED + ChatColor.ITALIC + "Use /arcanite talents to view your upgrades";

            sendNotification(player, message, NotificationType.UPGRADE);
            
            // Epic title effect with dramatic timing
            showTitle(player, "✨ UPGRADE UNLOCKED ✨", upgradeName + " " + getRomanNumeral(tier));
        } catch (Exception e) {
            // Robust fallback
            player.sendMessage(SUCCESS + "Unlocked: " + upgradeName + " " + getRomanNumeral(tier));
        }
    }

    /**
     * Creates spectacular level up notifications with celebration sequences.
     */
    public static void sendLevelUp(Player player, int newLevel, String tag) {
        try {
            String cleanTag = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', tag));
            
            String message = WARNING + "🎉 " + SUCCESS + ChatColor.BOLD + "LEVEL UP!" + 
                           WARNING + " 🎉\n" +
                           ACCENT + "► " + MUTED + "You are now " + 
                           ChatColor.translateAlternateColorCodes('&', tag) + "\n" +
                           ACCENT + ChatColor.ITALIC + "New abilities and crystal slots unlocked!";

            sendNotification(player, message, NotificationType.LEVEL_UP);
            
            // Dramatic title effect with clean tag display
            showTitle(player, "🎉 LEVEL UP! 🎉", cleanTag);
        } catch (Exception e) {
            // Fallback
            player.sendMessage(SUCCESS + "Level Up! You are now level " + newLevel);
        }
    }

    /**
     * Creates immersive crystal activation feedback with energy visualization.
     */
    public static void sendCrystalActivated(Player player, List<String> abilities, int energy) {
        try {
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(ACCENT).append("⚡ ").append(SUCCESS).append(ChatColor.BOLD)
                         .append("CRYSTAL ACTIVATED").append(ACCENT).append(" ⚡\n")
                         .append(MUTED).append("► Energy: ").append(createAdvancedEnergyBar(energy, 18000))
                         .append("\n").append(MUTED).append("► Effects: ");

            for (int i = 0; i < abilities.size(); i++) {
                if (i > 0) messageBuilder.append(MUTED).append(", ");
                messageBuilder.append(PRIMARY).append(beautifyAbilityName(abilities.get(i)));
            }

            sendNotification(player, messageBuilder.toString(), NotificationType.CRYSTAL_ACTIVATE);
        } catch (Exception e) {
            // Fallback
            player.sendMessage(ACCENT + "Crystal activated with " + abilities.size() + " effects!");
        }
    }

    /**
     * Creates advanced energy bar with color coding and visual effects.
     */
    private static String createAdvancedEnergyBar(int current, int max) {
        try {
            int barLength = 20;
            int filled = Math.max(0, Math.min(barLength, (int) ((double) current / max * barLength)));
            
            StringBuilder bar = new StringBuilder();
            bar.append(MUTED).append("[");
            
            // Dynamic color based on energy level with smooth transitions
            ChatColor energyColor = current > max * 0.75 ? ChatColor.GREEN :
                                   current > max * 0.5 ? ChatColor.YELLOW :
                                   current > max * 0.25 ? ChatColor.GOLD : ChatColor.RED;
            
            for (int i = 0; i < barLength; i++) {
                if (i < filled) {
                    bar.append(energyColor).append("█");
                } else {
                    bar.append(ChatColor.DARK_GRAY).append("░");
                }
            }
            
            bar.append(MUTED).append("] ").append(ACCENT).append(current).append("/").append(max);
            
            // Add energy status indicator
            String status = current > max * 0.75 ? " ⚡" : 
                           current > max * 0.25 ? " ⚠" : " ⛔";
            bar.append(HIGHLIGHT).append(status);
            
            return bar.toString();
        } catch (Exception e) {
            return current + "/" + max; // Simple fallback
        }
    }

    /**
     * Creates professional error messages with helpful suggestions.
     */
    public static void sendError(Player player, String errorKey, String suggestion) {
        try {
            String message = ERROR + "❌ " + ERROR + ChatColor.BOLD + "ERROR\n" +
                           MUTED + "► " + ERROR + get(errorKey) + "\n" +
                           WARNING + "💡 Suggestion: " + MUTED + ChatColor.ITALIC + suggestion;

            sendNotification(player, message, NotificationType.ERROR);
        } catch (Exception e) {
            // Fallback
            player.sendMessage(ERROR + "Error: " + errorKey);
        }
    }

    /**
     * Creates comprehensive help displays with interactive command entries.
     */
    public static void sendHelpMessage(Player player) {
        try {
            String header = PRIMARY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
            String title = "    " + SUCCESS + ChatColor.BOLD + "⚗️ ArcaniteCrystals Help ⚗️";
            
            StringBuilder commands = new StringBuilder();
            commands.append("\n").append(createCommandEntry("/arcanite talents", "Open crystal upgrade menu"))
                    .append("\n").append(createCommandEntry("/levelup", "Advance to next level"))
                    .append("\n").append(createCommandEntry("/mysterycrystal", "Get mystery crystal"))
                    .append("\n").append(createCommandEntry("/arcanite info", "View plugin information"))
                    .append("\n");

            String footer = PRIMARY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

            player.sendMessage(header + "\n" + title + commands.toString() + footer);
        } catch (Exception e) {
            // Fallback
            player.sendMessage(PRIMARY + "ArcaniteCrystals Help - Use /arcanite talents for upgrades");
        }
    }

    /**
     * Creates interactive command entries with professional formatting.
     */
    private static String createCommandEntry(String command, String description) {
        return ACCENT + "  ► " + PRIMARY + ChatColor.BOLD + command + 
               MUTED + " - " + description;
    }

    /**
     * Formats professional messages with sophisticated styling based on type.
     */
    private static String formatProfessionalMessage(String message, NotificationType type) {
        return type.getBorderColor() + "▌ " + ChatColor.RESET + message;
    }

    /**
     * Beautifies ability names for professional display.
     */
    private static String beautifyAbilityName(String abilityId) {
        try {
            String[] parts = abilityId.split("_");
            if (parts.length < 2) return abilityId;
            
            String effect = parts[0];
            String tier = parts[1];
            
            String beautifulName = switch (effect.toLowerCase()) {
                case "speed" -> "Swift Movement";
                case "regeneration" -> "Life Restoration";
                case "jump" -> "Leap Enhancement";
                case "haste" -> "Mining Acceleration";
                case "strength" -> "Combat Prowess";
                case "echo" -> "Damage Resistance";
                case "poison" -> "Toxic Immunity";
                default -> effect.substring(0, 1).toUpperCase() + effect.substring(1).toLowerCase();
            };
            
            return beautifulName + " " + tier;
        } catch (Exception e) {
            return abilityId;
        }
    }

    /**
     * Converts integer to Roman numerals with extended support.
     */
    private static String getRomanNumeral(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(number);
        };
    }

    /**
     * Professional notification types with sophisticated audio and visual effects.
     */
    public enum NotificationType {
        SUCCESS(ChatColor.GREEN, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, true),
        ERROR(ChatColor.RED, Sound.ENTITY_VILLAGER_NO, 0.8f, false),
        WARNING(ChatColor.GOLD, Sound.BLOCK_NOTE_BLOCK_BELL, 1.2f, false),
        INFO(ChatColor.BLUE, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, false),
        UPGRADE(ChatColor.AQUA, Sound.ENTITY_PLAYER_LEVELUP, 1.3f, true),
        LEVEL_UP(ChatColor.GREEN, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, true),
        CRYSTAL_ACTIVATE(ChatColor.BLUE, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.1f, true);

        private final ChatColor color;
        private final Sound sound;
        private final float pitch;
        private final boolean hasParticles;

        NotificationType(ChatColor color, Sound sound, float pitch, boolean hasParticles) {
            this.color = color;
            this.sound = sound;
            this.pitch = pitch;
            this.hasParticles = hasParticles;
        }

        public ChatColor getBorderColor() { 
            return color; 
        }
        
        public String getFallbackPrefix() {
            return color.toString() + "▌ ";
        }
        
        public Sound getSound() { 
            return sound; 
        }
        
        public float getPitch() { 
            return pitch; 
        }
        
        public boolean hasParticles() { 
            return hasParticles; 
        }
    }

    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
