package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ProgressDashboardGUI implements InventoryHolder, Listener {
    private static final int LEVEL_PROG_SLOT = 0;
    private static final int TIER_PROG_SLOT = 1;
    private static final int UPGRADE_PROG_SLOT = 2;
    private static final int ACHIEVEMENT_PROG_SLOT = 3;
    private static final int STATS_SLOT = 4;
    
    private static final int REQUIREMENTS_SLOT = 18;
    private static final int NEXT_GOALS_SLOT = 20;
    private static final int RECOMMENDATIONS_SLOT = 22;
    private static final int MILESTONES_SLOT = 24;
    
    private static final int SERVER_RANK_SLOT = 27;
    private static final int RECENT_PROG_SLOT = 29;
    private static final int GOALS_SLOT = 31;
    private static final int EXPORT_SLOT = 33;
    
    private static final int HELP_SLOT = 45;
    private static final int REFRESH_SLOT = 47;
    private static final int BACK_SLOT = 49;
    private static final int CLOSE_SLOT = 53;
    
    private final Player player;
    private final UUID playerId;
    private final Inventory inventory;
    private boolean isOpen = false;
    
    public ProgressDashboardGUI(Player player) {
        this.player = player;
        this.playerId = player.getUniqueId();
        this.inventory = Bukkit.createInventory(this, 54, 
            ChatColor.DARK_PURPLE + "✦ Progress Dashboard ✦");
        buildDashboard();
    }
    
    private void buildDashboard() {
        // Progress overview cards
        addLevelProgressCard();
        addTierProgressCard();
        addUpgradeProgressCard();
        addAchievementProgressCard();
        addStatisticsCard();
        
        // Detailed breakdown
        addRequirementsBreakdown();
        addNextGoals();
        addRecommendations();
        addMilestones();
        
        // Analytics
        addServerRanking();
        addRecentProgress();
        addGoalTracking();
        
        // Utility buttons
        addUtilityButtons();
    }
    
    private void addLevelProgressCard() {
        int currentLevel = PlayerDataManager.getLevel(playerId);
        int nextLevel = currentLevel + 1;
        
        ItemStack levelCard = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta levelMeta = levelCard.getItemMeta();
        levelMeta.setDisplayName(ChatColor.GREEN + "Level Progress");
        
        List<String> levelLore = new ArrayList<>();
        levelLore.add(ChatColor.BLUE + "Current Level: " + ChatColor.WHITE + currentLevel + "/10");
        
        if (nextLevel <= 10) {
            dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.LevelConfiguration nextConfig = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getLevelConfiguration(nextLevel);
            if (nextConfig != null) {
                dev.lsdmc.arcaniteCrystals.util.RequirementChecker checker = new dev.lsdmc.arcaniteCrystals.util.RequirementChecker(player);
                List<String> missing = checker.getMissingForLevel(nextLevel);
                
                if (missing.isEmpty()) {
                    levelLore.add(ChatColor.GREEN + "✓ Ready to level up!");
                    levelLore.add(ChatColor.YELLOW + "Use /levelup command");
                } else {
                    levelLore.add(ChatColor.YELLOW + "Requirements for Level " + nextLevel + ":");
                    for (String req : missing) {
                        levelLore.add(ChatColor.RED + "  ✗ " + req);
                    }
                    
                    // Progress bars for each requirement
                    var requirements = nextConfig.getRequirements();
                    if (requirements.getMoney() > 0) {
                        double balance = getPlayerBalance();
                        double progress = Math.min(100, balance / requirements.getMoney() * 100);
                        levelLore.add(ChatColor.GOLD + "Money: " + createProgressBar(progress));
                    }
                    
                    if (requirements.getPlayerKills() > 0) {
                        int kills = player.getStatistic(Statistic.PLAYER_KILLS);
                        double progress = Math.min(100, (double) kills / requirements.getPlayerKills() * 100);
                        levelLore.add(ChatColor.RED + "Kills: " + createProgressBar(progress));
                    }
                    
                    if (requirements.getPlaytimeHours() > 0) {
                        long playtime = player.getStatistic(Statistic.PLAY_ONE_MINUTE) * 50L;
                        long playtimeHours = playtime / 3_600_000L; // Convert to hours
                        double progress = Math.min(100, (double) playtimeHours / requirements.getPlaytimeHours() * 100);
                        levelLore.add(ChatColor.BLUE + "Time: " + createProgressBar(progress));
                    }
                }
            }
        } else {
            levelLore.add(ChatColor.GOLD + "✦ MAX LEVEL ACHIEVED ✦");
        }
        
        levelMeta.setLore(levelLore);
        levelCard.setItemMeta(levelMeta);
        inventory.setItem(LEVEL_PROG_SLOT, levelCard);
    }
    
    private void addTierProgressCard() {
        int currentLevel = PlayerDataManager.getLevel(playerId);
        int currentTier = (currentLevel - 1) / 3 + 1;
        
        ItemStack tierCard = new ItemStack(Material.DIAMOND);
        ItemMeta tierMeta = tierCard.getItemMeta();
        tierMeta.setDisplayName(ChatColor.AQUA + "Tier Progress");
        
        List<String> tierLore = new ArrayList<>();
        tierLore.add(ChatColor.BLUE + "Current Tier: " + ChatColor.WHITE + currentTier + "/4");
        
        if (currentTier < 4) {
            int nextTierLevel = currentTier * 3;
            tierLore.add(ChatColor.YELLOW + "Next Tier at Level " + nextTierLevel);
            
            double progress = (double) (currentLevel - (currentTier - 1) * 3) / 3 * 100;
            tierLore.add(ChatColor.GREEN + "Progress: " + createProgressBar(progress));
            
            tierLore.add("");
            tierLore.add(ChatColor.GOLD + "Tier Benefits:");
            tierLore.add(ChatColor.WHITE + "• Access to tier " + (currentTier + 1) + " upgrades");
            tierLore.add(ChatColor.WHITE + "• Enhanced crystal effects");
            tierLore.add(ChatColor.WHITE + "• New crystal types");
        } else {
            tierLore.add(ChatColor.GOLD + "✦ MAXIMUM TIER REACHED ✦");
        }
        
        tierMeta.setLore(tierLore);
        tierCard.setItemMeta(tierMeta);
        inventory.setItem(TIER_PROG_SLOT, tierCard);
    }
    
    private void addUpgradeProgressCard() {
        Set<String> unlocked = PlayerDataManager.getUnlockedUpgrades(playerId);
        int totalUpgrades = getTotalUpgradeCount();
        double completionPercent = (double) unlocked.size() / totalUpgrades * 100;
        
        ItemStack upgradeCard = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta upgradeMeta = upgradeCard.getItemMeta();
        upgradeMeta.setDisplayName(ChatColor.AQUA + "Upgrade Progress");
        
        List<String> upgradeLore = new ArrayList<>();
        upgradeLore.add(ChatColor.BLUE + "Unlocked: " + ChatColor.WHITE + 
                       unlocked.size() + "/" + totalUpgrades);
        upgradeLore.add(ChatColor.YELLOW + "Completion: " + createProgressBar(completionPercent));
        
        // Breakdown by tier
        Map<Integer, Integer> unlockedByTier = new HashMap<>();
        Map<Integer, Integer> totalByTier = new HashMap<>();
        
        for (String upgradeId : unlocked) {
            int tier = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getTier(upgradeId);
            unlockedByTier.merge(tier, 1, Integer::sum);
        }
        
        for (int tier = 1; tier <= 3; tier++) {
            int unlockedCount = unlockedByTier.getOrDefault(tier, 0);
            int totalCount = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getUpgradesForTier(tier).size();
            totalByTier.put(tier, totalCount);
            
            double tierPercent = totalCount > 0 ? (double) unlockedCount / totalCount * 100 : 0;
            String tierName = tier == 1 ? "Basic" : tier == 2 ? "Advanced" : "Master";
            
            upgradeLore.add(ChatColor.WHITE + tierName + ": " + createProgressBar(tierPercent) + 
                           " " + unlockedCount + "/" + totalCount);
        }
        
        // Next affordable upgrade
        String nextUpgrade = findNextAffordableUpgrade(player);
        if (nextUpgrade != null) {
            upgradeLore.add("");
            upgradeLore.add(ChatColor.GREEN + "Next affordable: " + ChatColor.YELLOW + nextUpgrade);
        }
        
        upgradeMeta.setLore(upgradeLore);
        upgradeCard.setItemMeta(upgradeMeta);
        inventory.setItem(UPGRADE_PROG_SLOT, upgradeCard);
    }
    
    private void addAchievementProgressCard() {
        ItemStack achievementCard = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta achievementMeta = achievementCard.getItemMeta();
        achievementMeta.setDisplayName(ChatColor.GOLD + "Achievements");
        
        List<String> achievementLore = new ArrayList<>();
        achievementLore.add(ChatColor.GRAY + "Track your accomplishments");
        
        // TODO: Implement achievement system
        achievementLore.add(ChatColor.YELLOW + "Achievement system coming soon!");
        
        achievementMeta.setLore(achievementLore);
        achievementCard.setItemMeta(achievementMeta);
        inventory.setItem(ACHIEVEMENT_PROG_SLOT, achievementCard);
    }
    
    private void addStatisticsCard() {
        ItemStack statsCard = new ItemStack(Material.BOOK);
        ItemMeta statsMeta = statsCard.getItemMeta();
        statsMeta.setDisplayName(ChatColor.BLUE + "Statistics");
        
        List<String> statsLore = new ArrayList<>();
        statsLore.add(ChatColor.GRAY + "Your crystal usage stats");
        
        // Crystal stats
        int crystalsActivated = getStatistic(playerId, "crystals_activated");
        int crystalsRecharged = getStatistic(playerId, "crystals_recharged");
        int crystalsEnhanced = getStatistic(playerId, "crystals_enhanced");
        
        statsLore.add(ChatColor.AQUA + "Crystals Activated: " + ChatColor.WHITE + crystalsActivated);
        statsLore.add(ChatColor.AQUA + "Crystals Recharged: " + ChatColor.WHITE + crystalsRecharged);
        statsLore.add(ChatColor.AQUA + "Crystals Enhanced: " + ChatColor.WHITE + crystalsEnhanced);
        
        // Time stats
        long totalTime = player.getStatistic(Statistic.PLAY_ONE_MINUTE) * 50L;
        statsLore.add(ChatColor.AQUA + "Total Playtime: " + ChatColor.WHITE + formatTime(totalTime));
        
        statsMeta.setLore(statsLore);
        statsCard.setItemMeta(statsMeta);
        inventory.setItem(STATS_SLOT, statsCard);
    }
    
    private void addRequirementsBreakdown() {
        int currentLevel = PlayerDataManager.getLevel(playerId);
        int nextLevel = currentLevel + 1;
        
        ItemStack reqCard = new ItemStack(Material.PAPER);
        ItemMeta reqMeta = reqCard.getItemMeta();
        reqMeta.setDisplayName(ChatColor.YELLOW + "Level Requirements");
        
        List<String> reqLore = new ArrayList<>();
        if (nextLevel <= 10) {
            dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.LevelConfiguration nextConfig = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getLevelConfiguration(nextLevel);
            if (nextConfig != null) {
                reqLore.add(ChatColor.GRAY + "Requirements for Level " + nextLevel + ":");
                var requirements = nextConfig.getRequirements();
                
                if (requirements.getMoney() > 0) {
                    double balance = getPlayerBalance();
                    reqLore.add(ChatColor.GOLD + "Money: " + ChatColor.WHITE + 
                              String.format("$%.2f", balance) + "/" + 
                              String.format("$%.2f", requirements.getMoney()));
                }
                
                if (requirements.getPlayerKills() > 0) {
                    int kills = player.getStatistic(Statistic.PLAYER_KILLS);
                    reqLore.add(ChatColor.RED + "Kills: " + ChatColor.WHITE + 
                              kills + "/" + requirements.getPlayerKills());
                }
                
                if (requirements.getPlaytimeHours() > 0) {
                    long playtime = player.getStatistic(Statistic.PLAY_ONE_MINUTE) * 50L;
                    long playtimeHours = playtime / 3_600_000L;
                    reqLore.add(ChatColor.BLUE + "Playtime: " + ChatColor.WHITE + 
                              playtimeHours + "h/" + requirements.getPlaytimeHours() + "h");
                }
            }
        } else {
            reqLore.add(ChatColor.GOLD + "✦ MAX LEVEL REACHED ✦");
        }
        
        reqMeta.setLore(reqLore);
        reqCard.setItemMeta(reqMeta);
        inventory.setItem(REQUIREMENTS_SLOT, reqCard);
    }
    
    private void addNextGoals() {
        ItemStack goalsCard = new ItemStack(Material.COMPASS);
        ItemMeta goalsMeta = goalsCard.getItemMeta();
        goalsMeta.setDisplayName(ChatColor.GREEN + "Next Goals");
        
        List<String> goalsLore = new ArrayList<>();
        goalsLore.add(ChatColor.GRAY + "Recommended next steps:");
        
        // Generate personalized goals
        List<String> goals = generateGoals(player);
        for (String goal : goals) {
            goalsLore.add(ChatColor.YELLOW + "• " + goal);
        }
        
        goalsMeta.setLore(goalsLore);
        goalsCard.setItemMeta(goalsMeta);
        inventory.setItem(NEXT_GOALS_SLOT, goalsCard);
    }
    
    private void addRecommendations() {
        ItemStack recCard = new ItemStack(Material.TORCH);
        ItemMeta recMeta = recCard.getItemMeta();
        recMeta.setDisplayName(ChatColor.YELLOW + "Recommendations");
        
        List<String> recLore = new ArrayList<>();
        recLore.add(ChatColor.GRAY + "Suggested next steps:");
        
        // Generate personalized recommendations
        List<String> recommendations = generateRecommendations(player);
        for (int i = 0; i < Math.min(recommendations.size(), 5); i++) {
            recLore.add(ChatColor.AQUA + "• " + recommendations.get(i));
        }
        
        recMeta.setLore(recLore);
        recCard.setItemMeta(recMeta);
        inventory.setItem(RECOMMENDATIONS_SLOT, recCard);
    }
    
    private void addMilestones() {
        ItemStack milestoneCard = new ItemStack(Material.NETHER_STAR);
        ItemMeta milestoneMeta = milestoneCard.getItemMeta();
        milestoneMeta.setDisplayName(ChatColor.GOLD + "Milestones");
        
        List<String> milestoneLore = new ArrayList<>();
        milestoneLore.add(ChatColor.GRAY + "Track your major achievements");
        
        // TODO: Implement milestone system
        milestoneLore.add(ChatColor.YELLOW + "Milestone system coming soon!");
        
        milestoneMeta.setLore(milestoneLore);
        milestoneCard.setItemMeta(milestoneMeta);
        inventory.setItem(MILESTONES_SLOT, milestoneCard);
    }
    
    private void addServerRanking() {
        ItemStack rankCard = new ItemStack(Material.EMERALD);
        ItemMeta rankMeta = rankCard.getItemMeta();
        rankMeta.setDisplayName(ChatColor.GREEN + "Server Ranking");
        
        List<String> rankLore = new ArrayList<>();
        rankLore.add(ChatColor.GRAY + "Your position on the server");
        
        // TODO: Implement server ranking
        rankLore.add(ChatColor.YELLOW + "Ranking system coming soon!");
        
        rankMeta.setLore(rankLore);
        rankCard.setItemMeta(rankMeta);
        inventory.setItem(SERVER_RANK_SLOT, rankCard);
    }
    
    private void addRecentProgress() {
        ItemStack progressCard = new ItemStack(Material.CLOCK);
        ItemMeta progressMeta = progressCard.getItemMeta();
        progressMeta.setDisplayName(ChatColor.AQUA + "Recent Progress");
        
        List<String> progressLore = new ArrayList<>();
        progressLore.add(ChatColor.GRAY + "Your recent achievements");
        
        // TODO: Implement recent progress tracking
        progressLore.add(ChatColor.YELLOW + "Progress tracking coming soon!");
        
        progressMeta.setLore(progressLore);
        progressCard.setItemMeta(progressMeta);
        inventory.setItem(RECENT_PROG_SLOT, progressCard);
    }
    
    private void addGoalTracking() {
        ItemStack goalsCard = new ItemStack(Material.BOOK);
        ItemMeta goalsMeta = goalsCard.getItemMeta();
        goalsMeta.setDisplayName(ChatColor.BLUE + "Goal Tracking");
        
        List<String> goalsLore = new ArrayList<>();
        goalsLore.add(ChatColor.GRAY + "Track your personal goals");
        
        // TODO: Implement goal tracking
        goalsLore.add(ChatColor.YELLOW + "Goal tracking coming soon!");
        
        goalsMeta.setLore(goalsLore);
        goalsCard.setItemMeta(goalsMeta);
        inventory.setItem(GOALS_SLOT, goalsCard);
    }
    
    private void addUtilityButtons() {
        // Help button
        ItemStack helpBtn = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta helpMeta = helpBtn.getItemMeta();
        helpMeta.setDisplayName(ChatColor.YELLOW + "Help");
        helpMeta.setLore(List.of(ChatColor.GRAY + "View help information"));
        helpBtn.setItemMeta(helpMeta);
        inventory.setItem(HELP_SLOT, helpBtn);
        
        // Refresh button
        ItemStack refreshBtn = new ItemStack(Material.COMPARATOR);
        ItemMeta refreshMeta = refreshBtn.getItemMeta();
        refreshMeta.setDisplayName(ChatColor.AQUA + "Refresh");
        refreshMeta.setLore(List.of(ChatColor.GRAY + "Update dashboard data"));
        refreshBtn.setItemMeta(refreshMeta);
        inventory.setItem(REFRESH_SLOT, refreshBtn);
        
        // Back button
        ItemStack backBtn = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backBtn.getItemMeta();
        backMeta.setDisplayName(ChatColor.YELLOW + "Back to Main Menu");
        backBtn.setItemMeta(backMeta);
        inventory.setItem(BACK_SLOT, backBtn);
        
        // Close button
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Close Menu");
        closeBtn.setItemMeta(closeMeta);
        inventory.setItem(CLOSE_SLOT, closeBtn);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);
        
        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        switch (slot) {
            case HELP_SLOT -> {
                clicker.closeInventory();
                new HelpSystemGUI(clicker).open();
            }
            case REFRESH_SLOT -> {
                buildDashboard();
                clicker.sendMessage(ChatColor.GREEN + "Dashboard refreshed!");
            }
            case BACK_SLOT -> {
                clicker.closeInventory();
                new ArcaniteMainMenu(clicker).open();
            }
            case CLOSE_SLOT -> clicker.closeInventory();
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this && isOpen) {
            isOpen = false;
            HandlerList.unregisterAll(this);
        }
    }
    
    public void open() {
        if (!isOpen) {
            player.openInventory(inventory);
            isOpen = true;
            Bukkit.getPluginManager().registerEvents(this, ArcaniteCrystals.getInstance());
        }
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    // Utility methods
    private String createProgressBar(double percent) {
        int bars = 10;
        int filled = (int) (percent / 100.0 * bars);
        
        StringBuilder bar = new StringBuilder();
        ChatColor color = percent >= 80 ? ChatColor.GREEN :
                         percent >= 50 ? ChatColor.YELLOW : ChatColor.RED;
        
        bar.append(color).append("[");
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append(ChatColor.DARK_GRAY).append("░").append(color);
            }
        }
        bar.append("]").append(ChatColor.WHITE).append(" ").append(String.format("%.1f%%", percent));
        
        return bar.toString();
    }
    
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return days + "d " + (hours % 24) + "h";
        if (hours > 0) return hours + "h " + (minutes % 60) + "m";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }
    
    private double getPlayerBalance() {
        if (ArcaniteCrystals.getEconomy() == null) return 0;
        return ArcaniteCrystals.getEconomy().getBalance(player);
    }
    
    private int getTotalUpgradeCount() {
        // TODO: Implement upgrade count
        return 0;
    }
    
    private String findNextAffordableUpgrade(Player player) {
        // TODO: Implement upgrade finding
        return null;
    }
    
    private int getStatistic(UUID playerId, String stat) {
        // TODO: Implement statistics tracking
        return 0;
    }
    
    private List<String> generateGoals(Player player) {
        List<String> goals = new ArrayList<>();
        
        int level = PlayerDataManager.getLevel(playerId);
        Set<String> upgrades = PlayerDataManager.getUnlockedUpgrades(playerId);
        double balance = getPlayerBalance();
        
        // Level-based goals
        if (level < 3) {
            goals.add("Reach level 3 to unlock tier 2 upgrades");
        } else if (level < 5) {
            goals.add("Reach level 5 to unlock tier 3 upgrades");
        } else if (level < 10) {
            goals.add("Reach level 10 for maximum tier");
        }
        
        // Upgrade goals
        if (upgrades.size() < 5) {
            goals.add("Purchase 5 basic upgrades");
        } else if (upgrades.size() < 10) {
            goals.add("Purchase 10 upgrades total");
        }
        
        // Economy goals
        if (balance < 10000) {
            goals.add("Save up $10,000 for upgrades");
        } else if (balance < 50000) {
            goals.add("Save up $50,000 for tier 3 upgrades");
        }
        
        return goals;
    }
    
    private List<String> generateRecommendations(Player player) {
        List<String> recommendations = new ArrayList<>();
        
        int level = PlayerDataManager.getLevel(playerId);
        Set<String> upgrades = PlayerDataManager.getUnlockedUpgrades(playerId);
        double balance = getPlayerBalance();
        
        // Level-based recommendations
        if (level < 3 && balance < 10000) {
            recommendations.add("Focus on earning money for early upgrades");
        } else if (level < 5 && upgrades.size() < 5) {
            recommendations.add("Purchase basic tier upgrades to unlock tier 2");
        } else if (level < 7) {
            recommendations.add("Work towards tier 2 upgrades for better effects");
        }
        
        // Crystal usage recommendations
        int crystalCount = countPlayerCrystals(player);
        if (crystalCount == 0) {
            recommendations.add("Get your first crystal with /mysterycrystal");
        } else if (crystalCount < 3) {
            recommendations.add("Collect more crystals for different situations");
        }
        
        // Economy recommendations
        if (balance > 100000 && upgrades.size() < 10) {
            recommendations.add("You have enough money for several upgrades");
        }
        
        // Achievement recommendations
        recommendations.add("Check achievements menu for bonus goals");
        
        return recommendations;
    }
    
    private int countPlayerCrystals(Player player) {
        // TODO: Implement crystal counting
        return 0;
    }
} 