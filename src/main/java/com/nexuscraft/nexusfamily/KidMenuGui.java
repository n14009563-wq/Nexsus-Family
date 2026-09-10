package com.nexuscraft.nexusfamily;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Builds and opens the two GUIs a parent (or admin) uses to manage a kid: a small button menu,
 * and the kid's actual item storage. Both are plain, undecorated chest inventories -- identified
 * to the click/close handlers by their InventoryHolder, not by title text or item tags. */
public final class KidMenuGui {

    public static final int MENU_SIZE = 9;
    public static final int STORAGE_SIZE = 27;
    public static final int SLOT_INVENTORY = 2;
    public static final int SLOT_INFO = 6;

    private KidMenuGui() {
    }

    public static void openMenu(Player player, Villager kid, KidManager kidManager) {
        KidMenuHolder holder = new KidMenuHolder(kid.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, MENU_SIZE, "Manage " + kidManager.getName(kid));
        holder.setInventory(inventory);

        inventory.setItem(SLOT_INVENTORY, button(Material.matchMaterial("CHEST"), "&aInventory",
                "&7Give " + kidManager.getName(kid) + " armor, tools, or items."));
        inventory.setItem(SLOT_INFO, button(Material.matchMaterial("PAPER"), "&bInfo",
                "&7See " + kidManager.getName(kid) + "'s parents and growth status."));

        player.openInventory(inventory);
    }

    public static void openStorage(Player player, Villager kid, KidManager kidManager) {
        KidStorageHolder holder = new KidStorageHolder(kid.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, STORAGE_SIZE, kidManager.getName(kid) + "'s Inventory");
        holder.setInventory(inventory);
        inventory.setContents(kidManager.getInventoryContents(kid, STORAGE_SIZE));
        player.openInventory(inventory);
    }

    private static ItemStack button(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material != null ? material : Material.matchMaterial("PAPER"), 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            meta.setLore(java.util.List.of(ChatColor.translateAlternateColorCodes('&', lore)));
            item.setItemMeta(meta);
        }
        return item;
    }
}
