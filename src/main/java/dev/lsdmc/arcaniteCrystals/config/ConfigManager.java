// src/main/java/dev/lsdmc/arcaniteCrystals/config/ConfigManager.java
package dev.lsdmc.arcaniteCrystals.config;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.nio.file.Files;

public class ConfigManager {
    private static FileConfiguration config;
    private static File configFile;
    private static FileConfiguration upgradesConfig;
    private static FileConfiguration levelsConfig;
    private static FileConfiguration messagesConfig;
    
    public static void initialize() {
        configFile = new File(ArcaniteCrystals.getInstance().getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            ArcaniteCrystals.getInstance().saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Load other configs
        File upgradesFile = new File(ArcaniteCrystals.getInstance().getDataFolder(), "upgrades.yml");
        if (!upgradesFile.exists()) {
            ArcaniteCrystals.getInstance().saveResource("upgrades.yml", false);
        }
        upgradesConfig = YamlConfiguration.loadConfiguration(upgradesFile);
        
        File levelsFile = new File(ArcaniteCrystals.getInstance().getDataFolder(), "levels.yml");
        if (!levelsFile.exists()) {
            ArcaniteCrystals.getInstance().saveResource("levels.yml", false);
        }
        levelsConfig = YamlConfiguration.loadConfiguration(levelsFile);
        
        File messagesFile = new File(ArcaniteCrystals.getInstance().getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            ArcaniteCrystals.getInstance().saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        loadDefaults();
    }
    
    private static void loadDefaults() {
        // Crystal Settings
        setDefault("crystals.max-level", 10);
        setDefault("crystals.base-energy", 100);
        setDefault("crystals.energy-regen-interval", 300); // 5 minutes
        setDefault("crystals.energy-regen-amount", 10);
        
        // Fusion Settings
        setDefault("fusion.success-chance", 0.7);
        setDefault("fusion.cooldown", 3600); // 1 hour
        setDefault("fusion.max-level-difference", 2);
        
        // Socket Settings
        setDefault("socket.max-sockets", 3);
        setDefault("socket.cooldown", 1800); // 30 minutes
        
        // Decay Settings
        setDefault("decay.check-interval", 600); // 10 minutes
        setDefault("decay.chance", 0.1);
        setDefault("decay.max-level", 5);
        setDefault("decay.corruption-chance", 0.2);
        setDefault("decay.max-corruption-level", 3);
        
        // Statistics Settings
        setDefault("statistics.save-interval", 300); // 5 minutes
        
        // Save the config if any defaults were added
        saveConfig();
    }
    
    private static void setDefault(String path, Object value) {
        if (!config.contains(path)) {
            config.set(path, value);
        }
    }
    
    public static void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            ArcaniteCrystals.getInstance().getLogger().log(Level.SEVERE, "Could not save config to " + configFile, e);
        }
    }
    
    public static FileConfiguration getConfig() {
        return config;
    }
    
    public static void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        loadDefaults();
    }
    
    public static FileConfiguration getUpgradesConfig() {
        return upgradesConfig;
    }
    
    public static FileConfiguration getLevelsConfig() {
        return levelsConfig;
    }
    
    public static FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
    
    public static String backupConfig() {
        try {
            File dataFolder = ArcaniteCrystals.getInstance().getDataFolder();
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File backupFolder = new File(dataFolder, "backups");
            backupFolder.mkdirs();
            
            File backupFile = new File(backupFolder, "config_backup_" + timestamp + ".yml");
            Files.copy(new File(dataFolder, "config.yml").toPath(), backupFile.toPath());
            
            return backupFile.getPath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
