// src/main/java/dev/lsdmc/arcaniteCrystals/manager/CrystalManager.java
package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.util.EffectUtils;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Comprehensive crystal management system handling activation, energy, cooldowns,
 * effects, security, and all crystal-related mechanics.
 */
public class CrystalManager {

    private static final NamespacedKey KEY_CRYSTAL = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal");
    private static final NamespacedKey KEY_ABILITIES = new NamespacedKey(ArcaniteCrystals.getInstance(), "abilities");
    private static final NamespacedKey KEY_ENERGY = new NamespacedKey(ArcaniteCrystals.getInstance(), "energy");

    // NBT Keys for crystal data
    private static final NamespacedKey CRYSTAL_ID_KEY = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal_id");
    private static final NamespacedKey CRYSTAL_ENERGY_KEY = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal_energy");
    private static final NamespacedKey CRYSTAL_EFFECTS_KEY = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal_effects");
    private static final NamespacedKey CRYSTAL_CREATED_KEY = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal_created");
    private static final NamespacedKey CRYSTAL_ACTIVATED_KEY = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal_activated");
    
    // Active crystal tracking
    private static final Map<UUID, ItemStack> activeCrystals = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastEffectTick = new ConcurrentHashMap<>();
    
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
        loadConfiguration();
        startEnergyDrainTask();
        startAuraEffectTask();
        ArcaniteCrystals.getInstance().getLogger().info("CrystalManager initialized successfully");
    }
    
    /**
     * Loads configuration values into cache.
     */
    private static void loadConfiguration() {
        var config = ConfigManager.getConfig();
        maxEnergy = config.getInt("crystal.energy", 18000);
        energyDrain = config.getInt("crystal.drain", 80);
        cooldownMs = config.getInt("crystal.cooldown", 300) * 1000L;
        
        String matName = config.getString("crystal.material", "DIAMOND");
        crystalMaterial = Material.matchMaterial(matName);
        if (crystalMaterial == null) crystalMaterial = Material.DIAMOND;
        
        String rechargeName = config.getString("recharge.material", "QUARTZ");
        rechargeMaterial = Material.matchMaterial(rechargeName);
        if (rechargeMaterial == null) rechargeMaterial = Material.QUARTZ;
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
     * Checks if a crystal is activated and has energy.
     */
    public static boolean isActivatedCrystal(ItemStack item) {
        if (!isCrystal(item)) return false;
        try {
            Integer energy = item.getItemMeta().getPersistentDataContainer().get(KEY_ENERGY, PersistentDataType.INTEGER);
            return energy != null && energy > 0;
        } catch (Exception e) {
            return false;
        }
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
            
            List<String> lore = Arrays.asList(
                ChatColor.GRAY + "(Unidentified Crystal)",
                ChatColor.DARK_GRAY + "Right-click to activate",
                ChatColor.DARK_PURPLE + "✨ Mystical Energy Contained ✨"
            );
            
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
     * Activates a crystal with comprehensive validation.
     */
    public static void activateCrystal(Player player) {
        try {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (!isCrystal(item)) {
                player.sendMessage(ChatColor.RED + "You must hold an Arcanite Crystal!");
                return;
            }

            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();
            long last = PlayerDataManager.getCooldown(uuid);
            long cooldownMs = ConfigManager.getConfig().getLong("crystal.cooldown", 300) * 1000;

            // Check cooldown
            if (now < last + cooldownMs) {
                long secsLeft = (last + cooldownMs - now) / 1000;
                player.sendMessage(ChatColor.YELLOW + "⏰ Crystal Cooldown: " + 
                                 ChatColor.RED + secsLeft + "s " + ChatColor.YELLOW + "remaining");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
                return;
            }

            // Get player's available upgrades
            int tier = LevelManager.getMaxTier(uuid);
            int slots = LevelManager.getSlots(uuid);
            Set<String> allUpgrades = PlayerDataManager.getUnlockedUpgrades(uuid);
            
            // Filter by tier and convert to list
            List<String> availableUpgrades = allUpgrades.stream()
                    .filter(id -> LevelManager.getTier(id) <= tier)
                    .collect(Collectors.toList());

            if (availableUpgrades.isEmpty()) {
                player.sendMessage(ChatColor.RED + "You have no unlocked upgrades! Visit /arcanite talents to unlock some.");
                return;
            }

            // Select random abilities
            Collections.shuffle(availableUpgrades);
            List<String> chosenAbilities = availableUpgrades.stream()
                    .limit(Math.min(slots, availableUpgrades.size()))
                    .collect(Collectors.toList());

            // Update crystal
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                player.sendMessage(ChatColor.RED + "Crystal error - invalid metadata!");
                return;
            }

            // Set abilities and energy
            meta.getPersistentDataContainer().set(KEY_ABILITIES, PersistentDataType.STRING, 
                    String.join(",", chosenAbilities));
            int initialEnergy = ConfigManager.getConfig().getInt("crystal.energy", 18000);
            meta.getPersistentDataContainer().set(KEY_ENERGY, PersistentDataType.INTEGER, initialEnergy);
            
            // Update lore
            updateCrystalLore(item, meta, chosenAbilities, initialEnergy, initialEnergy);
            
            item.setItemMeta(meta);

            // Set cooldown
            PlayerDataManager.setCooldown(uuid, now);

            // Success feedback
            SoundManager.playCrystalActivateSound(player);
            ParticleManager.playCrystalActivationEffect(player);
            MessageManager.sendCrystalActivated(player, chosenAbilities, initialEnergy);
            
            // Title notification
            MessageManager.showTitle(player, 
                "⚡ CRYSTAL ACTIVATED ⚡",
                "Hold in off-hand to use");
                
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Error activating crystal: " + e.getMessage());
            ArcaniteCrystals.getInstance().getLogger().warning("Error activating crystal for " + player.getName() + ": " + e.getMessage());
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
     * Gets the abilities from a crystal.
     */
    public static List<String> getAbilities(ItemStack item) {
        if (!isActivatedCrystal(item)) return Collections.emptyList();
        try {
            String data = item.getItemMeta().getPersistentDataContainer().get(KEY_ABILITIES, PersistentDataType.STRING);
            return (data == null || data.isBlank()) 
                    ? Collections.emptyList() 
                    : Arrays.asList(data.split(","));
        } catch (Exception e) {
            return Collections.emptyList();
        }
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
    private static void updateCrystalLore(ItemStack crystal, ItemMeta meta, List<String> abilities, int currentEnergy, int maxEnergy) {
        try {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.AQUA + "Active Crystal");
            lore.add(ChatColor.GRAY + "Hold in off-hand to activate");
            lore.add("");
            lore.add(ChatColor.GOLD + "✨ Enchanted Effects:");
            
            for (String ability : abilities) {
                String displayName = beautifyAbilityName(ability);
                lore.add(ChatColor.LIGHT_PURPLE + "  ► " + displayName);
            }
            
            lore.add("");
            lore.add(createEnergyDisplay(currentEnergy, maxEnergy));
            lore.add(ChatColor.DARK_GRAY + "Energy depletes over time");
            
            meta.setLore(lore);
        } catch (Exception e) {
            ArcaniteCrystals.getInstance().getLogger().warning("Error updating crystal lore: " + e.getMessage());
        }
    }
    
    /**
     * Creates a visual energy display.
     */
    private static String createEnergyDisplay(int current, int max) {
        try {
            int barLength = 15;
            int filled = Math.max(0, Math.min(barLength, (int) ((double) current / max * barLength)));
            
            StringBuilder bar = new StringBuilder();
            bar.append(ChatColor.AQUA).append("Energy: ");
            
            // Color based on energy level
            ChatColor energyColor = current > max * 0.6 ? ChatColor.GREEN :
                                   current > max * 0.3 ? ChatColor.YELLOW : ChatColor.RED;
            
            bar.append(energyColor).append("[");
            
            for (int i = 0; i < barLength; i++) {
                if (i < filled) {
                    bar.append("█");
                } else {
                    bar.append(ChatColor.DARK_GRAY).append("░").append(energyColor);
                }
            }
            
            bar.append(ChatColor.AQUA).append("] ").append(current).append("/").append(max);
            return bar.toString();
        } catch (Exception e) {
            return ChatColor.AQUA + "Energy: " + current + "/" + max;
        }
    }
    
    /**
     * Beautifies ability names for display.
     */
    private static String beautifyAbilityName(String abilityId) {
        try {
            // Extract effect and tier from ID
            String[] parts = abilityId.split("_");
            if (parts.length < 2) return abilityId;
            
            String effect = parts[0];
            String tier = parts[1];
            
            // Beautify effect names
            String beautifulName = switch (effect.toLowerCase()) {
                case "speed" -> "Speed";
                case "regeneration" -> "Regeneration";
                case "jump" -> "Jump Boost";
                case "haste" -> "Haste";
                case "strength" -> "Strength";
                case "echo" -> "Resistance";
                case "poison" -> "Poison";
                default -> effect.substring(0, 1).toUpperCase() + effect.substring(1).toLowerCase();
            };
            
            return beautifulName + " " + tier;
        } catch (Exception e) {
            return abilityId;
        }
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
        ItemStack crystal = createBlankCrystal();
        
        // Determine available effects based on player level
        int playerLevel = PlayerDataManager.getLevel(player.getUniqueId());
        int maxTier = LevelManager.getMaxTier(player.getUniqueId());
        int slots = LevelManager.getSlots(player.getUniqueId());
        Set<String> unlockedUpgrades = PlayerDataManager.getUnlockedUpgrades(player.getUniqueId());
        
        // Select random effects from unlocked upgrades
        List<String> availableEffects = new ArrayList<>(unlockedUpgrades);
        Collections.shuffle(availableEffects);
        
        List<String> selectedEffects = new ArrayList<>();
        int effectCount = Math.min(slots, Math.min(availableEffects.size(), 3)); // Max 3 effects
        
        for (int i = 0; i < effectCount; i++) {
            selectedEffects.add(availableEffects.get(i));
        }
        
        return createCrystalWithEffects(selectedEffects);
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
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String effectsStr = container.get(CRYSTAL_EFFECTS_KEY, PersistentDataType.STRING);
        
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
     * Activates a crystal for a player.
     */
    public static boolean activateCrystal(Player player, ItemStack crystal) {
        if (!isCrystal(crystal)) return false;
        
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
        
        // Activate crystal
        activeCrystals.put(player.getUniqueId(), crystal.clone());
        
        // Apply effects
        applyEffects(player, effects);
        
        // Mark as activated
        ItemMeta meta = crystal.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(CRYSTAL_ACTIVATED_KEY, PersistentDataType.BYTE, (byte) 1);
        crystal.setItemMeta(meta);
        
        // Feedback
        String effectNames = effects.stream()
                .map(CrystalManager::beautifyEffectName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("Unknown");
        
        MessageManager.sendCrystalActivated(player, effects, energy);
        SoundManager.playCrystalActivateSound(player);
        ParticleManager.playCrystalActivationEffect(player);
        
        return true;
    }
    
    /**
     * Deactivates a crystal for a player.
     */
    public static void deactivateCrystal(Player player) {
        ItemStack activeCrystal = activeCrystals.remove(player.getUniqueId());
        if (activeCrystal == null) return;
        
        // Remove effects
        removeEffects(player, getCrystalEffects(activeCrystal));
        
        // Set cooldown
        PlayerDataManager.setCooldown(player.getUniqueId(), System.currentTimeMillis() + cooldownMs);
        
        player.sendMessage(ChatColor.GRAY + "Crystal effects have ended.");
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
     * Recharges a crystal using quartz.
     */
    public static boolean rechargeCrystal(Player player, ItemStack crystal, ItemStack quartz) {
        if (!isCrystal(crystal)) return false;
        if (quartz.getType() != rechargeMaterial) return false;
        
        List<String> effects = getCrystalEffects(crystal);
        if (effects.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Cannot recharge an unidentified crystal!");
            return false;
        }
        
        int currentEnergy = getEnergy(crystal);
        if (currentEnergy >= maxEnergy) {
            player.sendMessage(ChatColor.YELLOW + "This crystal is already fully charged!");
            return false;
        }
        
        // Consume quartz and recharge
        quartz.setAmount(quartz.getAmount() - 1);
        setEnergy(crystal, maxEnergy);
        
        // Feedback
        MessageManager.sendNotification(player, "Crystal recharged successfully!", MessageManager.NotificationType.SUCCESS);
        SoundManager.playCrystalRechargeSound(player);
        ParticleManager.playCrystalRechargeEffect(player);
        
        return true;
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
        deactivateCrystal(player);
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
                        deactivateCrystal(player);
                        
                        player.sendMessage(ChatColor.RED + "Your crystal has been depleted!");
                        SoundManager.playCrystalDepletionSound(player);
                        ParticleManager.playCrystalDepletionEffect(player);
                        continue;
                    }
                    
                    // Drain energy
                    setEnergy(crystal, currentEnergy - energyDrain);
                    
                    // Update the crystal in player's inventory
                    ItemStack offhand = player.getInventory().getItemInOffHand();
                    if (isCrystal(offhand)) {
                        setEnergy(offhand, currentEnergy - energyDrain);
                    }
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
     * Beautifies effect names for display.
     */
    private static String beautifyEffectName(String effectId) {
        if (effectId.contains("_")) {
            String[] parts = effectId.split("_");
            String baseName = parts[0];
            String tier = parts.length > 1 ? parts[1] : "";
            
            String beautiful = switch (baseName.toLowerCase()) {
                case "speed" -> "Swift Movement";
                case "regeneration" -> "Life Regeneration";
                case "jump" -> "Enhanced Jumping";
                case "haste" -> "Mining Acceleration";
                case "strength" -> "Combat Prowess";
                case "echo" -> "Damage Resistance";
                case "poison" -> "Toxic Immunity";
                default -> baseName.substring(0, 1).toUpperCase() + baseName.substring(1);
            };
            
            return beautiful + (tier.isEmpty() ? "" : " " + tier);
        }
        
        return effectId;
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
     * Shuts down the crystal manager.
     */
    public static void shutdown() {
        // Deactivate all crystals
        for (UUID playerId : new HashSet<>(activeCrystals.keySet())) {
            Player player = ArcaniteCrystals.getInstance().getServer().getPlayer(playerId);
            if (player != null) {
                deactivateCrystal(player);
            }
        }
        
        activeCrystals.clear();
        lastEffectTick.clear();
    }
}
