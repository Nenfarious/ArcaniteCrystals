package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.config.ConfigManager;
import dev.lsdmc.arcaniteCrystals.util.GUIUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only GUI displaying every level, its requirements and perks.
 */
public class LevelOverviewGUI implements InventoryHolder, Listener {

    private static final int SIZE = 54;
    private final Player viewer;
    private final Inventory inv;
    private boolean open = false;

    public LevelOverviewGUI(Player viewer) {
        this.viewer = viewer;
        this.inv = Bukkit.createInventory(this, SIZE, ChatColor.GOLD + "✦ Level Progression ✦");
        build();
    }

    private void build() {
        GUIUtils.fillInventory(inv, Material.BLACK_STAINED_GLASS_PANE, " ");

        int rowSlot = 10; // start slot row 1 col 1
        for (int lvl = 1; lvl <= 10; lvl++) {
            dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.LevelConfiguration cfg = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getLevelConfiguration(lvl);
            if (cfg == null) continue;
            Material mat = lvl <= 3 ? Material.IRON_INGOT : lvl <= 6 ? Material.GOLD_INGOT : Material.DIAMOND;
            ItemStack icon = new ItemStack(mat);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + "Level " + lvl);

            List<String> lore = new ArrayList<>();
            // Requirements
            lore.add(ChatColor.YELLOW + "Requirements:");
            var requirements = cfg.getRequirements();
            if (requirements.getMoney() > 0) lore.add(ChatColor.GRAY + "• $" + String.format("%.2f", requirements.getMoney()));
            if (requirements.getPlayerKills() > 0) lore.add(ChatColor.GRAY + "• " + requirements.getPlayerKills() + " player kills");
            if (requirements.getPlaytimeHours() > 0) {
                lore.add(ChatColor.GRAY + "• " + requirements.getPlaytimeHours() + " hours playtime");
            }
            // Perks
            lore.add("");
            lore.add(ChatColor.YELLOW + "Perks:");
            // Get tier and slots from levels.yml directly for this level
            var levelsConfig = ConfigManager.getLevelsConfig();
            ConfigurationSection levelSection = levelsConfig.getConfigurationSection("level-" + lvl);
            int tier = levelSection != null ? levelSection.getInt("tier", 1) : 1;
            int slots = levelSection != null ? levelSection.getInt("crystal-slots", 1) : 1;
            lore.add(ChatColor.GRAY + "• Tier " + tier + " effects");
            lore.add(ChatColor.GRAY + "• Crystal slots: " + slots);
            cfg.getBuffs().forEach((stat, val) -> lore.add(ChatColor.GRAY + "• +" + val + " " + stat.replace('_',' ')));

            meta.setLore(lore);
            icon.setItemMeta(meta);
            inv.setItem(rowSlot, icon);
            rowSlot = (rowSlot % 9 == 7) ? rowSlot + 3 : rowSlot + 1;
        }

        inv.setItem(49, GUIUtils.createNavIcon(Material.BARRIER, ChatColor.RED + "Close"));
    }

    public void open() {
        if (!open) {
            viewer.openInventory(inv);
            open = true;
            Bukkit.getPluginManager().registerEvents(this, ArcaniteCrystals.getInstance());
        }
    }

    @EventHandler
    public void click(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;
        e.setCancelled(true);
        if (e.getRawSlot() == 49) {
            e.getWhoClicked().closeInventory();
        }
    }

    @EventHandler
    public void close(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() == this && open) {
            open = false;
            HandlerList.unregisterAll(this);
        }
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
} 