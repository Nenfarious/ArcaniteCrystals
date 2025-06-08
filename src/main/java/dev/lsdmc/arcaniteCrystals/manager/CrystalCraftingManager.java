package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.util.EffectUtils;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.RecipeChoice;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Professional crystal crafting system with sophisticated tiered recipes,
 * dynamic success rates, comprehensive validation, and immersive crafting experience.
 */
public class CrystalCraftingManager implements Listener {

    private static final NamespacedKey KEY_CRYSTAL = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal");
    private static final NamespacedKey KEY_TIER = new NamespacedKey(ArcaniteCrystals.getInstance(), "tier");
    private static final NamespacedKey KEY_PREVIEW = new NamespacedKey(ArcaniteCrystals.getInstance(), "preview");
    private static final NamespacedKey KEY_POWER_LEVEL = new NamespacedKey(ArcaniteCrystals.getInstance(), "power_level");
    
    // Enhanced crystal tier definitions with sophisticated recipes
    private static final Map<CrystalTier, CrystalRecipe> CRYSTAL_RECIPES;
    
    // Crafting experience rewards
    private static final Map<CrystalTier, Integer> EXPERIENCE_REWARDS = Map.of(
        CrystalTier.APPRENTICE, 50,
        CrystalTier.ADEPT, 150,
        CrystalTier.MASTERWORK, 500
    );

    static {
        Map<CrystalTier, CrystalRecipe> recipeMap = new HashMap<>();
        
        // Tier 1: Basic Crystal - Early game accessible
        recipeMap.put(CrystalTier.APPRENTICE, new CrystalRecipe(
            CrystalTier.APPRENTICE,
            createBasicRecipeMaterials(),
            "✦ Basic Arcanite Crystal ✦",
            createBasicCrystalLore(),
            0.85, // 85% success rate
            1, // Level requirement
            Arrays.asList("speed_I", "regeneration_I", "jump_I") // Possible effects
        ));

        // Tier 2: Rare Crystal - Mid-game challenge
        recipeMap.put(CrystalTier.ADEPT, new CrystalRecipe(
            CrystalTier.ADEPT,
            createRareRecipeMaterials(),
            "◆ Rare Arcanite Crystal ◆",
            createRareCrystalLore(),
            0.65, // 65% success rate
            5, // Level requirement
            Arrays.asList("speed_II", "regeneration_II", "haste_II", "strength_II") // Possible effects
        ));

        // Tier 3: Legendary Crystal - End-game ultimate
        recipeMap.put(CrystalTier.MASTERWORK, new CrystalRecipe(
            CrystalTier.MASTERWORK,
            createLegendaryRecipeMaterials(),
            "❖ Legendary Arcanite Crystal ❖",
            createLegendaryCrystalLore(),
            0.45, // 45% success rate
            8, // Level requirement
            Arrays.asList("speed_III", "regeneration_III", "haste_III", "strength_III", "echo_III") // Possible effects
        ));
        
        CRYSTAL_RECIPES = Collections.unmodifiableMap(recipeMap);
    }

    private static final Map<String, ShapedRecipe> shapedRecipes = new HashMap<>();
    private static final Map<String, ShapelessRecipe> shapelessRecipes = new HashMap<>();
    
    /**
     * Registers all crystal crafting recipes.
     */
    public static void registerRecipes() {
        registerBlankCrystalRecipe();
        registerCrystalUpgradeRecipes();
        registerCrystalCombinationRecipes();
    }
    
    /**
     * Registers the recipe for crafting a blank crystal.
     */
    private static void registerBlankCrystalRecipe() {
        NamespacedKey key = new NamespacedKey(ArcaniteCrystals.getInstance(), "blank_crystal");
        ShapedRecipe recipe = new ShapedRecipe(key, CrystalManager.createBlankCrystal());
        
        recipe.shape("DND", "NEN", "DND");
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('N', Material.NETHERITE_INGOT);
        recipe.setIngredient('E', Material.END_CRYSTAL);
        
        ArcaniteCrystals.getInstance().getServer().addRecipe(recipe);
        shapedRecipes.put("blank_crystal", recipe);
    }
    
