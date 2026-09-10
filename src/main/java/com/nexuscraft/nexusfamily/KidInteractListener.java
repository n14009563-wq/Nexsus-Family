package com.nexuscraft.nexusfamily;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Right-clicking a kid always cancels vanilla's own trade-GUI attempt (a kid isn't a real trader)
 * and gives a quick greeting instead. Shift-right-clicking, if you're a parent or an admin, opens
 * the management menu (Inventory/Info) instead of a greeting.
 */
public final class KidInteractListener implements Listener {

    private final JavaPlugin plugin;
    private final KidManager kidManager;
    private final KidRegistry kidRegistry;
    private final DialogueEngine dialogueEngine;

    public KidInteractListener(JavaPlugin plugin, KidManager kidManager, KidRegistry kidRegistry, DialogueEngine dialogueEngine) {
        this.plugin = plugin;
        this.kidManager = kidManager;
        this.kidRegistry = kidRegistry;
        this.dialogueEngine = dialogueEngine;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Villager kid) || !kidManager.isKid(kid)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        kidManager.catchUpGrowth(kid, kidRegistry);

        boolean isParent = kidManager.isParent(kid, player.getUniqueId());
        boolean canManage = isParent || player.hasPermission("nexusfamily.admin")
                || plugin.getConfig().getBoolean("kids.management-open-to-everyone", false);

        if (player.isSneaking() && canManage) {
            KidMenuGui.openMenu(player, kid, kidManager);
            return;
        }
        if (player.isSneaking()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig()
                    .getString("messages.not-your-kid", "&cThat's not your kid, and you don't have permission to manage it.")));
            return;
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&f" + kidManager.getName(kid) + "&7: " + dialogueEngine.greeting(isParent)));
    }
}
