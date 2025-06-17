// src/main/java/dev/lsdmc/arcaniteCrystals/manager/CrystalManager.java
package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.util.EffectUtils;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.gson.Gson;

/**
 * Comprehensive crystal management system handling activation, energy, cooldowns,
 * effects, security, and all crystal-related mechanics.
 */
public class CrystalManager {

    public static final NamespacedKey KEY_CRYSTAL = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal");
    public static final NamespacedKey KEY_ABILITIES = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal_abilities");
    public static final NamespacedKey KEY_ENERGY = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal_energy");
    public static final NamespacedKey KEY_TIER = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal_tier");

    // NBT Keys for crystal data
    private static final NamespacedKey CRYSTAL_ID_KEY = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal_id");
    private static final NamespacedKey CRYSTAL_ENERGY_KEY = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal_energy");
    private static final NamespacedKey CRYSTAL_EFFECTS_KEY = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal_effects");
    private static final NamespacedKey CRYSTAL_CREATED_KEY = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal_created");
    private static final NamespacedKey CRYSTAL_ACTIVATED_KEY = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal_activated");
    private static final NamespacedKey CRYSTAL_TYPE_KEY = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal_type");
    
    // Active crystal tracking
    private static final Map<UUID, ItemStack> activeCrystals = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastEffectTick = new ConcurrentHashMap<>();
    private static final Random random = new Random();
    
    // Configuration cache
    private static int maxEnergy;
    private static int energyDrain;
    private static long cooldownMs;
    private static Material crystalMaterial;
    private static Material rechargeMaterial;

