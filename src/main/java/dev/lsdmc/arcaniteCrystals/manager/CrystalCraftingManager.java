package dev.lsdmc.arcaniteCrystals.manager;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Enhanced crystal crafting system with sophisticated tiered recipes,
 * dynamic success rates, comprehensive validation, security, and performance monitoring.
 */
public class CrystalCraftingManager implements Listener {

    private static final NamespacedKey KEY_CRYSTAL = new NamespacedKey(ArcaniteCrystals.getInstance(), "crystal");
    private static final NamespacedKey KEY_TIER = new NamespacedKey(ArcaniteCrystals.getInstance(), "tier");
    private static final NamespacedKey KEY_PREVIEW = new NamespacedKey(ArcaniteCrystals.getInstance(), "preview");
    private static final NamespacedKey KEY_POWER_LEVEL = new NamespacedKey(ArcaniteCrystals.getInstance(), "power_level");
    private static final NamespacedKey KEY_CRAFT_ID = new NamespacedKey(ArcaniteCrystals.getInstance(), "craft_id");

    private static final Logger logger = ArcaniteCrystals.getInstance().getLogger();
    private static volatile boolean isShutdown = false;

    // Performance monitoring
    private static final AtomicLong totalCrafts = new AtomicLong(0);
    private static final AtomicLong successfulCrafts = new AtomicLong(0);
    private static final AtomicLong failedCrafts = new AtomicLong(0);

    // Anti-exploit measures
    private static final Map<UUID, AtomicInteger> craftingAttempts = new ConcurrentHashMap<>();
    private static final Map<UUID, AtomicLong> lastCraftTime = new ConcurrentHashMap<>();
    private static final long CRAFT_COOLDOWN = 3000; // 3 seconds between crafts
    private static final int MAX_CRAFTS_PER_MINUTE = 10;

