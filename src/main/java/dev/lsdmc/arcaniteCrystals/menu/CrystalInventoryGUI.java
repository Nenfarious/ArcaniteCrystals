package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.manager.CrystalManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CrystalInventoryGUI implements InventoryHolder, Listener {
    private static final int FILTER_SLOT = 0;
    private static final int SORT_SLOT = 1;
    private static final int SEARCH_SLOT = 2;
    private static final int CRYSTAL_START = 9;
    private static final int CRYSTAL_END = 44;
    private static final int CRYSTALS_PER_PAGE = 36;
    
    private enum CrystalFilter {
        ALL("All Crystals"),
        ACTIVE("Active"),
        DEPLETED("Depleted"), 
        ENHANCED("Enhanced"),
        BASIC("Basic"),
        MASTER("Master"),
        BLANK("Unidentified");
        
        final String displayName;
        CrystalFilter(String displayName) { this.displayName = displayName; }
    }
    
    private enum SortMode {
        NAME("By Name"),
        ENERGY("By Energy"),
        TIER("By Tier"),
        RECENT("Recently Used");
        
        final String displayName;
        SortMode(String displayName) { this.displayName = displayName; }
    }
    
    private final Player player;
    private final Inventory inventory;
    private boolean isOpen = false;
    
    private CrystalFilter currentFilter = CrystalFilter.ALL;
    private SortMode currentSort = SortMode.NAME;
    private int currentPage = 0;
    private String searchQuery = "";
    
    private final Map<Integer, Long> lastUsedTimestamps = new ConcurrentHashMap<>();
    
    public CrystalInventoryGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, 
            ChatColor.DARK_PURPLE + "✦ Crystal Inventory ✦");
        buildCrystalInventory();
    }
    
    private void buildCrystalInventory() {
        // Add header
        ItemStack header = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta headerMeta = header.getItemMeta();
        headerMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Crystal Collection");
        
        List<String> headerLore = new ArrayList<>();
        headerLore.add(ChatColor.GRAY + "Manage your crystal collection");
        headerLore.add(ChatColor.GRAY + "and activate their effects");
        headerMeta.setLore(headerLore);
        header.setItemMeta(headerMeta);
        inventory.setItem(4, header);
        
        // Add filter and sort controls
        addFilterButton();
        addSortButton();
        addSearchButton();
        
        // Add navigation buttons
        addNavigationButtons();
        
        // Display crystals
        displayCrystals();
    }
    
    private void addFilterButton() {
        ItemStack filterBtn = new ItemStack(Material.HOPPER);
        ItemMeta filterMeta = filterBtn.getItemMeta();
        filterMeta.setDisplayName(ChatColor.AQUA + "Filter Crystals");
        
        List<String> filterLore = new ArrayList<>();
        filterLore.add(ChatColor.GRAY + "Current Filter: " + ChatColor.YELLOW + currentFilter.displayName);
        filterLore.add("");
        filterLore.add(ChatColor.GRAY + "Available Filters:");
        for (CrystalFilter filter : CrystalFilter.values()) {
            String prefix = filter == currentFilter ? ChatColor.GREEN + "► " : ChatColor.GRAY + "  ";
            filterLore.add(prefix + filter.displayName);
        }
        filterLore.add("");
        filterLore.add(ChatColor.YELLOW + "Click to cycle filters");
        
        filterMeta.setLore(filterLore);
        filterBtn.setItemMeta(filterMeta);
        inventory.setItem(FILTER_SLOT, filterBtn);
    }
    
    private void addSortButton() {
        ItemStack sortBtn = new ItemStack(Material.COMPARATOR);
        ItemMeta sortMeta = sortBtn.getItemMeta();
        sortMeta.setDisplayName(ChatColor.AQUA + "Sort Crystals");
        
        List<String> sortLore = new ArrayList<>();
        sortLore.add(ChatColor.GRAY + "Current Sort: " + ChatColor.YELLOW + currentSort.displayName);
        sortLore.add("");
        sortLore.add(ChatColor.GRAY + "Available Sorts:");
        for (SortMode sort : SortMode.values()) {
            String prefix = sort == currentSort ? ChatColor.GREEN + "► " : ChatColor.GRAY + "  ";
            sortLore.add(prefix + sort.displayName);
        }
        sortLore.add("");
        sortLore.add(ChatColor.YELLOW + "Click to cycle sort modes");
        
        sortMeta.setLore(sortLore);
        sortBtn.setItemMeta(sortMeta);
        inventory.setItem(SORT_SLOT, sortBtn);
    }
    
    private void addSearchButton() {
        ItemStack searchBtn = new ItemStack(Material.NAME_TAG);
        ItemMeta searchMeta = searchBtn.getItemMeta();
        searchMeta.setDisplayName(ChatColor.AQUA + "Search Crystals");
        
        List<String> searchLore = new ArrayList<>();
        if (searchQuery.isEmpty()) {
            searchLore.add(ChatColor.GRAY + "No active search");
        } else {
            searchLore.add(ChatColor.GRAY + "Searching for: " + ChatColor.YELLOW + searchQuery);
        }
        searchLore.add("");
        searchLore.add(ChatColor.YELLOW + "Click to search");
        searchLore.add(ChatColor.GRAY + "Type in chat to search");
        
        searchMeta.setLore(searchLore);
        searchBtn.setItemMeta(searchMeta);
        inventory.setItem(SEARCH_SLOT, searchBtn);
    }
    
    private void addNavigationButtons() {
        // Previous page
        if (currentPage > 0) {
            ItemStack prevBtn = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevBtn.getItemMeta();
            prevMeta.setDisplayName(ChatColor.YELLOW + "Previous Page");
            prevBtn.setItemMeta(prevMeta);
            inventory.setItem(45, prevBtn);
        }
        
        // Next page
        ItemStack nextBtn = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextBtn.getItemMeta();
        nextMeta.setDisplayName(ChatColor.YELLOW + "Next Page");
        nextBtn.setItemMeta(nextMeta);
        inventory.setItem(53, nextBtn);
    }
    
    private void displayCrystals() {
        // Get player's crystals
        List<ItemStack> crystals = findPlayerCrystals(player);
        
        // Apply filters
        crystals = applyCrystalFilter(crystals);
        
        // Apply search
        if (!searchQuery.isEmpty()) {
            crystals.removeIf(crystal -> {
                String name = crystal.getItemMeta().getDisplayName().toLowerCase();
                return !name.contains(searchQuery.toLowerCase());
            });
        }
        
        // Sort crystals
        sortCrystals(crystals);
        
        // Calculate pagination
        int totalPages = (int) Math.ceil(crystals.size() / (double) CRYSTALS_PER_PAGE);
        if (currentPage >= totalPages) currentPage = Math.max(0, totalPages - 1);
        
        // Display current page
        int startIndex = currentPage * CRYSTALS_PER_PAGE;
        int endIndex = Math.min(startIndex + CRYSTALS_PER_PAGE, crystals.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            ItemStack crystal = crystals.get(i);
            int slot = CRYSTAL_START + (i - startIndex);
            inventory.setItem(slot, createCrystalDisplayItem(crystal));
        }
    }
    
    private List<ItemStack> findPlayerCrystals(Player player) {
        List<ItemStack> crystals = new ArrayList<>();
        
        // Check inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && CrystalManager.isCrystal(item)) {
                crystals.add(item);
            }
        }
        
        // Check ender chest
        for (ItemStack item : player.getEnderChest().getContents()) {
            if (item != null && CrystalManager.isCrystal(item)) {
                crystals.add(item);
            }
        }
        
        return crystals;
    }
    
    private List<ItemStack> applyCrystalFilter(List<ItemStack> crystals) {
        return crystals.stream()
            .filter(crystal -> {
                switch (currentFilter) {
                    case ALL:
                        return true;
                    case ACTIVE:
                        return CrystalManager.isActivatedCrystal(crystal);
                    case DEPLETED:
                        return CrystalManager.getEnergy(crystal) <= 0;
                    case ENHANCED:
                        return CrystalManager.getCrystalType(crystal) == CrystalManager.CrystalType.ENHANCED;
                    case BASIC:
                        return CrystalManager.getCrystalType(crystal) == CrystalManager.CrystalType.BASIC;
                    case MASTER:
                        return CrystalManager.getCrystalType(crystal) == CrystalManager.CrystalType.MASTER;
                    case BLANK:
                        return !CrystalManager.isIdentified(crystal);
                    default:
                        return true;
                }
            })
            .collect(Collectors.toList());
    }
    
    private void sortCrystals(List<ItemStack> crystals) {
        crystals.sort((c1, c2) -> {
            switch (currentSort) {
                case NAME:
                    return c1.getItemMeta().getDisplayName()
                        .compareTo(c2.getItemMeta().getDisplayName());
                case ENERGY:
                    return Integer.compare(
                        CrystalManager.getEnergy(c2),
                        CrystalManager.getEnergy(c1)
                    );
                case TIER:
                    return Integer.compare(
                        CrystalManager.getCrystalType(c2).ordinal(),
                        CrystalManager.getCrystalType(c1).ordinal()
                    );
                case RECENT:
                    Long t1 = lastUsedTimestamps.get(c1.hashCode());
                    Long t2 = lastUsedTimestamps.get(c2.hashCode());
                    if (t1 == null && t2 == null) return 0;
                    if (t1 == null) return 1;
                    if (t2 == null) return -1;
                    return t2.compareTo(t1);
                default:
                    return 0;
            }
        });
    }
    
    private ItemStack createCrystalDisplayItem(ItemStack crystal) {
        ItemStack display = crystal.clone();
        ItemMeta meta = display.getItemMeta();
        List<String> lore = new ArrayList<>();
        
        // Add crystal type info
        CrystalManager.CrystalType type = CrystalManager.getCrystalType(crystal);
        lore.add(ChatColor.GRAY + "Type: " + type.getDisplayName());
        
        // Add energy info
        int energy = CrystalManager.getEnergy(crystal);
        int maxEnergy = CrystalManager.getMaxEnergy(crystal);
        double energyPercent = (double) energy / maxEnergy * 100;
        ChatColor energyColor = energyPercent > 75 ? ChatColor.GREEN :
                               energyPercent > 25 ? ChatColor.YELLOW : ChatColor.RED;
        lore.add(ChatColor.GRAY + "Energy: " + energyColor + energy + "/" + maxEnergy);
        
        // Add effects info
        if (CrystalManager.isIdentified(crystal)) {
            List<String> effects = CrystalManager.getCrystalEffects(crystal);
            lore.add("");
            lore.add(ChatColor.LIGHT_PURPLE + "Effects:");
            for (String effect : effects) {
                lore.add(ChatColor.GRAY + "• " + CrystalManager.beautifyEffectName(effect));
            }
        } else {
            lore.add("");
            lore.add(ChatColor.YELLOW + "Unidentified Crystal");
            lore.add(ChatColor.GRAY + "Right-click to identify");
        }
        
        // Add last used info
        Long lastUsed = lastUsedTimestamps.get(crystal.hashCode());
        if (lastUsed != null) {
            lore.add("");
            lore.add(ChatColor.GRAY + "Last Used: " + formatTimeAgo(lastUsed));
        }
        
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);
        
        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        if (slot >= CRYSTAL_START && slot <= CRYSTAL_END) {
            ItemStack clickedCrystal = event.getCurrentItem();
            if (clickedCrystal != null && CrystalManager.isCrystal(clickedCrystal)) {
                if (event.isShiftClick()) {
                    // Quick equip to off-hand
                    equipCrystal(clicker, clickedCrystal);
                } else {
                    // Open crystal options menu
                    new CrystalOptionsGUI(clicker, clickedCrystal).open();
                }
            }
        } else if (slot == FILTER_SLOT) {
            cycleFilter();
            buildCrystalInventory();
        } else if (slot == SORT_SLOT) {
            cycleSort();
            buildCrystalInventory();
        } else if (slot == SEARCH_SLOT) {
            // TODO: Implement search functionality
            clicker.sendMessage(ChatColor.YELLOW + "Search functionality coming soon!");
        } else if (slot == 45 && currentPage > 0) {
            currentPage--;
            buildCrystalInventory();
        } else if (slot == 53) {
            currentPage++;
            buildCrystalInventory();
        }
    }
    
    private void cycleFilter() {
        CrystalFilter[] filters = CrystalFilter.values();
        int currentIndex = Arrays.asList(filters).indexOf(currentFilter);
        currentFilter = filters[(currentIndex + 1) % filters.length];
    }
    
    private void cycleSort() {
        SortMode[] sorts = SortMode.values();
        int currentIndex = Arrays.asList(sorts).indexOf(currentSort);
        currentSort = sorts[(currentIndex + 1) % sorts.length];
    }
    
    private void equipCrystal(Player player, ItemStack crystal) {
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && !offHand.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "Your off-hand must be empty to equip a crystal!");
            return;
        }
        
        player.getInventory().setItemInOffHand(crystal);
        player.sendMessage(ChatColor.GREEN + "Crystal equipped to off-hand!");
        
        // Update last used timestamp
        lastUsedTimestamps.put(crystal.hashCode(), System.currentTimeMillis());
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
    
    private String formatTimeAgo(long milliseconds) {
        long seconds = milliseconds / 1000;
        if (seconds < 60) return seconds + "s ago";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        return days + "d ago";
    }
} 