    /**
     * Initializes the crystal manager with configuration values.
     */
    public static void initialize() {
        try {
            // Load configuration
            maxEnergy = ConfigManager.getConfig().getInt("crystal.energy", 18000);
            energyDrain = ConfigManager.getConfig().getInt("crystal.drain", 80);
            cooldownMs = ConfigManager.getConfig().getLong("crystal.cooldown", 300) * 1000;
            
            String matName = ConfigManager.getConfig().getString("crystal.material", "DIAMOND");
            crystalMaterial = Material.matchMaterial(matName);
            if (crystalMaterial == null) crystalMaterial = Material.DIAMOND;
            
            String rechargeName = ConfigManager.getConfig().getString("crystal.recharge-material", "QUARTZ");
            rechargeMaterial = Material.matchMaterial(rechargeName);
            if (rechargeMaterial == null) rechargeMaterial = Material.QUARTZ;
            
            // Start energy drain task
            startEnergyDrainTask();
            
            // Start aura effect task
            startAuraEffectTask();
            
            // Start effect applier
            EffectApplierManager.start(ArcaniteCrystals.getInstance());
            
            ArcaniteCrystals.getInstance().getLogger().info("CrystalManager initialized successfully");
        } catch (Exception e) {
            ArcaniteCrystals.getInstance().getLogger().severe("Error initializing CrystalManager: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Loads configuration values into cache.
     */
    private static void loadConfiguration() {
        FileConfiguration config = ConfigManager.getConfig();
        
        // Load crystal settings
        maxEnergy = config.getInt("crystal.energy", 18000);
        energyDrain = config.getInt("crystal.drain", 80);
        cooldownMs = config.getLong("crystal.cooldown", 300) * 1000L;
        
        // Load crystal material
        String matName = config.getString("crystal.material", "DIAMOND");
        crystalMaterial = Material.matchMaterial(matName);
        if (crystalMaterial == null) {
            crystalMaterial = Material.DIAMOND;
            ArcaniteCrystals.getInstance().getLogger().warning(
                "Invalid crystal material '" + matName + "', defaulting to DIAMOND");
        }
        
        // Load recharge material
        String rechargeMatName = config.getString("crystal.recharge-material", "QUARTZ");
        rechargeMaterial = Material.matchMaterial(rechargeMatName);
        if (rechargeMaterial == null) {
            rechargeMaterial = Material.QUARTZ;
            ArcaniteCrystals.getInstance().getLogger().warning(
                "Invalid recharge material '" + rechargeMatName + "', defaulting to QUARTZ");
        }
    }

    /**
     * Checks if an item is an Arcanite Crystal.
     */
    public static boolean isCrystal(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        try {
            return item.getItemMeta().getPersistentDataContainer().has(KEY_CRYSTAL, PersistentDataType.BYTE);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if a crystal is activated.
     */
    public static boolean isActivatedCrystal(ItemStack crystal) {
        if (!isCrystal(crystal)) return false;
        
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) return false;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(CRYSTAL_ACTIVATED_KEY, PersistentDataType.BYTE) &&
               container.get(CRYSTAL_ACTIVATED_KEY, PersistentDataType.BYTE) == 1;
    }

    /**
     * Checks if a crystal is depleted (has no energy).
     */
    public static boolean isDepletedCrystal(ItemStack item) {
        if (!isCrystal(item)) return false;
        try {
            Integer energy = item.getItemMeta().getPersistentDataContainer().get(KEY_ENERGY, PersistentDataType.INTEGER);
            return energy != null && energy <= 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gives a new blank crystal to a player.
     */
    public static void giveNewCrystal(Player player) {
        try {
            String matName = ConfigManager.getConfig().getString("crystal.material", "DIAMOND");
            Material material = Material.matchMaterial(matName);
            if (material == null) material = Material.DIAMOND;
            
            List<String> lore;
            if (player.hasPermission("arcanite.admin")) {
                lore = Arrays.asList(
                    ChatColor.GRAY + "(Unidentified Crystal)",
                    ChatColor.DARK_GRAY + "Right-click to identify", // admin hint
                    ChatColor.DARK_PURPLE + "✨ Mystical Energy Contained ✨"
                );
            } else {
                lore = Arrays.asList(
                    ChatColor.GRAY + "(Unidentified Crystal)",
                    ChatColor.DARK_PURPLE + "✨ Mystical Energy Contained ✨"
                );
            }
            
            ItemStack crystal = EffectUtils.buildCrystalItem(
                    material,
                    ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "ARCANITE CRYSTAL",
                    lore
            );
            
            // Mark as crystal
            ItemMeta meta = crystal.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(KEY_CRYSTAL, PersistentDataType.BYTE, (byte) 1);
                crystal.setItemMeta(meta);
            }

            // Give to player
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(crystal);
            leftovers.values().forEach(item ->
                    player.getWorld().dropItemNaturally(player.getLocation(), item)
            );
            
            // Success feedback
            MessageManager.sendNotification(player, 
                MessageManager.SUCCESS + "✨ CRYSTAL RECEIVED ✨\n" + 
                MessageManager.MUTED + "Right-click to activate your new crystal!",
                MessageManager.NotificationType.SUCCESS);
                
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Error giving crystal: " + e.getMessage());
            ArcaniteCrystals.getInstance().getLogger().warning("Error giving crystal to " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Activates a crystal for a player (one-time activation only).
     */
    public static boolean activateCrystal(Player player, ItemStack crystal) {
        if (!isCrystal(crystal)) return false;
        
        // Check if crystal is already permanently activated
        if (isActivatedCrystal(crystal)) {
            player.sendMessage(ChatColor.YELLOW + "This crystal has already been activated! Place it in your off-hand to use its effects.");
            return false;
        }
        
        List<String> effects = getCrystalEffects(crystal);
        if (effects.isEmpty()) {
            player.sendMessage(ChatColor.RED + "This crystal has no effects! Right-click to identify it first.");
            return false;
        }
        
        int energy = getEnergy(crystal);
        if (energy <= 0) {
            player.sendMessage(ChatColor.RED + "This crystal is depleted! Use quartz to recharge it.");
            SoundManager.playCrystalDepletionSound(player);
            return false;
        }
        
        // Check cooldown
        if (isOnCooldown(player)) {
            long remaining = getRemainingCooldown(player);
            player.sendMessage(ChatColor.RED + "Crystal is on cooldown for " + formatTime(remaining / 1000) + "!");
            SoundManager.playCooldownSound(player);
            ParticleManager.playCooldownWarningEffect(player);
            return false;
        }
        
        // PERMANENTLY mark crystal as activated (one-time only)
        ItemMeta meta = crystal.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(CRYSTAL_ACTIVATED_KEY, PersistentDataType.BYTE, (byte) 1);
        crystal.setItemMeta(meta);
        
        // Set cooldown for activation
        PlayerDataManager.setCooldown(player.getUniqueId(), System.currentTimeMillis() + cooldownMs);
        
        // Feedback
        String effectNames = effects.stream()
                .map(CrystalManager::beautifyEffectName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("Unknown");
        
        player.sendMessage(ChatColor.GREEN + "✨ Crystal permanently activated!");
        player.sendMessage(ChatColor.YELLOW + "Effects: " + ChatColor.WHITE + effectNames);
        player.sendMessage(ChatColor.GRAY + "Hold in off-hand to use the effects.");
        
        MessageManager.sendCrystalActivated(player, effects, energy);
        SoundManager.playCrystalActivateSound(player);
        ParticleManager.playCrystalActivationEffect(player);
        
        return true;
    }
    
    /**
     * Starts crystal effects when equipped in offhand (for activated crystals only).
     */
    public static boolean startCrystalEffects(Player player, ItemStack crystal) {
        if (!isCrystal(crystal) || !isActivatedCrystal(crystal)) return false;
        
        List<String> effects = getCrystalEffects(crystal);
        if (effects.isEmpty()) return false;
        
        int energy = getEnergy(crystal);
        if (energy <= 0) {
            player.sendMessage(ChatColor.RED + "This crystal is depleted! Use quartz to recharge it.");
            return false;
        }
        
        // Start tracking this crystal for effects
        activeCrystals.put(player.getUniqueId(), crystal.clone());
        
        // Apply effects
        applyEffects(player, effects);
        
        // Start effect applier for this player
        EffectApplierManager.addActive(player.getUniqueId());
        
        player.sendMessage(ChatColor.GREEN + "Crystal effects are now active!");
        
        return true;
    }
    
    /**
     * Stops crystal effects when removed from offhand.
     */
    public static void stopCrystalEffects(Player player) {
        ItemStack activeCrystal = activeCrystals.remove(player.getUniqueId());
        if (activeCrystal == null) return;
        
        // Remove effects
        removeEffects(player, getCrystalEffects(activeCrystal));
        
        // Remove from effect applier
        EffectApplierManager.removeActive(player.getUniqueId());
        
        player.sendMessage(ChatColor.GRAY + "Crystal effects have stopped.");
    }

    /**
     * Applies potion effects based on crystal effects.
     */
    private static void applyEffects(Player player, List<String> effectIds) {
        for (String effectId : effectIds) {
            ConfigurationSection upgrade = ConfigManager.getUpgradesConfig()
                    .getConfigurationSection("upgrades." + effectId);
            
            if (upgrade == null) continue;
            
            String effectName = upgrade.getString("effect", "SPEED");
            int amplifier = upgrade.getInt("amplifier", 0);
            
            try {
                PotionEffectType effectType = PotionEffectType.getByName(effectName);
                if (effectType != null) {
                    PotionEffect effect = new PotionEffect(effectType, Integer.MAX_VALUE, amplifier, false, false);
                    player.addPotionEffect(effect, true);
                }
            } catch (Exception e) {
                ArcaniteCrystals.getInstance().getLogger().warning("Invalid effect: " + effectName);
            }
        }
    }
    
    /**
     * Removes potion effects.
     */
    private static void removeEffects(Player player, List<String> effectIds) {
        for (String effectId : effectIds) {
            ConfigurationSection upgrade = ConfigManager.getUpgradesConfig()
                    .getConfigurationSection("upgrades." + effectId);
            
            if (upgrade == null) continue;
            
            String effectName = upgrade.getString("effect", "SPEED");
            
            try {
                PotionEffectType effectType = PotionEffectType.getByName(effectName);
                if (effectType != null) {
                    player.removePotionEffect(effectType);
                }
            } catch (Exception e) {
                // Ignore errors when removing effects
            }
        }
    }
    
    /**
     * Recharges a depleted crystal.
     */
    public static void rechargeCrystal(Player player) {
        try {
            ItemStack crystal = player.getInventory().getItemInOffHand();
            if (!isDepletedCrystal(crystal)) {
                player.sendMessage(ChatColor.YELLOW + "Crystal doesn't need recharging!");
                return;
            }

            // Remove recharge material
            String matName = ConfigManager.getConfig().getString("recharge.material", "QUARTZ");
            Material rechargeMat = Material.matchMaterial(matName);
            if (rechargeMat != null) {
                if (player.getInventory().containsAtLeast(new ItemStack(rechargeMat), 1)) {
                    player.getInventory().removeItem(new ItemStack(rechargeMat, 1));
                } else {
                    player.sendMessage(ChatColor.RED + "You need " + rechargeMat.name().replace("_", " ") + " to recharge!");
                    return;
                }
            }

            // Recharge crystal
            ItemMeta meta = crystal.getItemMeta();
            if (meta == null) return;

            int maxEnergy = ConfigManager.getConfig().getInt("crystal.energy", 18000);
            int rechargedEnergy = maxEnergy / 2; // Half energy on recharge
            meta.getPersistentDataContainer().set(KEY_ENERGY, PersistentDataType.INTEGER, rechargedEnergy);

            // Get abilities for lore update
            String abilitiesData = meta.getPersistentDataContainer().get(KEY_ABILITIES, PersistentDataType.STRING);
            List<String> abilities = (abilitiesData != null && !abilitiesData.isEmpty()) 
                    ? Arrays.asList(abilitiesData.split(","))
                    : Collections.emptyList();
            
            // Update lore and restore appearance
            updateCrystalLore(crystal, meta, abilities, rechargedEnergy, maxEnergy);
            
            String crystalMatName = ConfigManager.getConfig().getString("crystal.material", "DIAMOND");
            Material crystalMat = Material.matchMaterial(crystalMatName);
            if (crystalMat != null) {
                crystal.setType(crystalMat);
            }
            
            crystal.setItemMeta(meta);

            // Success feedback
            SoundManager.playCrystalRechargeSound(player);
            ParticleManager.playCrystalRechargeEffect(player);
            
            MessageManager.sendNotification(player,
                MessageManager.SUCCESS + "⚡ CRYSTAL RECHARGED ⚡\n" + 
                MessageManager.MUTED + "Energy restored to " + 
                MessageManager.ACCENT + rechargedEnergy + "/" + maxEnergy,
                MessageManager.NotificationType.SUCCESS);
                
            MessageManager.showTitle(player, "⚡ RECHARGED ⚡", 
                "Energy: " + rechargedEnergy + "/" + maxEnergy);
                
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Error recharging crystal: " + e.getMessage());
            ArcaniteCrystals.getInstance().getLogger().warning("Error recharging crystal for " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Gets abilities from a crystal (legacy method for compatibility).
     */
    public static List<String> getAbilities(ItemStack crystal) {
        return getCrystalEffects(crystal);
    }

    /**
     * Gets the energy from a crystal using NBT as single source of truth.
     */
    public static int getEnergy(ItemStack item) {
        if (!isCrystal(item)) return 0;
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return 0;
            
            Integer energy = meta.getPersistentDataContainer().get(KEY_ENERGY, PersistentDataType.INTEGER);
            return energy != null ? Math.max(0, energy) : 0;
        } catch (Exception e) {
            ArcaniteCrystals.getInstance().getLogger().warning("Error getting crystal energy: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Updates crystal lore with current status.
     */
    public static void updateCrystalLore(ItemStack crystal, ItemMeta meta, List<String> effects, int currentEnergy, int maxEnergy) {
        List<String> lore = new ArrayList<>();
        
        // Add crystal type
        lore.add(ChatColor.LIGHT_PURPLE + "✦ Arcanite Crystal ✦");
        lore.add("");
        
        // Add activation status
        if (isActivatedCrystal(crystal)) {
            lore.add(ChatColor.GREEN + "✓ Activated");
        } else {
            lore.add(ChatColor.YELLOW + "⚠ Not Activated");
            lore.add(ChatColor.GRAY + "Right-click to activate");
        }
        lore.add("");
        
        // Add effects
        if (!effects.isEmpty()) {
            lore.add(ChatColor.BLUE + "Effects:");
            for (String effect : effects) {
                lore.add(ChatColor.GRAY + "► " + ChatColor.WHITE + beautifyEffectName(effect));
            }
            lore.add("");
        }
        
        // Add energy bar with visual representation
        lore.add(ChatColor.AQUA + "Energy: " + formatEnergyBar(currentEnergy, maxEnergy));
        lore.add(ChatColor.GRAY + String.valueOf(currentEnergy) + "/" + maxEnergy);
        
        // Add usage instructions
        lore.add("");
        if (currentEnergy <= 0) {
            lore.add(ChatColor.RED + "⚠ Depleted - Use Quartz to recharge");
        } else if (isActivatedCrystal(crystal)) {
            lore.add(ChatColor.GREEN + "Hold in off-hand to use effects");
        } else {
            lore.add(ChatColor.YELLOW + "Right-click to activate first");
        }
        
        meta.setLore(lore);
        crystal.setItemMeta(meta);
    }
    
    /**
     * Formats an energy bar with visual representation.
     */
    private static String formatEnergyBar(int current, int max) {
        int bars = 10;
        int filled = (int) ((double) current / max * bars);
        
        StringBuilder bar = new StringBuilder();
        bar.append(ChatColor.GRAY).append("[");
        
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                bar.append(ChatColor.GREEN).append("█");
            } else {
                bar.append(ChatColor.DARK_GRAY).append("░");
            }
        }
        
        bar.append(ChatColor.GRAY).append("]");
        return bar.toString();
    }
    
    /**
     * Beautify an effect name for display
     */
    public static String beautifyEffectName(String effectId) {
        return switch (effectId.toLowerCase()) {
            case "speed" -> "Swift Movement";
            case "regeneration" -> "Life Regeneration";
            case "jump_boost" -> "Enhanced Jumping";
            case "haste" -> "Mining Speed";
            case "strength" -> "Combat Power";
            case "damage_resistance" -> "Damage Resistance";
            default -> effectId.replace("_", " ");
        };
    }

    /**
     * Creates a blank Arcanite Crystal with secure metadata.
     */
    public static ItemStack createBlankCrystal() {
        ItemStack crystal = new ItemStack(crystalMaterial);
        ItemMeta meta = crystal.getItemMeta();
        
        // Set display information
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
            ConfigManager.getMessagesConfig().getString("lore.crystalName", "&dARCANITE CRYSTAL")));
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', 
            ConfigManager.getMessagesConfig().getString("lore.blank", "&7(Unidentified)")));
        lore.add(ChatColor.translateAlternateColorCodes('&', 
            ConfigManager.getMessagesConfig().getString("lore.holdOffHand", "&7(Hold In Off-Hand)")));
        meta.setLore(lore);
        
        // Set secure metadata
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(CRYSTAL_ID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
        container.set(CRYSTAL_ENERGY_KEY, PersistentDataType.INTEGER, 0);
        container.set(CRYSTAL_EFFECTS_KEY, PersistentDataType.STRING, "");
        container.set(CRYSTAL_CREATED_KEY, PersistentDataType.LONG, System.currentTimeMillis());
        container.set(CRYSTAL_ACTIVATED_KEY, PersistentDataType.BYTE, (byte) 0);
        
        crystal.setItemMeta(meta);
        return crystal;
    }
    
    /**
     * Creates a mystery crystal with random effects based on player level.
     */
    public static ItemStack createMysteryCrystal(Player player) {
        // Determine available effects based on player level
        int playerLevel = PlayerDataManager.getLevel(player.getUniqueId());
        int maxTier = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getMaxTier(player.getUniqueId());
        int slots = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getSlots(player.getUniqueId());
        Set<String> unlockedUpgrades = PlayerDataManager.getUnlockedUpgrades(player.getUniqueId());
        
        // Select random effects from unlocked upgrades
        List<String> availableEffects = new ArrayList<>(unlockedUpgrades);
        Random random = new Random();
        Collections.shuffle(availableEffects, random);
        
        List<String> selectedEffects = new ArrayList<>();
        int effectCount = Math.min(slots, Math.min(availableEffects.size(), 3)); // Max 3 effects
        
        for (int i = 0; i < effectCount; i++) {
            selectedEffects.add(availableEffects.get(i));
        }
        
        return createMysteryCrystal(player, selectedEffects);
    }
    
    /**
     * Creates a mystery crystal with specified effects.
     */
    public static ItemStack createMysteryCrystal(Player player, List<String> effects) {
        String crystalMatName = ConfigManager.getConfig().getString("crystal.material", "DIAMOND");
        Material crystalMat = Material.matchMaterial(crystalMatName);
        if (crystalMat == null) crystalMat = Material.DIAMOND;
        
        ItemStack crystal = new ItemStack(crystalMat);
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) return crystal;
        
        // Mark as an Arcanite crystal
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(KEY_CRYSTAL, PersistentDataType.BYTE, (byte) 1);

        // Store effects under both modern and legacy keys for safety
        String effectCsv = String.join(",", effects);
        container.set(KEY_ABILITIES, PersistentDataType.STRING, effectCsv);
        container.set(CRYSTAL_EFFECTS_KEY, PersistentDataType.STRING, effectCsv);

        // Set initial energy
        int initialEnergy = ConfigManager.getConfig().getInt("crystal.energy", 18000);
        container.set(KEY_ENERGY, PersistentDataType.INTEGER, initialEnergy);

        // Standard display name
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "ARCANITE CRYSTAL");

        // Update lore
        updateCrystalLore(crystal, meta, effects, initialEnergy, initialEnergy);

        crystal.setItemMeta(meta);
        return crystal;
    }
    
    /**
     * Creates a crystal with specific effects.
     */
    public static ItemStack createCrystalWithEffects(List<String> effects) {
        ItemStack crystal = createBlankCrystal();
        ItemMeta meta = crystal.getItemMeta();
        
        // Update metadata with effects
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(CRYSTAL_EFFECTS_KEY, PersistentDataType.STRING, String.join(",", effects));
        container.set(CRYSTAL_ENERGY_KEY, PersistentDataType.INTEGER, maxEnergy);
        
        // Update display
        updateCrystalDisplay(crystal, effects, maxEnergy);
        
        return crystal;
    }
    
    /**
     * Updates crystal display with current state.
     */
    private static void updateCrystalDisplay(ItemStack crystal, List<String> effects, int energy) {
        if (crystal == null || !isCrystal(crystal)) return;
        
        ItemMeta meta = crystal.getItemMeta();
        List<String> lore = new ArrayList<>();
        
        if (effects.isEmpty()) {
            lore.add(ChatColor.translateAlternateColorCodes('&', 
                ConfigManager.getMessagesConfig().getString("lore.blank", "&7(Unidentified)")));
        } else {
            // Add effect lines
            for (String effectId : effects) {
                String effectDisplay = beautifyEffectName(effectId);
                lore.add(ChatColor.translateAlternateColorCodes('&', 
                    ConfigManager.getMessagesConfig().getString("lore.abilityLine", "&f - {ability} {arcanite_effect_level}")
                        .replace("{ability}", effectDisplay)
                        .replace("{arcanite_effect_level}", "")));
            }
            
            lore.add("");
            
            // Add energy bar
            if (energy > 0) {
                String timeLeft = formatTime(energy / 20); // Convert ticks to seconds
                lore.add(ChatColor.translateAlternateColorCodes('&', 
                    ConfigManager.getMessagesConfig().getString("lore.timeLeft", "&7Time Left: &b{time}")
                        .replace("{time}", timeLeft)));
            } else {
                lore.add(ChatColor.RED + "Depleted - Use Quartz to recharge");
            }
        }
        
        lore.add(ChatColor.translateAlternateColorCodes('&', 
            ConfigManager.getMessagesConfig().getString("lore.holdOffHand", "&7(Hold In Off-Hand)")));
        
        meta.setLore(lore);
        crystal.setItemMeta(meta);
    }
    
    /**
     * Checks if a crystal is rechargeable (depleted but valid).
     */
    public static boolean isRechargeable(ItemStack item) {
        if (!isCrystal(item)) return false;
        
        return getEnergy(item) == 0 && !getCrystalEffects(item).isEmpty();
    }
    
    /**
     * Gets crystal effects from metadata.
     */
    public static List<String> getCrystalEffects(ItemStack crystal) {
        if (!isCrystal(crystal)) return new ArrayList<>();

        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) return new ArrayList<>();

        PersistentDataContainer container = meta.getPersistentDataContainer();
        // Prefer the modern KEY_ABILITIES key, but allow legacy CRYSTAL_EFFECTS_KEY for backward compatibility
        String effectsStr = container.get(KEY_ABILITIES, PersistentDataType.STRING);
        if (effectsStr == null || effectsStr.isEmpty()) {
            effectsStr = container.get(CRYSTAL_EFFECTS_KEY, PersistentDataType.STRING);
        }

        if (effectsStr == null || effectsStr.isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.asList(effectsStr.split(","));
    }
    
    /**
     * Sets crystal energy using NBT as single source of truth with validation.
     */
    public static void setEnergy(ItemStack item, int energy) {
        if (!isCrystal(item)) return;
        
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            
            // Validate energy bounds
            int clampedEnergy = Math.max(0, Math.min(energy, maxEnergy));
            
            meta.getPersistentDataContainer().set(KEY_ENERGY, PersistentDataType.INTEGER, clampedEnergy);
            item.setItemMeta(meta);
            
            // Update visual appearance based on energy level
            updateCrystalAppearance(item, clampedEnergy);
            
        } catch (Exception e) {
            ArcaniteCrystals.getInstance().getLogger().warning("Error setting crystal energy: " + e.getMessage());
        }
    }
    
    /**
     * Updates crystal appearance based on energy level.
     */
    private static void updateCrystalAppearance(ItemStack crystal, int energy) {
        try {
            if (energy <= 0) {
                // Depleted crystal appearance
                crystal.setType(Material.GRAY_DYE);
            } else {
                // Active crystal appearance
                String matName = ConfigManager.getConfig().getString("crystal.material", "DIAMOND");
                Material activeMaterial = Material.matchMaterial(matName);
                if (activeMaterial != null) {
                    crystal.setType(activeMaterial);
                }
            }
        } catch (Exception e) {
            ArcaniteCrystals.getInstance().getLogger().warning("Error updating crystal appearance: " + e.getMessage());
        }
    }
    
    /**
     * Synchronizes player's off-hand crystal with active crystal state.
     */
    public static void synchronizeCrystalState(Player player) {
        try {
            ItemStack offHandCrystal = player.getInventory().getItemInOffHand();
            ItemStack activeCrystal = activeCrystals.get(player.getUniqueId());
            
            if (activeCrystal != null && isCrystal(offHandCrystal)) {
                // Sync energy from memory to off-hand item
                int memoryEnergy = getEnergy(activeCrystal);
                setEnergy(offHandCrystal, memoryEnergy);
                
                // Update the active crystal in memory to match
                activeCrystals.put(player.getUniqueId(), offHandCrystal.clone());
            }
        } catch (Exception e) {
            ArcaniteCrystals.getInstance().getLogger().warning("Error synchronizing crystal state for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Checks if player is on crystal cooldown.
     */
    public static boolean isOnCooldown(Player player) {
        long cooldownEnd = PlayerDataManager.getCooldown(player.getUniqueId());
        return System.currentTimeMillis() < cooldownEnd;
    }
    
    /**
     * Gets remaining cooldown in milliseconds.
     */
    public static long getRemainingCooldown(Player player) {
        long cooldownEnd = PlayerDataManager.getCooldown(player.getUniqueId());
        return Math.max(0, cooldownEnd - System.currentTimeMillis());
    }
    
    /**
     * Checks if player has an active crystal.
     */
    public static boolean hasActiveCrystal(Player player) {
        return activeCrystals.containsKey(player.getUniqueId());
    }
    
    /**
     * Gets the active crystal for a player.
     */
    public static ItemStack getActiveCrystal(Player player) {
        return activeCrystals.get(player.getUniqueId());
    }
    
    /**
     * Handles player disconnection - cleans up active crystals.
     */
    public static void handlePlayerDisconnect(Player player) {
        stopCrystalEffects(player);
        lastEffectTick.remove(player.getUniqueId());
    }
    
    /**
     * Starts the energy drain task for active crystals.
     */
    private static void startEnergyDrainTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, ItemStack> entry : activeCrystals.entrySet()) {
                    Player player = ArcaniteCrystals.getInstance().getServer().getPlayer(entry.getKey());
                    if (player == null || !player.isOnline()) {
                        activeCrystals.remove(entry.getKey());
                        continue;
                    }
                    
                    ItemStack crystal = entry.getValue();
                    int currentEnergy = getEnergy(crystal);
                    
                    if (currentEnergy <= 0) {
                        // Crystal depleted
                        stopCrystalEffects(player);
                        
                        player.sendMessage(ChatColor.RED + "Your crystal has been depleted!");
                        SoundManager.playCrystalDepletionSound(player);
                        ParticleManager.playCrystalDepletionEffect(player);
                        continue;
                    }
                    
                    // Drain energy
                    int newEnergy = Math.max(0, currentEnergy - energyDrain);
                    setEnergy(crystal, newEnergy);
                    
                    // Update the crystal in player's inventory
                    ItemStack offhand = player.getInventory().getItemInOffHand();
                    if (isCrystal(offhand)) {
                        setEnergy(offhand, newEnergy);
                        
                        // Update crystal lore with new energy
                        ItemMeta meta = offhand.getItemMeta();
                        if (meta != null) {
                            List<String> effects = getCrystalEffects(offhand);
                            int maxEnergy = getMaxEnergy(offhand);
                            updateCrystalLore(offhand, meta, effects, newEnergy, maxEnergy);
                        }
                    }
                    
                    // Update active crystal in memory
                    activeCrystals.put(entry.getKey(), offhand != null && isCrystal(offhand) ? offhand.clone() : crystal);
                }
            }
        }.runTaskTimer(ArcaniteCrystals.getInstance(), 0L, 20L); // Every second
    }
    
    /**
     * Starts the aura effect task for active crystals.
     */
    private static void startAuraEffectTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID playerId : activeCrystals.keySet()) {
                    Player player = ArcaniteCrystals.getInstance().getServer().getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        ParticleManager.playActiveAuraEffect(player);
                    }
                }
            }
        }.runTaskTimer(ArcaniteCrystals.getInstance(), 0L, 40L); // Every 2 seconds
    }
    
