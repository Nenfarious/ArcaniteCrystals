package dev.lsdmc.arcaniteCrystals.menu;

import dev.lsdmc.arcaniteCrystals.ArcaniteCrystals;
import dev.lsdmc.arcaniteCrystals.database.PlayerDataManager;
import dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager;
import dev.lsdmc.arcaniteCrystals.util.GUIUtils;
import dev.lsdmc.arcaniteCrystals.util.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Simple admin GUI to manage players – displays online players with level & upgrade info.
 */
public class PlayerManagementGUI implements InventoryHolder, Listener {

    private static final int SIZE = 54;
    private static final int CLOSE_SLOT = 49;

    private final Player admin;
    private final Inventory inv;
    private boolean open = false;

    public PlayerManagementGUI(Player admin) {
        this.admin = admin;
        this.inv = Bukkit.createInventory(this, SIZE, ChatColor.DARK_RED + "✦ Player Management ✦");
        build();
    }

    private void build() {
        // Fill background
        GUIUtils.fillInventory(inv, Material.GRAY_STAINED_GLASS_PANE, " ");

        List<Player> players = Bukkit.getOnlinePlayers().stream().collect(Collectors.toList());
        int slot = 10;
        for (Player p : players) {
            if (slot >= 44) break; // limit to first page
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(p);
            meta.setDisplayName(ChatColor.AQUA + p.getName());

            int lvl = PlayerDataManager.getLevel(p.getUniqueId());
            int upgrades = PlayerDataManager.getUnlockedUpgrades(p.getUniqueId()).size();
            int tier = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getMaxTier(p.getUniqueId());

            meta.setLore(List.of(
                    ChatColor.GRAY + "Level: " + ChatColor.GOLD + lvl,
                    ChatColor.GRAY + "Tier: " + ChatColor.GOLD + tier,
                    ChatColor.GRAY + "Upgrades: " + ChatColor.GOLD + upgrades,
                    "",
                    ChatColor.YELLOW + "Left-Click » View Info",
                    ChatColor.YELLOW + "Right-Click » Set Level"
            ));
            head.setItemMeta(meta);
            inv.setItem(slot, head);
            // advance slot skipping borders columns 0 & 8
            slot = (slot % 9 == 7) ? slot + 3 : slot + 1;
        }

        // Close button
        ItemStack close = GUIUtils.createNavIcon(Material.BARRIER, ChatColor.RED + "Close");
        inv.setItem(CLOSE_SLOT, close);
    }

    public void open() {
        if (!open) {
            admin.openInventory(inv);
            open = true;
            Bukkit.getPluginManager().registerEvents(this, ArcaniteCrystals.getInstance());
        }
    }

    @EventHandler
    public void click(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player viewer)) return;

        int slot = e.getRawSlot();
        if (slot == CLOSE_SLOT) {
            viewer.closeInventory();
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        if (!(clicked.getItemMeta() instanceof SkullMeta meta)) return;
        OfflinePlayer target = meta.getOwningPlayer();
        if (target == null) return;
        UUID targetId = target.getUniqueId();

        if (e.isRightClick()) {
            // Prompt set level (simple – sets next level in cycle 1-10)
            int current = PlayerDataManager.getLevel(targetId);
            int next = current >= 10 ? 1 : current + 1;
            PlayerDataManager.setLevel(targetId, next);
            viewer.sendMessage(ChatColor.GREEN + "Set " + target.getName() + " to level " + next);
        } else {
            // Show info
            int lvl = PlayerDataManager.getLevel(targetId);
            int upgrades = PlayerDataManager.getUnlockedUpgrades(targetId).size();
            int tier = dev.lsdmc.arcaniteCrystals.manager.ServerLevelManager.getMaxTier(targetId);
            viewer.sendMessage(ChatColor.GOLD + "=== Info for " + target.getName() + " ===");
            viewer.sendMessage(ChatColor.GRAY + "Level: " + ChatColor.YELLOW + lvl);
            viewer.sendMessage(ChatColor.GRAY + "Tier: " + ChatColor.YELLOW + tier);
            viewer.sendMessage(ChatColor.GRAY + "Upgrades: " + ChatColor.YELLOW + upgrades);
        }
        // refresh after short delay
        new BukkitRunnable() {
            @Override public void run() { build(); }
        }.runTaskLater(ArcaniteCrystals.getInstance(), 2L);
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