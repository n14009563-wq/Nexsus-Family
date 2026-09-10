package com.nexuscraft.nexusfamily;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

/** Optional shapeless recipe for the Wedding Ring, entirely config-driven. */
public final class RingRecipeRegistrar {

    private final JavaPlugin plugin;
    private final RingItem ringItem;
    private final NamespacedKey recipeKey;

    public RingRecipeRegistrar(JavaPlugin plugin, RingItem ringItem) {
        this.plugin = plugin;
        this.ringItem = ringItem;
        this.recipeKey = new NamespacedKey(plugin, "wedding_ring");
    }

    public void register() {
        Bukkit.removeRecipe(recipeKey); // safe even if never registered -- lets /reload re-apply changed ingredients

        if (!plugin.getConfig().getBoolean("marriage.ring.recipe.enabled", true)) {
            return;
        }

        List<String> ingredientNames = plugin.getConfig().getStringList("marriage.ring.recipe.ingredients");
        if (ingredientNames.isEmpty()) {
            plugin.getLogger().warning("[NexusFamily] marriage.ring.recipe.enabled is true but ingredients is empty -- skipping recipe registration.");
            return;
        }

        ItemStack result = ringItem.create(1);
        ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, result);
        for (String name : ingredientNames) {
            Material material = Material.matchMaterial(name);
            if (material == null) {
                plugin.getLogger().log(Level.WARNING, "[NexusFamily] Unknown material \"" + name + "\" in ring recipe ingredients -- skipped it.");
                continue;
            }
            recipe.addIngredient(material);
        }

        Bukkit.addRecipe(recipe);
    }
}
