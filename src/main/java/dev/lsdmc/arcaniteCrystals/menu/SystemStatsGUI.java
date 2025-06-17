package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.database.SqliteDataManager;
import dev.lsdmc.arcaniteCrystals.manager.EffectApplierManager;
import dev.lsdmc.arcaniteCrystals.util.GUIUtils;
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

/**
 * Shows real-time system statistics (effect manager, database, memory).
 */
public class SystemStatsGUI implements InventoryHolder, Listener {

    private final Player viewer;
    private final Inventory inv;
    private boolean open = false;

    public SystemStatsGUI(Player viewer) {
        this.viewer = viewer;
        this.inv = Bukkit.createInventory(this, 27, ChatColor.GREEN + "✦ System Stats ✦");
        build();
    }

    private void build() {
        GUIUtils.fillInventory(inv, Material.GRAY_STAINED_GLASS_PANE, " ");

        // Effect manager stats
        ItemStack effect = GUIUtils.createNavIcon(Material.NETHER_STAR, ChatColor.AQUA + "Effect Manager");
        ItemMeta em = effect.getItemMeta();
        em.setLore(java.util.List.of(ChatColor.GRAY + EffectApplierManager.getStats()));
        effect.setItemMeta(em);
        inv.setItem(10, effect);

        // Database stats
        ItemStack db = GUIUtils.createNavIcon(Material.BOOK, ChatColor.AQUA + "Database");
        ItemMeta dm = db.getItemMeta();
        dm.setLore(java.util.List.of(ChatColor.GRAY + SqliteDataManager.getStats()));
        db.setItemMeta(dm);
        inv.setItem(12, db);

        // Memory stats
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        long total = rt.totalMemory();
        double pct = (double) used / total * 100;
        ItemStack mem = GUIUtils.createNavIcon(Material.REDSTONE, ChatColor.AQUA + "Memory");
        ItemMeta mm = mem.getItemMeta();
        mm.setLore(java.util.List.of(ChatColor.GRAY + String.format("%.1f%% used (%.1f/%.1f MB)", pct, used / 1048576.0, total / 1048576.0)));
        mem.setItemMeta(mm);
        inv.setItem(14, mem);

        inv.setItem(26, GUIUtils.createNavIcon(Material.BARRIER, ChatColor.RED + "Close"));
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
        if (e.getRawSlot() == 26) {
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