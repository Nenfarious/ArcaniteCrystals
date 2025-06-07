// src/main/java/dev/lsdmc/arcaniteCrystals/util/GUIUtils.java
package dev.lsdmc.arcaniteCrystals.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Utility methods for building and filling GUI inventories.
 */
public class GUIUtils {

    /**
     * Fills the given inventory with a uniform filler item.
     *
     * @param inv         the inventory to fill
     * @param fillerMat   the material to use as filler
     * @param fillerName  the display name for the filler (color codes allowed)
     */
    public static void fillInventory(Inventory inv, Material fillerMat, String fillerName) {
        ItemStack filler = new ItemStack(fillerMat);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', fillerName));
        filler.setItemMeta(meta);

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    /**
     * Creates a simple navigation icon with the given material and name.
     *
     * @param mat  the material for the icon
     * @param name the display name (color codes allowed)
     * @return the built ItemStack
     */
    public static ItemStack createNavIcon(Material mat, String name) {
        ItemStack icon = new ItemStack(mat);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        icon.setItemMeta(meta);
        return icon;
    }
}