    /**
     * Registers recipes for upgrading crystals.
     */
    private static void registerCrystalUpgradeRecipes() {
        // Tier 1 to Tier 2
        registerCrystalUpgradeRecipe("tier1_to_tier2", 1, 2, 
            Material.NETHERITE_INGOT, Material.DIAMOND_BLOCK);
            
        // Tier 2 to Tier 3
        registerCrystalUpgradeRecipe("tier2_to_tier3", 2, 3,
            Material.NETHERITE_BLOCK, Material.END_CRYSTAL);
            
        // Tier 3 to Tier 4
        registerCrystalUpgradeRecipe("tier3_to_tier4", 3, 4,
            Material.NETHERITE_BLOCK, Material.DRAGON_EGG);
    }
    
    /**
     * Registers a specific crystal upgrade recipe.
     */
    private static void registerCrystalUpgradeRecipe(String name, int fromTier, int toTier, 
            Material... materials) {
        NamespacedKey key = new NamespacedKey(ArcaniteCrystals.getInstance(), name);
        ShapelessRecipe recipe = new ShapelessRecipe(key, 
            CrystalManager.createBlankCrystal());
            
        // Add the base crystal
        recipe.addIngredient(new RecipeChoice.ExactChoice(
            CrystalManager.createBlankCrystal()));
            
        // Add upgrade materials
        for (Material material : materials) {
            recipe.addIngredient(material);
        }
        
        ArcaniteCrystals.getInstance().getServer().addRecipe(recipe);
        shapelessRecipes.put(name, recipe);
    }
    
    /**
     * Registers recipes for combining crystals.
     */
    private static void registerCrystalCombinationRecipes() {
        // Combine two Tier 1 crystals
        registerCrystalCombinationRecipe("combine_tier1", 1, 1, 2);
        
        // Combine two Tier 2 crystals
        registerCrystalCombinationRecipe("combine_tier2", 2, 2, 3);
        
        // Combine two Tier 3 crystals
        registerCrystalCombinationRecipe("combine_tier3", 3, 3, 4);
    }
    
    /**
     * Registers a specific crystal combination recipe.
     */
    private static void registerCrystalCombinationRecipe(String name, int tier, int count, int resultTier) {
        NamespacedKey key = new NamespacedKey(ArcaniteCrystals.getInstance(), name);
        ShapelessRecipe recipe = new ShapelessRecipe(key, 
            CrystalManager.createBlankCrystal());
            
        // Add the crystals to combine
        ItemStack crystal = CrystalManager.createBlankCrystal();
        for (int i = 0; i < count; i++) {
            recipe.addIngredient(new RecipeChoice.ExactChoice(crystal));
        }
        
        ArcaniteCrystals.getInstance().getServer().addRecipe(recipe);
        shapelessRecipes.put(name, recipe);
    }
    
    /**
     * Unregisters all crystal crafting recipes.
     */
    public static void unregisterRecipes() {
        for (ShapedRecipe recipe : shapedRecipes.values()) {
            ArcaniteCrystals.getInstance().getServer().removeRecipe(recipe.getKey());
        }
        for (ShapelessRecipe recipe : shapelessRecipes.values()) {
            ArcaniteCrystals.getInstance().getServer().removeRecipe(recipe.getKey());
        }
        shapedRecipes.clear();
        shapelessRecipes.clear();
    }
    
