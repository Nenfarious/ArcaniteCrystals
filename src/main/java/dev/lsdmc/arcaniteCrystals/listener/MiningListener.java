// src/main/java/dev/lsdmc/arcaniteCrystals/listener/MiningListener.java
package dev.lsdmc.arcaniteCrystals.listener;

import dev.lsdmc.arcaniteCrystals.manager.MiningEffectManager;
import dev.lsdmc.arcaniteCrystals.util.ParticleManager;
import dev.lsdmc.arcaniteCrystals.util.SoundManager;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Professional mining effects listener with comprehensive ore processing,
 * enhanced drop mechanics, advanced fortune system, and immersive feedback.
 */
public class MiningListener implements Listener {

    // Comprehensive ore type mappings for auto-smelt
    private static final Map<Material, Material> SMELTABLE_ORES = Map.of(
        // Basic ores
        Material.IRON_ORE, Material.IRON_INGOT,
        Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT,
        Material.GOLD_ORE, Material.GOLD_INGOT,
        Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT,
        Material.COPPER_ORE, Material.COPPER_INGOT,
        Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT,
        // Ancient debris special case
        Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP
    );

    // Enhanced fortune-affected blocks with custom multipliers
    private static final Map<Material, FortuneConfig> FORTUNE_BLOCKS;
    
    static {
        Map<Material, FortuneConfig> fortuneMap = new HashMap<>();
        
        // Gem ores with high fortune multipliers
        fortuneMap.put(Material.DIAMOND_ORE, new FortuneConfig(Material.DIAMOND, 1, 4, 0.3));
        fortuneMap.put(Material.DEEPSLATE_DIAMOND_ORE, new FortuneConfig(Material.DIAMOND, 1, 4, 0.3));
        fortuneMap.put(Material.EMERALD_ORE, new FortuneConfig(Material.EMERALD, 1, 4, 0.25));
        fortuneMap.put(Material.DEEPSLATE_EMERALD_ORE, new FortuneConfig(Material.EMERALD, 1, 4, 0.25));
        
        // Coal with moderate fortune
        fortuneMap.put(Material.COAL_ORE, new FortuneConfig(Material.COAL, 1, 3, 0.4));
        fortuneMap.put(Material.DEEPSLATE_COAL_ORE, new FortuneConfig(Material.COAL, 1, 3, 0.4));
        
        // Redstone with high yield
        fortuneMap.put(Material.REDSTONE_ORE, new FortuneConfig(Material.REDSTONE, 2, 6, 0.5));
        fortuneMap.put(Material.DEEPSLATE_REDSTONE_ORE, new FortuneConfig(Material.REDSTONE, 2, 6, 0.5));
        
        // Lapis with high yield
        fortuneMap.put(Material.LAPIS_ORE, new FortuneConfig(Material.LAPIS_LAZULI, 3, 8, 0.45));
        fortuneMap.put(Material.DEEPSLATE_LAPIS_ORE, new FortuneConfig(Material.LAPIS_LAZULI, 3, 8, 0.45));
        
        // Quartz
        fortuneMap.put(Material.NETHER_QUARTZ_ORE, new FortuneConfig(Material.QUARTZ, 1, 3, 0.35));
        
        // Amethyst
        fortuneMap.put(Material.AMETHYST_CLUSTER, new FortuneConfig(Material.AMETHYST_SHARD, 2, 6, 0.4));
        
        FORTUNE_BLOCKS = Collections.unmodifiableMap(fortuneMap);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        var player = event.getPlayer();
        var block = event.getBlock();
        Material blockType = block.getType();
        
        boolean hasAutoSmelt = MiningEffectManager.hasAutoSmelt(player.getUniqueId());
        boolean hasFortune = MiningEffectManager.hasFortune(player.getUniqueId());
        
        // Early return if no mining effects are active
        if (!hasAutoSmelt && !hasFortune) return;
        
        boolean processed = false;
        
        // Auto-smelt processing with enhanced feedback
        if (hasAutoSmelt && SMELTABLE_ORES.containsKey(blockType)) {
            processed = processAutoSmelt(event, blockType);
        }
        
        // Fortune processing with sophisticated drop calculation
        if (hasFortune && FORTUNE_BLOCKS.containsKey(blockType)) {
            processed = processFortune(event, blockType) || processed;
        }
        
        // Provide feedback only if effects were applied
        if (processed) {
            // Experience bonus for enhanced mining
            int baseExp = event.getExpToDrop();
            if (baseExp > 0) {
                int bonusExp = (int) (baseExp * 0.25); // 25% bonus XP
                event.setExpToDrop(baseExp + bonusExp);
            }
            
            // Professional feedback message (rate-limited)
            if (ThreadLocalRandom.current().nextDouble() < 0.1) { // 10% chance
                String effectType = hasAutoSmelt && hasFortune ? "Auto-Smelt & Fortune" :
                                  hasAutoSmelt ? "Auto-Smelt" : "Fortune";
                player.sendMessage(MessageManager.ACCENT + "⚡ " + effectType + " activated!");
            }
        }
    }
    
