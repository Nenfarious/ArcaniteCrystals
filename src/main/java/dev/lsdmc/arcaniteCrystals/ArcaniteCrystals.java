// src/main/java/dev/lsdmc/arcaniteCrystals/ArcaniteCrystals.java
package dev.lsdmc.arcaniteCrystals;

import dev.lsdmc.arcaniteCrystals.command.ArcaniteCommand;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.DatabaseManager;
import dev.lsdmc.arcaniteCrystals.listener.CrystalListener;
import dev.lsdmc.arcaniteCrystals.manager.UpgradeManager;
import dev.lsdmc.arcaniteCrystals.placeholder.ArcaniteExpansion;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Main plugin class for ArcaniteCrystals.
 */
public class ArcaniteCrystals extends JavaPlugin {
    private static ArcaniteCrystals instance;
    private static Economy economy = null;
    private CrystalListener crystalListener;

    @Override
    public void onEnable() {
        instance = this;

        // Load configuration
        ConfigManager.initialize();

        // Initialize managers
        initializeManagers();

        // Register commands
        registerCommands();

        // Register listeners
        registerListeners();
        
        // Setup economy if available
        setupEconomy();
        
        // Register PlaceholderAPI expansion if available
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ArcaniteExpansion().register();
            getLogger().info("PlaceholderAPI expansion registered!");
        }
        
        getLogger().info("ArcaniteCrystals has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Cleanup managers
        UpgradeManager.cleanup();
        
        // Close database connections
        DatabaseManager.shutdown();
        
        getLogger().info("ArcaniteCrystals has been disabled!");
    }
    
    private void initializeManagers() {
        // Initialize database
        DatabaseManager.initialize(this);
        
        // Initialize crystal listener
        crystalListener = new CrystalListener();
    }
    
    private void registerCommands() {
        getCommand("arcanite").setExecutor(new ArcaniteCommand());
        getCommand("arcanite").setTabCompleter(new ArcaniteCommand());
    }
    
    private void registerListeners() {
        // Register crystal listener
        getServer().getPluginManager().registerEvents(crystalListener, this);
    }
    
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found! Economy features will be disabled.");
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("No economy plugin found! Economy features will be disabled.");
            return false;
        }
        
        economy = rsp.getProvider();
        return economy != null;
    }
    
    /**
     * Gets the plugin instance.
     */
    public static ArcaniteCrystals getInstance() {
        return instance;
    }
    
    /**
     * Gets the economy provider.
     */
    public static Economy getEconomy() {
        return economy;
    }
    
    /**
     * Gets the crystal listener.
     */
    public CrystalListener getCrystalListener() {
        return crystalListener;
    }
}
