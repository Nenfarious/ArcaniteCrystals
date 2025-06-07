// src/main/java/dev/lsdmc/arcaniteCrystals/ArcaniteCrystals.java
package dev.lsdmc.arcaniteCrystals;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;

import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.command.*;
import dev.lsdmc.arcaniteCrystals.config.ConfigValidator;
import dev.lsdmc.arcaniteCrystals.database.DatabaseManager;
import dev.lsdmc.arcaniteCrystals.listener.*;
import dev.lsdmc.arcaniteCrystals.manager.EffectApplierManager;
import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import dev.lsdmc.arcaniteCrystals.placeholder.ArcaniteExpansion;

public class ArcaniteCrystals extends JavaPlugin {

    private static ArcaniteCrystals instance;
    public static ArcaniteCrystals getInstance() { return instance; }

    @Override
    public void onEnable() {
        instance = this;

        // Check dependencies first
        if (!checkDependencies()) {
            getLogger().severe("Required dependencies not found! Plugin will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Core configuration
        ConfigManager.initialize(this);
        if (!ConfigValidator.validate(this)) {
            getLogger().severe("Configuration validation failed! Check the errors above.");
            getLogger().severe("Plugin will continue but may not function correctly.");
        }

        // Database setup with proper error handling
        if (!DatabaseManager.initialize(this)) {
            getLogger().severe("Database initialization failed! Plugin cannot function.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers
        CrystalManager.initialize();

        // Register core commands
        registerCommands();

        // Register event listeners
        registerEventListeners();

        // Start effect management
        EffectApplierManager.start(this);

        // PlaceholderAPI integration
        setupPlaceholderAPI();

        getLogger().info("ArcaniteCrystals v" + getDescription().getVersion() + " enabled successfully!");
        getLogger().info("Database mode: " + DatabaseManager.getCurrentMode());
        getLogger().info("Core features: Crystal system, Level progression, Upgrade menu, Admin tools");
        getLogger().info("Crystal Manager: Initialized with energy system and cooldown management");
    }

    /**
     * Check for required dependencies and log status.
     */
    private boolean checkDependencies() {
        boolean allDependenciesFound = true;
        
        // Check for Vault (required)
        Plugin vault = getServer().getPluginManager().getPlugin("Vault");
        if (vault == null || !vault.isEnabled()) {
            getLogger().severe("Vault plugin not found or disabled! This plugin is required for economy features.");
            allDependenciesFound = false;
        } else {
            getLogger().info("✓ Vault dependency found: " + vault.getDescription().getVersion());
        }
        
        // Check for PlaceholderAPI (optional)
        Plugin placeholderAPI = getServer().getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderAPI == null || !placeholderAPI.isEnabled()) {
            getLogger().info("PlaceholderAPI not found - placeholder support will be disabled");
        } else {
            getLogger().info("✓ PlaceholderAPI dependency found: " + placeholderAPI.getDescription().getVersion());
        }
        
        return allDependenciesFound;
    }

    /**
     * Register all plugin commands with proper error handling.
     */
    private void registerCommands() {
        try {
            getCommand("arcanite").setExecutor(new ArcaniteCommand());
            getCommand("arcanite").setTabCompleter(new ArcaniteCommand());
            getCommand("givecrystal").setExecutor(new GiveCommand());
            getCommand("mysterycrystal").setExecutor(new MysteryCommand());
            getCommand("levelup").setExecutor(new LevelUpCommand());
            
            getLogger().info("Successfully registered all commands");
        } catch (Exception e) {
            getLogger().severe("Error registering commands: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Register all event listeners with proper error handling.
     */
    private void registerEventListeners() {
        try {
            getServer().getPluginManager().registerEvents(new CrystalUseListener(), this);
            getServer().getPluginManager().registerEvents(new RechargeListener(), this);
            getServer().getPluginManager().registerEvents(new MiningListener(), this);
            getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
            getServer().getPluginManager().registerEvents(new dev.lsdmc.arcaniteCrystals.manager.CrystalCraftingManager(), this);
            
            getLogger().info("Successfully registered all event listeners");
        } catch (Exception e) {
            getLogger().severe("Error registering event listeners: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Setup PlaceholderAPI integration with proper error handling.
     */
    private void setupPlaceholderAPI() {
        try {
            if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                new ArcaniteExpansion().register();
                getLogger().info("PlaceholderAPI expansion registered successfully.");
            }
        } catch (Exception e) {
            getLogger().warning("Error setting up PlaceholderAPI: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        try {
            // Shutdown managers in reverse order
            CrystalManager.shutdown();
            EffectApplierManager.stop();
            DatabaseManager.shutdown();
            
            getLogger().info("ArcaniteCrystals disabled safely.");
        } catch (Exception e) {
            getLogger().severe("Error during plugin shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