    /**
     * Processes auto-smelt effects with comprehensive ore handling and tool enchantment respect.
     */
    private boolean processAutoSmelt(BlockBreakEvent event, Material blockType) {
        Material smeltedMaterial = SMELTABLE_ORES.get(blockType);
        if (smeltedMaterial == null) return false;
        
        var player = event.getPlayer();
        var block = event.getBlock();
        
        // Cancel default drops
        event.setDropItems(false);
        
        // Calculate drop amount (respect silk touch but not fortune for auto-smelt)
        ItemStack tool = player.getInventory().getItemInMainHand();
        int dropAmount = 1;
        
        // Don't apply fortune to auto-smelted items to prevent double fortune
        if (tool.hasItemMeta() && tool.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)) {
            // Silk touch overrides auto-smelt - drop original block
            ItemStack originalDrop = new ItemStack(blockType, 1);
            block.getWorld().dropItemNaturally(block.getLocation(), originalDrop);
            return false;
        }
        
        // Special handling for ancient debris (no fortune ever)
        if (blockType == Material.ANCIENT_DEBRIS) {
            dropAmount = 1; // Always 1 scrap
        }
        
        // Drop smelted items
        ItemStack smeltedDrop = new ItemStack(smeltedMaterial, dropAmount);
        block.getWorld().dropItemNaturally(block.getLocation(), smeltedDrop);
        
        // Enhanced visual and audio feedback
        ParticleManager.playMiningEffect(block.getLocation(), true);
        SoundManager.playMiningSound(block.getLocation(), true);
        
        return true;
    }
    
    /**
     * Processes fortune effects with sophisticated drop calculation and proper stacking prevention.
     */
    private boolean processFortune(BlockBreakEvent event, Material blockType) {
        FortuneConfig config = FORTUNE_BLOCKS.get(blockType);
        if (config == null) return false;
        
        var player = event.getPlayer();
        var block = event.getBlock();
        ItemStack tool = player.getInventory().getItemInMainHand();
        
        // Check if fortune should trigger (independent of tool fortune)
        if (ThreadLocalRandom.current().nextDouble() > config.triggerChance) {
            return false; // Crystal fortune didn't trigger this time
        }
        
        // Get existing tool fortune level for calculation separation
        int toolFortuneLevel = 0;
        if (tool.hasItemMeta() && tool.getItemMeta().hasEnchant(Enchantment.FORTUNE)) {
            toolFortuneLevel = tool.getItemMeta().getEnchantLevel(Enchantment.FORTUNE);
        }
        
        // Calculate crystal fortune bonus (separate from tool fortune)
        int crystalFortuneBonus = ThreadLocalRandom.current().nextInt(
            config.minBonus, config.maxBonus + 1);
        
        // Apply crystal fortune multiplier (more controlled than before)
        crystalFortuneBonus = Math.max(1, (int) (crystalFortuneBonus * 1.2)); // 20% more effective, reduced from 50%
        
        // Cap total bonus items to prevent excessive drops
        int maxBonusItems = ConfigManager.getConfig().getInt("mining.fortune.max-bonus-items", 5);
        crystalFortuneBonus = Math.min(crystalFortuneBonus, maxBonusItems);
        
        // Only add crystal bonus if it provides meaningful improvement
        if (crystalFortuneBonus > 0) {
            // Create bonus drops that are clearly from crystal enhancement
            ItemStack bonusDrop = new ItemStack(config.dropMaterial, crystalFortuneBonus);
            
            // Add custom metadata to track crystal-generated drops
            ItemMeta bonusMeta = bonusDrop.getItemMeta();
            if (bonusMeta != null) {
                bonusMeta.setDisplayName("Crystal-Enhanced " + config.dropMaterial.name());
                bonusDrop.setItemMeta(bonusMeta);
            }
            
            block.getWorld().dropItemNaturally(block.getLocation(), bonusDrop);
            
            // Log for debugging/monitoring
            if (ThreadLocalRandom.current().nextDouble() < 0.1) { // 10% chance to log
                player.sendMessage(ChatColor.DARK_PURPLE + "⚡ Crystal Fortune: +" + crystalFortuneBonus + " " + config.dropMaterial.name());
            }
        }
        
        // Enhanced visual and audio feedback for fortune
        ParticleManager.playMiningEffect(block.getLocation(), false);
        SoundManager.playMiningSound(block.getLocation(), false);
        
        return true;
    }
    
    /**
     * Configuration for fortune-affected blocks.
     */
    private static class FortuneConfig {
        final Material dropMaterial;
        final int minBonus;
        final int maxBonus;
        final double triggerChance;
        
        FortuneConfig(Material dropMaterial, int minBonus, int maxBonus, double triggerChance) {
            this.dropMaterial = dropMaterial;
            this.minBonus = minBonus;
            this.maxBonus = maxBonus;
            this.triggerChance = triggerChance;
        }
    }
}