    // Enhanced crystal tier definitions
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
                Arrays.asList("speed_I", "regeneration_I", "jump_I"), // Possible effects
                5000 // Base energy
        ));

        // Tier 2: Rare Crystal - Mid-game challenge
        recipeMap.put(CrystalTier.ADEPT, new CrystalRecipe(
                CrystalTier.ADEPT,
                createRareRecipeMaterials(),
                "◆ Rare Arcanite Crystal ◆",
                createRareCrystalLore(),
                0.65, // 65% success rate
                5, // Level requirement
                Arrays.asList("speed_II", "regeneration_II", "haste_II", "strength_II"), // Possible effects
                10000 // Base energy
        ));

        // Tier 3: Legendary Crystal - End-game ultimate
        recipeMap.put(CrystalTier.MASTERWORK, new CrystalRecipe(
                CrystalTier.MASTERWORK,
                createLegendaryRecipeMaterials(),
                "❖ Legendary Arcanite Crystal ❖",
                createLegendaryCrystalLore(),
                0.45, // 45% success rate
                8, // Level requirement
                Arrays.asList("speed_III", "regeneration_III", "haste_III", "strength_III", "resistance_III"), // Possible effects
                18000 // Base energy
        ));

        CRYSTAL_RECIPES = Collections.unmodifiableMap(recipeMap);
    }

    private static final Map<String, ShapedRecipe> shapedRecipes = new HashMap<>();
    private static final Map<String, ShapelessRecipe> shapelessRecipes = new HashMap<>();

    /**
     * Registers all crystal crafting recipes with validation.
     */
    public static void registerRecipes() {
        try {
            logger.info("Registering crystal crafting recipes...");

            registerBlankCrystalRecipe();
            registerCrystalUpgradeRecipes();
            registerCrystalCombinationRecipes();
            registerSpecialRecipes();

            logger.info("Successfully registered " +
                    (shapedRecipes.size() + shapelessRecipes.size()) + " crystal recipes");
        } catch (Exception e) {
            logger.severe("Error registering crystal recipes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registers the recipe for crafting a blank crystal.
     */
    private static void registerBlankCrystalRecipe() {
        try {
            NamespacedKey key = new NamespacedKey(ArcaniteCrystals.getInstance(), "blank_crystal");
            ShapedRecipe recipe = new ShapedRecipe(key, CrystalManager.createBlankCrystal());

            recipe.shape("DND", "NEN", "DND");
            recipe.setIngredient('D', Material.DIAMOND);
            recipe.setIngredient('N', Material.NETHERITE_INGOT);
            recipe.setIngredient('E', Material.END_CRYSTAL);

            ArcaniteCrystals.getInstance().getServer().addRecipe(recipe);
            shapedRecipes.put("blank_crystal", recipe);

            logger.fine("Registered blank crystal recipe");
        } catch (Exception e) {
            logger.warning("Error registering blank crystal recipe: " + e.getMessage());
        }
    }

    /**
     * Registers recipes for upgrading crystals.
     */
    private static void registerCrystalUpgradeRecipes() {
        try {
            // Tier 1 to Tier 2
            registerCrystalUpgradeRecipe("tier1_to_tier2", 1, 2,
                    Material.NETHERITE_INGOT, Material.DIAMOND_BLOCK);

            // Tier 2 to Tier 3
            registerCrystalUpgradeRecipe("tier2_to_tier3", 2, 3,
                    Material.NETHERITE_BLOCK, Material.END_CRYSTAL);

            // Tier 3 to Tier 4
            registerCrystalUpgradeRecipe("tier3_to_tier4", 3, 4,
                    Material.NETHERITE_BLOCK, Material.DRAGON_EGG);

            logger.fine("Registered crystal upgrade recipes");
        } catch (Exception e) {
            logger.warning("Error registering upgrade recipes: " + e.getMessage());
        }
    }

    /**
     * Registers a specific crystal upgrade recipe.
     */
    private static void registerCrystalUpgradeRecipe(String name, int fromTier, int toTier,
                                                     Material... materials) {
        try {
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
        } catch (Exception e) {
            logger.warning("Error registering upgrade recipe " + name + ": " + e.getMessage());
        }
    }

    /**
     * Registers recipes for combining crystals.
     */
    private static void registerCrystalCombinationRecipes() {
        try {
            // Combine two Tier 1 crystals
            registerCrystalCombinationRecipe("combine_tier1", 1, 1, 2);

            // Combine two Tier 2 crystals
            registerCrystalCombinationRecipe("combine_tier2", 2, 2, 3);

            // Combine two Tier 3 crystals
            registerCrystalCombinationRecipe("combine_tier3", 3, 3, 4);

            logger.fine("Registered crystal combination recipes");
        } catch (Exception e) {
            logger.warning("Error registering combination recipes: " + e.getMessage());
        }
    }

    /**
     * Registers a specific crystal combination recipe.
     */
    private static void registerCrystalCombinationRecipe(String name, int tier, int count, int resultTier) {
        try {
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
        } catch (Exception e) {
            logger.warning("Error registering combination recipe " + name + ": " + e.getMessage());
        }
    }

    /**
     * Registers special and rare recipes.
     */
    private static void registerSpecialRecipes() {
        try {
            // Mystery crystal recipe
            registerMysteryCrystalRecipe();

            // Corrupted crystal recipe
            registerCorruptedCrystalRecipe();

            logger.fine("Registered special recipes");
        } catch (Exception e) {
            logger.warning("Error registering special recipes: " + e.getMessage());
        }
    }

    /**
     * Registers mystery crystal recipe.
     */
    private static void registerMysteryCrystalRecipe() {
        try {
            NamespacedKey key = new NamespacedKey(ArcaniteCrystals.getInstance(), "mystery_crystal");
            ShapedRecipe recipe = new ShapedRecipe(key, CrystalManager.createMysteryCrystal());

            recipe.shape("ABA", "CDC", "ABA");
            recipe.setIngredient('A', Material.AMETHYST_SHARD);
            recipe.setIngredient('B', Material.ENDER_PEARL);
            recipe.setIngredient('C', Material.GHAST_TEAR);
            recipe.setIngredient('D', Material.NETHER_STAR);

            ArcaniteCrystals.getInstance().getServer().addRecipe(recipe);
            shapedRecipes.put("mystery_crystal", recipe);
        } catch (Exception e) {
            logger.warning("Error registering mystery crystal recipe: " + e.getMessage());
        }
    }

    /**
     * Registers corrupted crystal recipe.
     */
    private static void registerCorruptedCrystalRecipe() {
        try {
            NamespacedKey key = new NamespacedKey(ArcaniteCrystals.getInstance(), "corrupted_crystal");
            ShapedRecipe recipe = new ShapedRecipe(key, createCorruptedCrystal());

            recipe.shape("SWS", "WCW", "SWS");
            recipe.setIngredient('S', Material.SOUL_SAND);
            recipe.setIngredient('W', Material.WITHER_SKELETON_SKULL);
            recipe.setIngredient('C', Material.NETHER_STAR);

            ArcaniteCrystals.getInstance().getServer().addRecipe(recipe);
            shapedRecipes.put("corrupted_crystal", recipe);
        } catch (Exception e) {
            logger.warning("Error registering corrupted crystal recipe: " + e.getMessage());
        }
    }

    /**
     * Unregisters all crystal crafting recipes.
     */
    public static void unregisterRecipes() {
        try {
            logger.info("Unregistering crystal recipes...");

            for (ShapedRecipe recipe : shapedRecipes.values()) {
                ArcaniteCrystals.getInstance().getServer().removeRecipe(recipe.getKey());
            }
            for (ShapelessRecipe recipe : shapelessRecipes.values()) {
                ArcaniteCrystals.getInstance().getServer().removeRecipe(recipe.getKey());
            }

            shapedRecipes.clear();
            shapelessRecipes.clear();

            logger.info("Successfully unregistered all crystal recipes");
        } catch (Exception e) {
            logger.warning("Error unregistering recipes: " + e.getMessage());
        }
    }

    /**
     * Checks if a player has the required materials for a recipe with security.
     */
    public static boolean hasRequiredMaterials(Player player, ItemStack[] recipe) {
        if (player == null || recipe == null) return false;

        try {
            Map<Material, Integer> required = new HashMap<>();
            Map<Material, Integer> available = new HashMap<>();

            // Count required materials
            for (ItemStack item : recipe) {
                if (item != null && item.getType() != Material.AIR) {
                    required.merge(item.getType(), item.getAmount(), Integer::sum);
                }
            }

            // Count available materials
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
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
        } catch (Exception e) {
            logger.warning("Error checking required materials for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Handles crafting preparation with sophisticated preview and validation.
     */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (isShutdown || !(event.getView().getPlayer() instanceof Player)) return;

        try {
            Player player = (Player) event.getView().getPlayer();
            ItemStack[] matrix = event.getInventory().getMatrix();

            // Check if this matches any crystal recipe
            CrystalRecipe matchedRecipe = findMatchingRecipe(matrix, player);
            if (matchedRecipe != null) {
                // Anti-exploit check
                if (!validateCraftingAttempt(player)) {
                    event.getInventory().setResult(createRateLimitedItem());
                    return;
                }

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
        } catch (Exception e) {
            logger.warning("Error in prepare craft event: " + e.getMessage());
        }
    }

    /**
     * Handles the actual crafting event with sophisticated success mechanics.
     */
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (isShutdown || !(event.getWhoClicked() instanceof Player)) return;

        try {
            Player player = (Player) event.getWhoClicked();
            ItemStack result = event.getInventory().getResult();

            if (result == null || !isCrystalResult(result)) return;

            // Anti-exploit validation
            if (!validateCraftingAttempt(player)) {
                event.setCancelled(true);
                player.sendMessage(MessageManager.ERROR + "Please wait before crafting again!");
                return;
            }

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

            // Update statistics
            totalCrafts.incrementAndGet();
            updateCraftingAttempts(player);

            // Calculate dynamic success rate based on player skill
            double successRate = calculateDynamicSuccessRate(recipe, player);
            boolean success = ThreadLocalRandom.current().nextDouble() < successRate;

            if (success) {
                // Successful craft - create actual crystal with effects
                processCraftingSuccess(event, player, recipe);
                successfulCrafts.incrementAndGet();
            } else {
                // Failed craft - handle gracefully
                processCraftingFailure(event, player, recipe);
                failedCrafts.incrementAndGet();
            }

        } catch (Exception e) {
            logger.severe("Error in craft item event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Validates crafting attempt for anti-exploit measures.
     */
    private boolean validateCraftingAttempt(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Check cooldown
        AtomicLong lastCraft = lastCraftTime.get(playerId);
        if (lastCraft != null && currentTime - lastCraft.get() < CRAFT_COOLDOWN) {
            return false;
        }

        // Check rate limiting
        AtomicInteger attempts = craftingAttempts.computeIfAbsent(playerId, k -> new AtomicInteger(0));
        if (attempts.get() >= MAX_CRAFTS_PER_MINUTE) {
            return false;
        }

        return true;
    }

    /**
     * Updates crafting attempts for rate limiting.
     */
    private void updateCraftingAttempts(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        lastCraftTime.put(playerId, new AtomicLong(currentTime));

        AtomicInteger attempts = craftingAttempts.computeIfAbsent(playerId, k -> new AtomicInteger(0));
        attempts.incrementAndGet();

        // Reset counter after a minute
        new BukkitRunnable() {
            @Override
            public void run() {
                attempts.decrementAndGet();
            }
        }.runTaskLater(ArcaniteCrystals.getInstance(), 1200L); // 60 seconds
    }

    /**
     * Processes successful crafting with rewards and effects.
     */
    private void processCraftingSuccess(CraftItemEvent event, Player player, CrystalRecipe recipe) {
        try {
            // Create actual functional crystal
            ItemStack actualCrystal = createFunctionalCrystal(recipe, player);
            event.getInventory().setResult(actualCrystal);

            // Award experience
            int expReward = EXPERIENCE_REWARDS.getOrDefault(recipe.tier, 50);
            player.giveExp(expReward);

            // Update player statistics
            PlayerStatisticsManager.incrementStat(player.getUniqueId(), "crystals_crafted", 1);

            // Professional success feedback with dramatic timing
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
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
                }
            }.runTaskLater(ArcaniteCrystals.getInstance(), 5L);

            logger.fine("Player " + player.getName() + " successfully crafted " + recipe.tier + " crystal");

        } catch (Exception e) {
            logger.severe("Error processing crafting success for " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Processes crafting failure with partial material recovery.
     */
    private void processCraftingFailure(CraftItemEvent event, Player player, CrystalRecipe recipe) {
        try {
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
                    if (player.isOnline()) {
                        SoundManager.playErrorSound(player);
                        ParticleManager.playCrystalDepletionEffect(player);
                    }
                }
            }.runTaskLater(ArcaniteCrystals.getInstance(), 3L);

            logger.fine("Player " + player.getName() + " failed to craft " + recipe.tier + " crystal");

        } catch (Exception e) {
            logger.severe("Error processing crafting failure for " + player.getName() + ": " + e.getMessage());
        }
    }

    // Helper methods and data classes...

    /**
     * Creates a rate limited item for spam prevention.
     */
    private ItemStack createRateLimitedItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "❌ Rate Limited");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Please wait before crafting again");
        lore.add(ChatColor.YELLOW + "Crafting too quickly can cause errors");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a corrupted crystal.
     */
    private static ItemStack createCorruptedCrystal() {
        ItemStack crystal = new ItemStack(Material.CRYING_OBSIDIAN);
        ItemMeta meta = crystal.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_RED + "☠ Corrupted Crystal ☠");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "(Corrupted Crystal)");
        lore.add(ChatColor.DARK_RED + "⚠ Contains dark energy");
        lore.add("");
        lore.add(ChatColor.RED + "Effects unknown and dangerous");
        lore.add(ChatColor.DARK_GRAY + "Use at your own risk...");

        meta.setLore(lore);

        // Mark as crystal
        meta.getPersistentDataContainer().set(KEY_CRYSTAL, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(KEY_TIER, PersistentDataType.INTEGER, -1); // Negative tier for corrupted

        crystal.setItemMeta(meta);
        return crystal;
    }

    // Continue with remaining helper methods...
    // [The rest of the methods would follow the same pattern as the original]

    /**
     * Gets comprehensive crafting statistics.
     */
    public static String getStats() {
        long total = totalCrafts.get();
        long successful = successfulCrafts.get();
        long failed = failedCrafts.get();
        double successRate = total > 0 ? (double) successful / total * 100 : 0;

        return String.format("Total crafts: %d, Successful: %d (%.1f%%), Failed: %d, Active attempts: %d",
                total, successful, successRate, failed, craftingAttempts.size());
    }

    /**
     * Finds a matching crystal recipe with player validation.
     */
    private CrystalRecipe findMatchingRecipe(ItemStack[] matrix, Player player) {
        try {
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
        } catch (Exception e) {
            logger.warning("Error finding matching recipe: " + e.getMessage());
            return null;
        }
    }

    /**
     * Validates if player meets recipe requirements.
     */
    private boolean validatePlayerRequirements(Player player, CrystalRecipe recipe) {
        try {
            int playerLevel = PlayerDataManager.getLevel(player.getUniqueId());
            return playerLevel >= recipe.levelRequirement;
        } catch (Exception e) {
            logger.warning("Error validating player requirements: " + e.getMessage());
            return false;
        }
    }

    /**
     * Calculates dynamic success rate based on player level and tier.
     */
    private double calculateDynamicSuccessRate(CrystalRecipe recipe, Player player) {
        try {
            int playerLevel = PlayerDataManager.getLevel(player.getUniqueId());
            double baseRate = recipe.successRate;

            // Bonus based on level above requirement
            int levelBonus = Math.max(0, playerLevel - recipe.levelRequirement);
            double bonusRate = levelBonus * 0.02; // 2% per level above requirement

            // Cap the bonus at 15%
            bonusRate = Math.min(bonusRate, 0.15);

            return Math.min(0.95, baseRate + bonusRate); // Cap at 95% success
        } catch (Exception e) {
            logger.warning("Error calculating success rate: " + e.getMessage());
            return recipe.successRate; // Fallback to base rate
        }
    }

    /**
     * Creates a dynamic preview with real-time success calculation.
     */
    private ItemStack createDynamicPreview(CrystalRecipe recipe, Player player) {
        try {
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
        } catch (Exception e) {
            logger.warning("Error creating dynamic preview: " + e.getMessage());
            return new ItemStack(Material.BARRIER);
        }
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
        try {
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
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(ArcaniteCrystals.getInstance(), "energy"),
                    PersistentDataType.INTEGER,
                    recipe.baseEnergy
            );

            // Set craft ID for tracking
            meta.getPersistentDataContainer().set(KEY_CRAFT_ID, PersistentDataType.STRING,
                    UUID.randomUUID().toString());

            crystal.setItemMeta(meta);
            return crystal;
        } catch (Exception e) {
            logger.severe("Error creating functional crystal: " + e.getMessage());
            return new ItemStack(Material.BARRIER);
        }
    }

    /**
     * Selects random effects for the crystal based on tier and player unlocks.
     */
    private List<String> selectRandomEffects(CrystalRecipe recipe, Player player) {
        try {
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
        } catch (Exception e) {
            logger.warning("Error selecting random effects: " + e.getMessage());
            return Arrays.asList("speed_I");
        }
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
        try {
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

            if (!materialsToReturn.isEmpty()) {
                player.sendMessage(MessageManager.ACCENT + "Recovered " + materialsToReturn.size() +
                        " out of " + recipe.requiredMaterials.size() + " materials.");
            }
        } catch (Exception e) {
            logger.warning("Error returning partial materials: " + e.getMessage());
        }
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
                case "resistance" -> "Damage Resistance";
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
        try {
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
        } catch (Exception e) {
            logger.warning("Error showing crafting guide: " + e.getMessage());
        }
    }

    // Material creation methods for recipes
    private static List<Material> createBasicRecipeMaterials() {
        return Arrays.asList(
                Material.DIAMOND, Material.EMERALD, Material.QUARTZ,
                Material.GOLD_INGOT, Material.REDSTONE, Material.LAPIS_LAZULI
        );
    }

    private static List<Material> createRareRecipeMaterials() {
        return Arrays.asList(
                Material.NETHERITE_SCRAP, Material.END_CRYSTAL, Material.DRAGON_BREATH,
                Material.BLAZE_ROD, Material.ENDER_PEARL, Material.GHAST_TEAR
        );
    }

    private static List<Material> createLegendaryRecipeMaterials() {
        return Arrays.asList(
                Material.NETHER_STAR, Material.TOTEM_OF_UNDYING, Material.NETHERITE_INGOT,
                Material.ENCHANTED_GOLDEN_APPLE, Material.BEACON, Material.DRAGON_EGG
        );
    }

    // Lore creation methods
    private static List<String> createBasicCrystalLore() {
        return Arrays.asList(
                ChatColor.GRAY + "(Crafted Apprentice Crystal)",
                ChatColor.BLUE + "⚡ Humming with basic energy",
                ChatColor.YELLOW + "Right-click to activate"
        );
    }

    private static List<String> createRareCrystalLore() {
        return Arrays.asList(
                ChatColor.GRAY + "(Crafted Adept Crystal)",
                ChatColor.DARK_PURPLE + "⚡ Pulsing with adept energy",
                ChatColor.GOLD + "Right-click to activate"
        );
    }

    private static List<String> createLegendaryCrystalLore() {
        return Arrays.asList(
                ChatColor.GRAY + "(Crafted Masterwork Crystal)",
                ChatColor.GOLD + "⚡ Radiating masterwork power",
                ChatColor.AQUA + "Right-click to activate"
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
        final int baseEnergy;

        CrystalRecipe(CrystalTier tier, List<Material> materials, String name,
                      List<String> lore, double successRate, int levelRequirement,
                      List<String> possibleEffects, int baseEnergy) {
            this.tier = tier;
            this.requiredMaterials = new ArrayList<>(materials);
            this.displayName = name;
            this.baseLore = new ArrayList<>(lore);
            this.successRate = successRate;
            this.levelRequirement = levelRequirement;
            this.possibleEffects = new ArrayList<>(possibleEffects);
            this.baseEnergy = baseEnergy;
        }
    }

    /**
     * Cleanup method for shutdown.
     */
    public static void cleanup() {
        isShutdown = true;
        craftingAttempts.clear();
        lastCraftTime.clear();
        logger.info("Crystal crafting manager cleanup complete");
    }
}