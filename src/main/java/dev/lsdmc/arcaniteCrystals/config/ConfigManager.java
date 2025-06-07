// src/main/java/dev/lsdmc/arcaniteCrystals/config/ConfigManager.java
package dev.lsdmc.arcaniteCrystals.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Handles backup, purge, reload and saving of config.yml and other config files.
 */
public class ConfigManager {

    private static JavaPlugin plugin;
    private static FileConfiguration config;
    private static FileConfiguration messagesConfig;
    private static FileConfiguration upgradesConfig;
    private static FileConfiguration levelsConfig;

    public static void initialize(JavaPlugin pl) {
        plugin = pl;
        pl.saveDefaultConfig();
        config = plugin.getConfig();
        
        // Load additional config files
        loadMessagesConfig();
        loadUpgradesConfig();
        loadLevelsConfig();
    }

    public static FileConfiguration getConfig() {
        return config;
    }

    public static FileConfiguration getMessagesConfig() {
        if (messagesConfig == null) {
            loadMessagesConfig();
        }
        return messagesConfig;
    }

    public static FileConfiguration getUpgradesConfig() {
        if (upgradesConfig == null) {
            loadUpgradesConfig();
        }
        return upgradesConfig;
    }

    public static FileConfiguration getLevelsConfig() {
        if (levelsConfig == null) {
            loadLevelsConfig();
        }
        return levelsConfig;
    }

    private static void loadMessagesConfig() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private static void loadUpgradesConfig() {
        File upgradesFile = new File(plugin.getDataFolder(), "upgrades.yml");
        if (!upgradesFile.exists()) {
            plugin.saveResource("upgrades.yml", false);
        }
        upgradesConfig = YamlConfiguration.loadConfiguration(upgradesFile);
    }

    private static void loadLevelsConfig() {
        File levelsFile = new File(plugin.getDataFolder(), "levels.yml");
        if (!levelsFile.exists()) {
            plugin.saveResource("levels.yml", false);
        }
        levelsConfig = YamlConfiguration.loadConfiguration(levelsFile);
    }

    /** Persist current config to disk. */
    public static void saveConfig() {
        plugin.saveConfig();
        config = plugin.getConfig();
    }

    /** Reload from disk into memory. */
    public static void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Also reload additional config files
        loadMessagesConfig();
        loadUpgradesConfig();
        loadLevelsConfig();
    }

    /**
     * Create a timestamped backup of config.yml under /plugins/ArcaniteCrystals/backups/
     * @return path to backup file or null on failure
     */
    public static String backupConfig() {
        try {
            File dataFolder = plugin.getDataFolder();
            File configFile = new File(dataFolder, "config.yml");
            if (!configFile.exists()) return null;

            File backupDir = new File(dataFolder, "backups");
            backupDir.mkdirs();

            String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            File backup = new File(backupDir, "config-" + timestamp + ".yml");
            Files.copy(configFile.toPath(), backup.toPath());
            return backup.getPath();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to backup config.yml: " + e.getMessage());
            return null;
        }
    }

    /**
     * Purge current config.yml so that saveDefaultConfig + reloadConfig
     * will restore the shipped defaults.
     * @return true if purged & restored, false on error
     */
    public static boolean purgeConfig() {
        try {
            File dataFolder = plugin.getDataFolder();
            File configFile = new File(dataFolder, "config.yml");
            if (configFile.exists() && !configFile.delete()) {
                return false;
            }
            plugin.saveDefaultConfig();  // write fresh default
            reloadConfig();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to purge config.yml: " + e.getMessage());
            return false;
        }
    }
}
