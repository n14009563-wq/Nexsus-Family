package com.nexuscraft.nexusfamily;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds and identifies the Wedding Ring item. Identified by a hidden PersistentDataContainer
 * marker (not display name), same approach as the Truce Crystal in NexusWarbeasts, so it can't
 * be faked by renaming another item in an anvil.
 */
public final class RingItem {

    private final JavaPlugin plugin;
    private final NamespacedKey markerKey;

    private Material material = Material.matchMaterial("GOLD_NUGGET");
    private String displayName = "&6Wedding Ring";
    private List<String> lore = new ArrayList<>();

    public RingItem(JavaPlugin plugin) {
        this.plugin = plugin;
        this.markerKey = new NamespacedKey(plugin, "wedding_ring");
    }

    public void loadFromConfig() {
        String materialName = plugin.getConfig().getString("marriage.ring.material", "GOLD_NUGGET");
        Material parsed = Material.matchMaterial(materialName);
        this.material = parsed != null ? parsed : Material.matchMaterial("GOLD_NUGGET");
        this.displayName = plugin.getConfig().getString("marriage.ring.display-name", "&6Wedding Ring");
        this.lore = plugin.getConfig().getStringList("marriage.ring.lore");
    }

    public ItemStack create(int amount) {
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(coloredLore);
            meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** True only for an item this plugin actually created -- checked via PDC, never display name. */
    public boolean isRing(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte marker = meta.getPersistentDataContainer().get(markerKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }
}