    /**
     * Checks if a player has the required materials for a recipe.
     */
    public static boolean hasRequiredMaterials(Player player, ItemStack[] recipe) {
        Map<Material, Integer> required = new HashMap<>();
        Map<Material, Integer> available = new HashMap<>();
        
        // Count required materials
        for (ItemStack item : recipe) {
            if (item != null) {
                required.merge(item.getType(), 1, Integer::sum);
            }
        }
        
        // Count available materials
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                available.merge(item.getType(), item.getAmount(), Integer::sum);
            }
        }
        
        // Check if player has all required materials
        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            if (available.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Handles crafting preparation with sophisticated preview and validation.
     */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getView().getPlayer();
        ItemStack[] matrix = event.getInventory().getMatrix();
        
        // Check if this matches any crystal recipe
        CrystalRecipe matchedRecipe = findMatchingRecipe(matrix, player);
        if (matchedRecipe != null) {
            // Validate player requirements
            if (!validatePlayerRequirements(player, matchedRecipe)) {
                // Show requirement failure preview
                ItemStack errorPreview = createRequirementErrorPreview(matchedRecipe);
                event.getInventory().setResult(errorPreview);
            } else {
                // Show preview crystal with dynamic success chance
                ItemStack previewCrystal = createDynamicPreview(matchedRecipe, player);
                event.getInventory().setResult(previewCrystal);
            }
        }
    }

    /**
     * Handles the actual crafting event with sophisticated success mechanics.
     */
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getInventory().getResult();
        
        if (result == null || !isCrystalResult(result)) return;
        
        // Get the recipe that was used
        CrystalRecipe recipe = getRecipeFromResult(result);
        if (recipe == null) return;
        
        // Check if this is just a preview
        if (isPreviewItem(result)) {
            event.setCancelled(true);
            return;
        }
        
        // Final validation
        if (!validatePlayerRequirements(player, recipe)) {
            event.setCancelled(true);
            player.sendMessage(MessageManager.ERROR + "You no longer meet the requirements for this crystal!");
            return;
        }
        
        // Calculate dynamic success rate based on player skill
        double successRate = calculateDynamicSuccessRate(recipe, player);
        boolean success = ThreadLocalRandom.current().nextDouble() < successRate;
        
        if (success) {
            // Successful craft - create actual crystal with effects
            processCraftingSuccess(event, player, recipe);
        } else {
            // Failed craft - handle gracefully
            processCraftingFailure(event, player, recipe);
        }
    }

    /**
     * Processes successful crafting with rewards and effects.
     */
    private void processCraftingSuccess(CraftItemEvent event, Player player, CrystalRecipe recipe) {
        // Create actual functional crystal
        ItemStack actualCrystal = createFunctionalCrystal(recipe, player);
        event.getInventory().setResult(actualCrystal);
        
        // Award experience
        int expReward = EXPERIENCE_REWARDS.getOrDefault(recipe.tier, 50);
        player.giveExp(expReward);
        
        // Professional success feedback with dramatic timing
        new BukkitRunnable() {
            @Override
            public void run() {
                String message = MessageManager.SUCCESS + "✨ " + ChatColor.BOLD + "CRYSTAL FORGED! ✨\n" +
                               MessageManager.ACCENT + "► " + ChatColor.stripColor(recipe.displayName) + "\n" +
                               MessageManager.MUTED + "The mystical energies have coalesced perfectly!\n" +
                               MessageManager.HIGHLIGHT + "Gained " + expReward + " experience!";
                
                MessageManager.sendNotification(player, message, MessageManager.NotificationType.SUCCESS);
                
                // Epic crafting effects
                SoundManager.playUpgradeUnlockSound(player);
                ParticleManager.playUpgradeEffect(player);
                
                // Show dramatic title
                MessageManager.showTitle(player, "✨ CRYSTAL FORGED ✨", 
                                       ChatColor.stripColor(recipe.displayName));
            }
        }.runTaskLater(ArcaniteCrystals.getInstance(), 5L);
    }
    
    /**
     * Processes crafting failure with partial material recovery.
     */
    private void processCraftingFailure(CraftItemEvent event, Player player, CrystalRecipe recipe) {
        event.setCancelled(true);
        
        // Return partial materials based on tier
        double returnRate = switch (recipe.tier) {
            case APPRENTICE -> 0.7; // 70% return rate
            case ADEPT -> 0.6; // 60% return rate  
            case MASTERWORK -> 0.5; // 50% return rate
        };
        
        returnPartialMaterials(player, recipe, returnRate);
        
        // Professional failure feedback
        String message = MessageManager.ERROR + "⚠ " + ChatColor.BOLD + "FORGING FAILED ⚠\n" +
                       MessageManager.WARNING + "► The mystical energies were unstable\n" +
                       MessageManager.MUTED + String.format("%.0f%% of materials recovered", returnRate * 100) + 
                       "\n" + MessageManager.ACCENT + "💡 Higher level increases success chance!";
        
        MessageManager.sendNotification(player, message, MessageManager.NotificationType.WARNING);
        
        // Failure effects with delay for drama
        new BukkitRunnable() {
            @Override
            public void run() {
                SoundManager.playErrorSound(player);
                ParticleManager.playCrystalDepletionEffect(player);
            }
        }.runTaskLater(ArcaniteCrystals.getInstance(), 3L);
    }

    /**
     * Finds a matching crystal recipe with player validation.
     */
    private CrystalRecipe findMatchingRecipe(ItemStack[] matrix, Player player) {
        Set<Material> providedMaterials = new HashSet<>();
        
        for (ItemStack item : matrix) {
            if (item != null && item.getType() != Material.AIR) {
                providedMaterials.add(item.getType());
            }
        }
        
        // Check each recipe for a match (prioritize higher tiers first)
        List<CrystalTier> tierOrder = Arrays.asList(CrystalTier.MASTERWORK, CrystalTier.ADEPT, CrystalTier.APPRENTICE);
        
        for (CrystalTier tier : tierOrder) {
            CrystalRecipe recipe = CRYSTAL_RECIPES.get(tier);
            if (recipe != null && 
                providedMaterials.containsAll(recipe.requiredMaterials) && 
                providedMaterials.size() == recipe.requiredMaterials.size()) {
                return recipe;
            }
        }
        
        return null;
    }

    /**
     * Validates if player meets recipe requirements.
     */
    private boolean validatePlayerRequirements(Player player, CrystalRecipe recipe) {
        int playerLevel = PlayerDataManager.getLevel(player.getUniqueId());
        return playerLevel >= recipe.levelRequirement;
    }

    /**
     * Calculates dynamic success rate based on player level and tier.
     */
    private double calculateDynamicSuccessRate(CrystalRecipe recipe, Player player) {
        int playerLevel = PlayerDataManager.getLevel(player.getUniqueId());
        double baseRate = recipe.successRate;
        
        // Bonus based on level above requirement
        int levelBonus = Math.max(0, playerLevel - recipe.levelRequirement);
        double bonusRate = levelBonus * 0.02; // 2% per level above requirement
        
        // Cap the bonus at 15%
        bonusRate = Math.min(bonusRate, 0.15);
        
        return Math.min(0.95, baseRate + bonusRate); // Cap at 95% success
    }

    /**
     * Creates a dynamic preview with real-time success calculation.
     */
    private ItemStack createDynamicPreview(CrystalRecipe recipe, Player player) {
        ItemStack crystal = new ItemStack(getCrystalMaterial(recipe.tier));
        ItemMeta meta = crystal.getItemMeta();
        
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', recipe.displayName));
        
        List<String> lore = new ArrayList<>(recipe.baseLore);
        lore.add("");
        
        // Dynamic success rate display
        double successRate = calculateDynamicSuccessRate(recipe, player);
        ChatColor rateColor = successRate >= 0.8 ? ChatColor.GREEN :
                             successRate >= 0.6 ? ChatColor.YELLOW : ChatColor.RED;
        
        lore.add(rateColor + "⚡ Success Rate: " + ChatColor.WHITE + 
                 String.format("%.0f%%", successRate * 100));
        
        // Level requirement
        int playerLevel = PlayerDataManager.getLevel(player.getUniqueId());
        boolean meetsLevel = playerLevel >= recipe.levelRequirement;
        ChatColor levelColor = meetsLevel ? ChatColor.GREEN : ChatColor.RED;
        
        lore.add(levelColor + "📊 Level Required: " + ChatColor.WHITE + recipe.levelRequirement);
        if (!meetsLevel) {
            lore.add(ChatColor.RED + "❌ You need level " + recipe.levelRequirement + "!");
        }
        
        lore.add("");
        lore.add(ChatColor.GRAY + "Possible Effects:");
        for (String effect : recipe.possibleEffects) {
            lore.add(ChatColor.LIGHT_PURPLE + "  ► " + beautifyEffectName(effect));
        }
        
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Crafting may fail and return materials");
        
        meta.setLore(lore);
        
        // Mark as preview
        meta.getPersistentDataContainer().set(KEY_CRYSTAL, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(KEY_TIER, PersistentDataType.INTEGER, recipe.tier.ordinal());
        meta.getPersistentDataContainer().set(KEY_PREVIEW, PersistentDataType.BYTE, (byte) 1);
        
        crystal.setItemMeta(meta);
        return crystal;
    }

    /**
     * Creates error preview when requirements aren't met.
     */
    private ItemStack createRequirementErrorPreview(CrystalRecipe recipe) {
        ItemStack crystal = new ItemStack(Material.BARRIER);
        ItemMeta meta = crystal.getItemMeta();
        
        meta.setDisplayName(ChatColor.RED + "❌ Requirements Not Met");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "To craft " + ChatColor.stripColor(recipe.displayName));
        lore.add(ChatColor.RED + "Level Required: " + recipe.levelRequirement);
        lore.add("");
        lore.add(ChatColor.YELLOW + "Level up to unlock this recipe!");
        
        meta.setLore(lore);
        crystal.setItemMeta(meta);
        return crystal;
    }

    /**
     * Creates the actual functional crystal after successful crafting.
     */
    private ItemStack createFunctionalCrystal(CrystalRecipe recipe, Player player) {
        ItemStack crystal = new ItemStack(getCrystalMaterial(recipe.tier));
        ItemMeta meta = crystal.getItemMeta();
        
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', recipe.displayName));
        
        // Select random effects based on tier
        List<String> selectedEffects = selectRandomEffects(recipe, player);
        
        // Create enhanced lore with effects
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "(Crafted Crystal)");
        lore.add(ChatColor.BLUE + "⚡ Contains mystical energy");
        lore.add("");
        lore.add(ChatColor.GOLD + "✨ Enchanted Effects:");
        
        for (String effect : selectedEffects) {
            lore.add(ChatColor.LIGHT_PURPLE + "  ► " + beautifyEffectName(effect));
        }
        
        lore.add("");
        lore.add(ChatColor.YELLOW + "Right-click to activate");
        lore.add(ChatColor.DARK_PURPLE + "✨ " + recipe.tier.name() + " tier crystal ✨");
        
        meta.setLore(lore);
        
        // Set persistent data
        meta.getPersistentDataContainer().set(KEY_CRYSTAL, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(KEY_TIER, PersistentDataType.INTEGER, recipe.tier.ordinal());
        
        // Store effects for the crystal
        String effectsData = String.join(",", selectedEffects);
        meta.getPersistentDataContainer().set(
            new NamespacedKey(ArcaniteCrystals.getInstance(), "effects"), 
            PersistentDataType.STRING, 
            effectsData
        );
        
        // Set initial energy
        int energy = ArcaniteCrystals.getInstance().getConfig().getInt("crystal.energy", 18000);
        meta.getPersistentDataContainer().set(
            new NamespacedKey(ArcaniteCrystals.getInstance(), "energy"), 
            PersistentDataType.INTEGER, 
            energy
        );
        
        crystal.setItemMeta(meta);
        return crystal;
    }

    /**
     * Selects random effects for the crystal based on tier and player unlocks.
     */
    private List<String> selectRandomEffects(CrystalRecipe recipe, Player player) {
        Set<String> unlockedUpgrades = PlayerDataManager.getUnlockedUpgrades(player.getUniqueId());
        List<String> availableEffects = new ArrayList<>();
        
        // Filter recipe effects by player unlocks
        for (String effect : recipe.possibleEffects) {
            if (unlockedUpgrades.contains(effect)) {
                availableEffects.add(effect);
            }
        }
        
        // If no unlocked effects, fall back to basic tier I effects
        if (availableEffects.isEmpty()) {
            availableEffects.addAll(Arrays.asList("speed_I", "regeneration_I", "jump_I"));
        }
        
        // Select random effects based on tier
        int effectCount = switch (recipe.tier) {
            case APPRENTICE -> 1 + ThreadLocalRandom.current().nextInt(2); // 1-2 effects
            case ADEPT -> 2 + ThreadLocalRandom.current().nextInt(2); // 2-3 effects
            case MASTERWORK -> 3 + ThreadLocalRandom.current().nextInt(2); // 3-4 effects
        };
        
        Collections.shuffle(availableEffects);
        return availableEffects.stream()
                .limit(Math.min(effectCount, availableEffects.size()))
                .toList();
    }

    /**
     * Gets appropriate material for crystal tier.
     */
    private Material getCrystalMaterial(CrystalTier tier) {
        return switch (tier) {
            case APPRENTICE -> Material.DIAMOND;
            case ADEPT -> Material.EMERALD;
            case MASTERWORK -> Material.NETHERITE_INGOT;
        };
    }

    /**
     * Returns partial materials when crafting fails.
     */
    private void returnPartialMaterials(Player player, CrystalRecipe recipe, double returnRate) {
        List<Material> materialsToReturn = new ArrayList<>();
        for (Material material : recipe.requiredMaterials) {
            if (ThreadLocalRandom.current().nextDouble() < returnRate) {
                materialsToReturn.add(material);
            }
        }
        
        // Give back the materials
        for (Material material : materialsToReturn) {
            ItemStack returnItem = new ItemStack(material, 1);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(returnItem);
            
            // Drop items that don't fit
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
        
        player.sendMessage(MessageManager.ACCENT + "Recovered " + materialsToReturn.size() + 
                          " out of " + recipe.requiredMaterials.size() + " materials.");
    }

    /**
     * Checks if an item is a crystal crafting result.
     */
    private boolean isCrystalResult(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY_CRYSTAL, PersistentDataType.BYTE);
    }
    
    /**
     * Checks if an item is a preview item.
     */
    private boolean isPreviewItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY_PREVIEW, PersistentDataType.BYTE);
    }

    /**
     * Gets the recipe used to create a result item.
     */
    private CrystalRecipe getRecipeFromResult(ItemStack result) {
        if (!result.hasItemMeta()) return null;
        
        Integer tierOrdinal = result.getItemMeta().getPersistentDataContainer()
                .get(KEY_TIER, PersistentDataType.INTEGER);
        
        if (tierOrdinal == null) return null;
        
        try {
            CrystalTier tier = CrystalTier.values()[tierOrdinal];
            return CRYSTAL_RECIPES.get(tier);
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Beautifies effect names for display.
     */
    private String beautifyEffectName(String effectId) {
        if (effectId.contains("_")) {
            String[] parts = effectId.split("_");
            String baseName = parts[0];
            String tier = parts.length > 1 ? parts[1] : "";
            
            String beautiful = switch (baseName.toLowerCase()) {
                case "speed" -> "Swift Movement";
                case "regeneration" -> "Life Restoration";
                case "jump" -> "Leap Enhancement";
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
     * Gets comprehensive crafting guide for players.
     */
    public static void showCraftingGuide(Player player) {
        player.sendMessage(MessageManager.PRIMARY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("    " + MessageManager.SUCCESS + ChatColor.BOLD + "⚗️ Crystal Crafting Guide ⚗️");
        player.sendMessage("");
        
        int playerLevel = PlayerDataManager.getLevel(player.getUniqueId());
        
        for (CrystalTier tier : CrystalTier.values()) {
            CrystalRecipe recipe = CRYSTAL_RECIPES.get(tier);
            if (recipe != null) {
                boolean canCraft = playerLevel >= recipe.levelRequirement;
                ChatColor titleColor = canCraft ? ChatColor.AQUA : ChatColor.DARK_GRAY;
                
                player.sendMessage(titleColor + "▣ " + ChatColor.stripColor(recipe.displayName));
                player.sendMessage(MessageManager.MUTED + "  Level Required: " + 
                                 (canCraft ? MessageManager.HIGHLIGHT : ChatColor.RED) + recipe.levelRequirement);
                player.sendMessage(MessageManager.MUTED + "  Base Success Rate: " + 
                                 MessageManager.PRIMARY + String.format("%.0f%%", recipe.successRate * 100));
                player.sendMessage(MessageManager.MUTED + "  Materials: " + 
                                 MessageManager.PRIMARY + recipe.requiredMaterials.size() + " rare items");
                
                if (canCraft) {
                    player.sendMessage(MessageManager.MUTED + "  XP Reward: " + 
                                     MessageManager.HIGHLIGHT + EXPERIENCE_REWARDS.get(tier));
                } else {
                    player.sendMessage(ChatColor.RED + "  🔒 Level up to unlock!");
                }
                
                player.sendMessage("");
            }
        }
        
        player.sendMessage(MessageManager.ACCENT + "💡 Your level increases success rates!");
        player.sendMessage(MessageManager.MUTED + "Place all required materials in a crafting table!");
        player.sendMessage(MessageManager.PRIMARY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // Material creation methods for recipes
    private static List<Material> createBasicRecipeMaterials() {
        return Arrays.asList(
            Material.DIAMOND, Material.EMERALD, Material.QUARTZ,
            Material.GOLD_INGOT, Material.REDSTONE, Material.LAPIS_LAZULI,
            Material.AMETHYST_SHARD, Material.PRISMARINE_CRYSTALS, Material.BLAZE_POWDER
        );
    }
    
    private static List<Material> createRareRecipeMaterials() {
        return Arrays.asList(
            Material.NETHERITE_SCRAP, Material.END_CRYSTAL, Material.DRAGON_BREATH,
            Material.BLAZE_ROD, Material.ENDER_PEARL, Material.GHAST_TEAR,
            Material.PHANTOM_MEMBRANE, Material.ECHO_SHARD, Material.HEART_OF_THE_SEA
        );
    }
    
    private static List<Material> createLegendaryRecipeMaterials() {
        return Arrays.asList(
            Material.ELYTRA, Material.BEACON, Material.NETHER_STAR,
            Material.TOTEM_OF_UNDYING, Material.NETHERITE_INGOT, Material.ENCHANTED_GOLDEN_APPLE,
            Material.END_PORTAL_FRAME, Material.CONDUIT, Material.DRAGON_EGG
        );
    }
    
    // Lore creation methods
    private static List<String> createBasicCrystalLore() {
        return Arrays.asList(
            ChatColor.GRAY + "(Crafted Apprentice Crystal)",
            ChatColor.BLUE + "⚡ Humming with basic energy",
            ChatColor.YELLOW + "Right-click to activate",
            ChatColor.LIGHT_PURPLE + "✨ Basic tier enchantments ✨",
            ChatColor.DARK_GRAY + "A beginner's first crystal"
        );
    }
    
    private static List<String> createRareCrystalLore() {
        return Arrays.asList(
            ChatColor.GRAY + "(Crafted Adept Crystal)",
            ChatColor.DARK_PURPLE + "⚡ Pulsing with adept energy",
            ChatColor.GOLD + "Right-click to activate",
            ChatColor.LIGHT_PURPLE + "✨ Enhanced tier enchantments ✨",
            ChatColor.DARK_GRAY + "Forged from otherworldly materials"
        );
    }
    
    private static List<String> createLegendaryCrystalLore() {
        return Arrays.asList(
            ChatColor.GRAY + "(Crafted Masterwork Crystal)",
            ChatColor.GOLD + "⚡ Radiating masterwork power",
            ChatColor.AQUA + "Right-click to activate",
            ChatColor.LIGHT_PURPLE + "✨ Maximum tier enchantments ✨",
            ChatColor.RED + "★ Forged from legendary artifacts ★",
            ChatColor.DARK_RED + "The pinnacle of crystal mastery"
        );
    }

    /**
     * Crystal tier enumeration.
     */
    public enum CrystalTier {
        APPRENTICE("Apprentice Crystal", 1),
        ADEPT("Adept Crystal", 2),
        MASTERWORK("Masterwork Crystal", 3);
        
        private final String displayName;
        private final int tier;
        
        CrystalTier(String displayName, int tier) {
            this.displayName = displayName;
            this.tier = tier;
        }
        
        public String getDisplayName() { return displayName; }
        public int getTier() { return tier; }
    }

    /**
     * Enhanced crystal recipe data class.
     */
    private static class CrystalRecipe {
        final CrystalTier tier;
        final List<Material> requiredMaterials;
        final String displayName;
        final List<String> baseLore;
        final double successRate;
        final int levelRequirement;
        final List<String> possibleEffects;

        CrystalRecipe(CrystalTier tier, List<Material> materials, String name, 
                     List<String> lore, double successRate, int levelRequirement,
                     List<String> possibleEffects) {
            this.tier = tier;
            this.requiredMaterials = new ArrayList<>(materials);
            this.displayName = name;
            this.baseLore = new ArrayList<>(lore);
            this.successRate = successRate;
            this.levelRequirement = levelRequirement;
            this.possibleEffects = new ArrayList<>(possibleEffects);
        }
    }
} 