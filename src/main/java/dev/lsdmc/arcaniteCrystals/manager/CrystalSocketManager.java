package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages crystal socketing mechanics.
 */
public class CrystalSocketManager {
    private static final Map<UUID, Map<String, List<String>>> playerSockets = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Integer>> socketCooldowns = new ConcurrentHashMap<>();
    
    private static final NamespacedKey KEY_SOCKETED_CRYSTALS = new NamespacedKey("arcanitecrystals", "socketed_crystals");
    private static final NamespacedKey KEY_SOCKET_COOLDOWN = new NamespacedKey("arcanitecrystals", "socket_cooldown");
    
    private static final int MAX_SOCKETS = 3;
    private static final int SOCKET_COOLDOWN = 3600; // 1 hour in seconds
    
    private static final Logger logger = ArcaniteCrystals.getInstance().getLogger();
    
    private static final NamespacedKey KEY_CRYSTAL_ID = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal_id");
    private static final NamespacedKey KEY_ITEM_ID = new NamespacedKey(ArcaniteCrystals.getInstance(), "item_id");
    
    private static final double SOCKET_SUCCESS_CHANCE = 0.7; // 70% base success rate
    private static final double SOCKET_DESTROY_CHANCE = 0.1; // 10% chance to destroy crystal on failure
    
    /**
     * Attempts to socket a crystal into an item.
     */
    public static boolean socketCrystal(Player player, ItemStack targetItem, ItemStack crystal) {
        if (!isValidSocketTarget(targetItem) || !CrystalManager.isCrystal(crystal)) {
            return false;
        }
        
        String itemId = getItemId(targetItem);
        if (itemId == null) {
            itemId = UUID.randomUUID().toString();
            setItemId(targetItem, itemId);
        }
        
        // Check cooldown
        if (isOnCooldown(player, itemId)) {
            player.sendMessage("§cThis item is still on socket cooldown!");
            return false;
        }
        
        // Get current sockets
        List<String> socketedCrystals = getSocketedCrystals(targetItem);
        if (socketedCrystals.size() >= MAX_SOCKETS) {
            player.sendMessage("§cThis item has reached its maximum socket capacity!");
            return false;
        }
        
        // Add crystal to sockets
        String crystalId = CrystalManager.getCrystalId(crystal);
        if (crystalId == null) {
            crystalId = UUID.randomUUID().toString();
            CrystalManager.setCrystalId(crystal, crystalId);
        }
        
        socketedCrystals.add(crystalId);
        setSocketedCrystals(targetItem, socketedCrystals);
        
        // Set cooldown
        setCooldown(player, itemId);
        
        // Play effects
        ParticleManager.playCrystalSocketEffect(player);
        SoundManager.playCrystalSocketSound(player);
        
        // Update item lore
        updateSocketLore(targetItem);
        
        return true;
    }
    
    /**
     * Removes a socketed crystal from an item.
     */
    public static boolean removeSocketedCrystal(Player player, ItemStack item, int socketIndex) {
        if (!isValidSocketTarget(item)) {
            return false;
        }
        
        String itemId = getItemId(item);
        if (itemId == null) {
            return false;
        }
        
        List<String> socketedCrystals = getSocketedCrystals(item);
        if (socketIndex < 0 || socketIndex >= socketedCrystals.size()) {
            return false;
        }
        
        // Remove crystal
        socketedCrystals.remove(socketIndex);
        setSocketedCrystals(item, socketedCrystals);
        
        // Play effects
        ParticleManager.playCrystalSocketRemoveEffect(player);
        SoundManager.playCrystalSocketRemoveSound(player);
        
        // Update item lore
        updateSocketLore(item);
        
        return true;
    }
    
    /**
     * Gets all socketed crystals in an item.
     */
    public static List<String> getSocketedCrystals(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return new ArrayList<>();
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String data = container.get(KEY_SOCKETED_CRYSTALS, PersistentDataType.STRING);
        
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(Arrays.asList(data.split(",")));
    }
    
    /**
     * Sets the socketed crystals for an item.
     */
    private static void setSocketedCrystals(ItemStack item, List<String> crystalIds) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String data = String.join(",", crystalIds);
        container.set(KEY_SOCKETED_CRYSTALS, PersistentDataType.STRING, data);
        
