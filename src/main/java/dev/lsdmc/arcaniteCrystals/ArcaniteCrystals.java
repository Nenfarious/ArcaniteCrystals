// src/main/java/dev/lsdmc/arcaniteCrystals/ArcaniteCrystals.java
package dev.lsdmc.arcaniteCrystals;

import dev.lsdmc.arcaniteCrystals.command.ArcaniteCommand;
import dev.lsdmc.arcaniteCrystals.command.LevelUpCommand;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.DatabaseManager;
import dev.lsdmc.arcaniteCrystals.listener.CrystalListener;
import dev.lsdmc.arcaniteCrystals.manager.UpgradeManager;
import dev.lsdmc.arcaniteCrystals.manager.CrystalRecipeManager;
import dev.lsdmc.arcaniteCrystals.manager.CrystalCraftingManager;
import dev.lsdmc.arcaniteCrystals.placeholder.ArcaniteExpansion;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import dev.lsdmc.arcaniteCrystals.manager.PlayerStatisticsManager;
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
        
        // Register catalyst recipes
        CrystalRecipeManager.registerRecipes();
        
        // Register crystal crafting recipes
        CrystalCraftingManager.registerRecipes();
        
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
        
        // BEGIN PATCH stop crystal manager tasks
        dev.lsdmc.arcaniteCrystals.manager.CrystalManager.shutdown();
        // END PATCH
        
        // Cleanup server level manager
        dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.cleanup();
        
        getLogger().info("ArcaniteCrystals has been disabled!");
    }
    
    private void initializeManagers() {
        // Initialize database
        DatabaseManager.initialize(this);
        
        // Initialize crystal listener
        crystalListener = new CrystalListener();
        
        // Initialize statistics manager
        new PlayerStatisticsManager();
        
        // BEGIN PATCH initialize crystal manager
        dev.lsdmc.arcaniteCrystals.manager.CrystalManager.initialize();
        // END PATCH
    }
    
    private void registerCommands() {
        getCommand("arcanite").setExecutor(new ArcaniteCommand());
        getCommand("arcanite").setTabCompleter(new ArcaniteCommand());
        
        // Register levelup command
        getCommand("levelup").setExecutor(new LevelUpCommand());
    }
    
    private void registerListeners() {
        // Register crystal listener
        getServer().getPluginManager().registerEvents(crystalListener, this);
        
        // Register crystal offhand listener for proper effect handling
        getServer().getPluginManager().registerEvents(new dev.lsdmc.arcaniteCrystals.listener.CrystalOffhandListener(), this);
        
        // Register crystal crafting listener
        getServer().getPluginManager().registerEvents(new CrystalCraftingManager(), this);
        
        // Register server level manager listener for player join events
        getServer().getPluginManager().registerEvents(new dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager(), this);
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