    /**
     * Formats time in seconds to readable format.
     */
    private static String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }
    
    /**
     * Shutdown method for cleanup.
     */
    public static void shutdown() {
        try {
            // Stop effect applier
            EffectApplierManager.stop();
            
            // Clear active crystals
            activeCrystals.clear();
            lastEffectTick.clear();
            
            ArcaniteCrystals.getInstance().getLogger().info("CrystalManager shutdown complete");
        } catch (Exception e) {
            ArcaniteCrystals.getInstance().getLogger().warning("Error during CrystalManager shutdown: " + e.getMessage());
        }
    }

    /**
     * Gets the tier of a crystal.
     */
    public static int getCrystalTier(ItemStack crystal) {
        if (crystal == null || !crystal.hasItemMeta()) {
            return 1;
        }
        
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) {
            return 1;
        }
        
        return meta.getPersistentDataContainer().getOrDefault(
            KEY_TIER,
            PersistentDataType.INTEGER,
            1
        );
    }

    /**
     * Gets the maximum energy for a crystal tier.
     */
    public static int getMaxEnergy(ItemStack crystal) {
        int tier = getCrystalTier(crystal);
        return switch (tier) {
            case 1 -> 100;  // Tier 1: 100 energy
            case 2 -> 200;  // Tier 2: 200 energy
            case 3 -> 300;  // Tier 3: 300 energy
            case 4 -> 500;  // Tier 4: 500 energy
            default -> 100;
        };
    }

    /**
     * Gets a random crystal effect.
     */
    public static String getRandomEffect() {
        String[] effects = {
            "strength",
            "speed",
            "jump_boost",
            "regeneration",
            "resistance",
            "fire_resistance",
            "water_breathing",
            "night_vision",
            "invisibility",
            "glowing"
        };
        return effects[random.nextInt(effects.length)];
    }
    
    /**
     * Gets the current energy of a crystal.
     */
    public static int getCurrentEnergy(ItemStack crystal) {
        if (crystal == null || !crystal.hasItemMeta()) {
            return 0;
        }
        
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) {
            return 0;
        }
        
        return meta.getPersistentDataContainer().getOrDefault(
            KEY_ENERGY,
            PersistentDataType.INTEGER,
            0
        );
    }

    /**
     * Gets the unique ID of a crystal.
     */
    public static String getCrystalId(ItemStack crystal) {
        if (!isCrystal(crystal)) return null;
        
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) return null;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(CRYSTAL_ID_KEY, PersistentDataType.STRING);
    }
    
    /**
     * Sets the unique ID of a crystal.
     */
    public static void setCrystalId(ItemStack crystal, String id) {
        if (!isCrystal(crystal)) return;
        
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) return;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(CRYSTAL_ID_KEY, PersistentDataType.STRING, id);
        crystal.setItemMeta(meta);
    }

    /**
     * Crystal types and their properties
     */
    public enum CrystalType {
        BASIC("Basic Crystal", 1, 10000),
        ENHANCED("Enhanced Crystal", 2, 15000),
        MASTER("Master Crystal", 3, 20000);
        
        private final String displayName;
        private final int maxEffects;
        private final int baseEnergy;
        
        CrystalType(String displayName, int maxEffects, int baseEnergy) {
            this.displayName = displayName;
            this.maxEffects = maxEffects;
            this.baseEnergy = baseEnergy;
        }
        
        public String getDisplayName() { return displayName; }
        public int getMaxEffects() { return maxEffects; }
        public int getBaseEnergy() { return baseEnergy; }
        
        public static CrystalType getRandomType() {
            CrystalType[] types = values();
            return types[random.nextInt(types.length)];
        }
    }
    
    /**
     * Create a basic crystal
     */
    public static ItemStack createBasicCrystal() {
        return createTieredCrystal(CrystalCraftingManager.CrystalTier.APPRENTICE);
    }
    
    /**
     * Create an enhanced crystal
     */
    public static ItemStack createEnhancedCrystal() {
        return createTieredCrystal(CrystalCraftingManager.CrystalTier.ADEPT);
    }
    
    /**
     * Create a master crystal
     */
    public static ItemStack createMasterCrystal() {
        return createTieredCrystal(CrystalCraftingManager.CrystalTier.MASTERWORK);
    }
    
    /**
     * Get crystal type
     */
    public static CrystalType getCrystalType(ItemStack crystal) {
        if (!isCrystal(crystal)) return CrystalType.BASIC;
        
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) return CrystalType.BASIC;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String typeStr = container.get(CRYSTAL_TYPE_KEY, PersistentDataType.STRING);
        
        try {
            return CrystalType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            return CrystalType.BASIC;
        }
    }
    
    /**
     * Generate random effects for a crystal based on player's unlocked upgrades
     */
    public static List<String> generateRandomEffects(Player player, Set<String> unlockedUpgrades, ItemStack crystal) {
        List<String> effects = new ArrayList<>();
        CrystalType type = getCrystalType(crystal);
        
        // Convert upgrades to list for random selection
        List<String> availableEffects = new ArrayList<>(unlockedUpgrades);
        if (availableEffects.isEmpty()) return effects;
        
        // Randomly select effects based on crystal type
        Random random = new Random();
        for (int i = 0; i < type.getMaxEffects() && !availableEffects.isEmpty(); i++) {
            int index = random.nextInt(availableEffects.size());
            effects.add(availableEffects.remove(index));
        }
        
        return effects;
    }
    
    /**
     * Fuse two crystals together
     */
    public static ItemStack fuseCrystals(ItemStack crystal1, ItemStack crystal2) {
        if (!isCrystal(crystal1) || !isCrystal(crystal2)) {
            return null;
        }
        
        List<String> effects1 = getCrystalEffects(crystal1);
        List<String> effects2 = getCrystalEffects(crystal2);
        
        // Combine effects
        List<String> combinedEffects = combineEffects(effects1, effects2);
        
        // Create new crystal with combined effects
        ItemStack result = createBasicCrystal();
        setCrystalEffects(result, combinedEffects);
        
        // Set energy to average of both crystals
        int energy1 = getEnergy(crystal1);
        int energy2 = getEnergy(crystal2);
        setEnergy(result, (energy1 + energy2) / 2);
        
        return result;
    }
    
    /**
     * Socket an effect into a crystal
     */
    public static boolean socketEffect(ItemStack crystal, String effect) {
        if (!isCrystal(crystal)) return false;
        
        CrystalType type = getCrystalType(crystal);
        List<String> currentEffects = getCrystalEffects(crystal);
        
        if (currentEffects.size() >= type.getMaxEffects()) {
            return false;
        }
        
        currentEffects.add(effect);
        setCrystalEffects(crystal, currentEffects);
        return true;
    }

    /**
     * Check if a crystal has been identified
     */
    public static boolean isIdentified(ItemStack crystal) {
        if (!isCrystal(crystal)) return false;
        
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) return false;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(CRYSTAL_EFFECTS_KEY, PersistentDataType.STRING);
    }
    
    /**
     * Set crystal effects
     */
    public static void setCrystalEffects(ItemStack crystal, List<String> effects) {
        if (!isCrystal(crystal)) return;
        
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) return;
        
        // Store effects in NBT
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String effectsJson = new Gson().toJson(effects);
        container.set(CRYSTAL_EFFECTS_KEY, PersistentDataType.STRING, effectsJson);
        
        // Update lore
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        
        // Remove old effects from lore
        lore.removeIf(line -> line.contains("Effect:"));
        
        // Add new effects to lore
        lore.add("");
        lore.add(ChatColor.LIGHT_PURPLE + "Effects:");
        for (String effect : effects) {
            lore.add(ChatColor.GRAY + "• " + beautifyEffectName(effect));
        }
        
        meta.setLore(lore);
        crystal.setItemMeta(meta);
    }

    /**
     * Create a random mystery crystal with random effects
     * This bypasses player level restrictions for loot crates
     */
    public static ItemStack createMysteryCrystal() {
        CrystalType type = CrystalType.getRandomType();
        ItemStack crystal = createBlankCrystal();
        ItemMeta meta = crystal.getItemMeta();
        
        // Set crystal type
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(CRYSTAL_TYPE_KEY, PersistentDataType.STRING, type.name());
        
        // Generate random effects
        List<String> effects = new ArrayList<>();
        int numEffects = random.nextInt(type.getMaxEffects()) + 1;
        
        // Get all possible effects from config
        ConfigurationSection upgradesConfig = ConfigManager.getUpgradesConfig()
            .getConfigurationSection("upgrades");
        if (upgradesConfig != null) {
            List<String> possibleEffects = new ArrayList<>(upgradesConfig.getKeys(false));
            Collections.shuffle(possibleEffects);
            
            // Add random effects
            for (int i = 0; i < numEffects && i < possibleEffects.size(); i++) {
                effects.add(possibleEffects.get(i));
            }
        }
        
        // Set effects
        String effectsJson = new Gson().toJson(effects);
        container.set(CRYSTAL_EFFECTS_KEY, PersistentDataType.STRING, effectsJson);
        
        // Set energy
        int energy = type.getBaseEnergy();
        container.set(CRYSTAL_ENERGY_KEY, PersistentDataType.INTEGER, energy);
        
        // Set as identified
        container.set(CRYSTAL_CREATED_KEY, PersistentDataType.LONG, System.currentTimeMillis());
        
        // Update display
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + type.getDisplayName());
        updateCrystalLore(crystal, meta, effects, energy, type.getBaseEnergy());
        
        crystal.setItemMeta(meta);
        return crystal;
    }

    public static List<String> combineEffects(List<String> effects1, List<String> effects2) {
        Set<String> combined = new HashSet<>(effects1);
        combined.addAll(effects2);
        
        // Limit to max effects based on crystal type
        CrystalType type = CrystalType.getRandomType();
        List<String> result = new ArrayList<>(combined);
        
        // Shuffle and limit to max effects
        Collections.shuffle(result);
        if (result.size() > type.getMaxEffects()) {
            result = result.subList(0, type.getMaxEffects());
        }
        
        return result;
    }

    // Helper to build tiered crystals consistently
    private static ItemStack createTieredCrystal(CrystalCraftingManager.CrystalTier tier) {
        String matName = ConfigManager.getConfig().getString("crystal.material", "DIAMOND");
        Material material = Material.matchMaterial(matName);
        if (material == null) material = Material.DIAMOND;

        ItemStack crystal = new ItemStack(material);
        ItemMeta meta = crystal.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + tier.getDisplayName());

        List<String> lore = new ArrayList<>();
        switch (tier) {
            case APPRENTICE -> lore.add(ChatColor.GRAY + "A novice crystal awaiting discovery.");
            case ADEPT -> lore.add(ChatColor.GRAY + "A refined crystal brimming with power.");
            case MASTERWORK -> lore.add(ChatColor.GRAY + "A masterwork crystal of immense potential.");
        }
        lore.add(ChatColor.YELLOW + "Identify at the workstation");
        meta.setLore(lore);
        crystal.setItemMeta(meta);
        return crystal;
    }
}