        item.setItemMeta(meta);
    }
    
    /**
     * Updates the item's lore to show socketed crystals.
     */
    private static void updateSocketLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        
        // Remove old socket lore
        lore.removeIf(line -> line.startsWith("§7Socketed Crystal:"));
        
        // Add new socket lore
        List<String> socketedCrystals = getSocketedCrystals(item);
        for (String crystalId : socketedCrystals) {
            lore.add("§7Socketed Crystal: §b" + crystalId);
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
    
    /**
     * Checks if an item is valid for socketing.
     */
    private static boolean isValidSocketTarget(ItemStack item) {
        if (item == null) return false;
        
        // Add your item type validation here
        return item.getType() == Material.DIAMOND_SWORD ||
               item.getType() == Material.NETHERITE_SWORD ||
               item.getType() == Material.DIAMOND_CHESTPLATE ||
               item.getType() == Material.NETHERITE_CHESTPLATE;
    }
    
    /**
     * Gets the unique ID of an item.
     */
    private static String getItemId(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(KEY_SOCKETED_CRYSTALS, PersistentDataType.STRING);
    }
    
    /**
     * Sets a unique ID for an item.
     */
    private static void setItemId(ItemStack item, String id) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(KEY_SOCKETED_CRYSTALS, PersistentDataType.STRING, id);
        
        item.setItemMeta(meta);
    }
    
    /**
     * Checks if an item is on socket cooldown.
     */
    private static boolean isOnCooldown(Player player, String itemId) {
        Map<String, Integer> cooldowns = socketCooldowns.get(player.getUniqueId());
        if (cooldowns == null) return false;
        
        Integer cooldown = cooldowns.get(itemId);
        if (cooldown == null) return false;
        
        return cooldown > 0;
    }
    
    /**
     * Sets the socket cooldown for an item.
     */
    private static void setCooldown(Player player, String itemId) {
        Map<String, Integer> cooldowns = socketCooldowns.computeIfAbsent(
            player.getUniqueId(), k -> new ConcurrentHashMap<>());
        cooldowns.put(itemId, SOCKET_COOLDOWN);
    }
    
    /**
     * Updates all socket cooldowns.
     */
    public static void updateCooldowns() {
        for (Map<String, Integer> cooldowns : socketCooldowns.values()) {
            cooldowns.replaceAll((k, v) -> Math.max(0, v - 1));
        }
    }
    
    /**
     * Checks if an item can be socketed.
     */
    private static boolean isSocketable(ItemStack item) {
        if (item == null) return false;
        
        Material type = item.getType();
        return type.name().endsWith("_SWORD") ||
               type.name().endsWith("_AXE") ||
               type.name().endsWith("_PICKAXE") ||
               type.name().endsWith("_SHOVEL") ||
               type.name().endsWith("_HOE") ||
               type.name().endsWith("_HELMET") ||
               type.name().endsWith("_CHESTPLATE") ||
               type.name().endsWith("_LEGGINGS") ||
               type.name().endsWith("_BOOTS");
    }
    
    /**
     * Checks if an item is a valid crystal.
     */
    private static boolean isValidCrystal(ItemStack item) {
        if (item == null) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(KEY_CRYSTAL_ID, PersistentDataType.STRING);
    }
    
    /**
     * Calculates the success chance for socketing.
     */
    private static double calculateSuccessChance(Player player, ItemStack item, ItemStack crystal) {
        double chance = SOCKET_SUCCESS_CHANCE;
        
        // Add player level bonus
        int playerLevel = player.getLevel();
        chance += playerLevel * 0.01; // 1% per level
        
        // Add item quality bonus
        if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
            chance += 0.05; // 5% for enchanted items
        }
        
        // Add crystal quality bonus
        if (crystal.hasItemMeta() && crystal.getItemMeta().hasEnchants()) {
            chance += 0.1; // 10% for enchanted crystals
        }
        
        return Math.min(0.95, chance); // Cap at 95%
    }
    
    /**
     * Applies the effects of a socketed crystal.
     */
    private static void applySocketEffects(Player player, ItemStack item, ItemStack crystal) {
        List<String> effects = CrystalManager.getCrystalEffects(crystal);
        for (String effect : effects) {
            UpgradeManager.applyUpgradeEffect(player, effect);
        }
        
        // Store active effects
        Map<String, List<String>> sockets = playerSockets.computeIfAbsent(
            player.getUniqueId(), k -> new ConcurrentHashMap<>());
        String itemId = getItemId(item);
        sockets.put(itemId, effects);
    }
    
    /**
     * Removes the effects of a socketed crystal.
     */
    private static void removeSocketEffects(Player player, ItemStack item, String crystalId) {
        Map<String, List<String>> sockets = playerSockets.get(player.getUniqueId());
        if (sockets == null) return;
        
        String itemId = getItemId(item);
        List<String> effects = sockets.get(itemId);
        if (effects != null) {
            for (String effect : effects) {
                UpgradeManager.removeUpgradeEffect(player, effect);
            }
            sockets.remove(itemId);
        }
    }
    
    /**
     * Gets the unique ID of a crystal.
     */
    private static String getCrystalId(ItemStack crystal) {
        if (!crystal.hasItemMeta()) return null;
        
        PersistentDataContainer container = crystal.getItemMeta().getPersistentDataContainer();
        return container.get(KEY_CRYSTAL_ID, PersistentDataType.STRING);
    }
    
    /**
     * Sets the unique ID of a crystal.
     */
    private static void setCrystalId(ItemStack crystal, String id) {
        ItemMeta meta = crystal.getItemMeta();
        if (meta == null) return;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(KEY_CRYSTAL_ID, PersistentDataType.STRING, id);
        crystal.setItemMeta(meta);
    }
    
    /**
     * Cleans up all socket data.
     */
    public static void cleanup() {
        playerSockets.clear();
        socketCooldowns.clear();
    }
} 