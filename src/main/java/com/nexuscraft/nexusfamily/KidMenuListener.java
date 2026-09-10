package com.nexuscraft.nexusfamily;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class KidMenuListener implements Listener {

    private final KidManager kidManager;

    public KidMenuListener(KidManager kidManager) {
        this.kidManager = kidManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory clicked = event.getClickedInventory();
        if (clicked == null || clicked != event.getInventory()) {
            return; // ignore clicks in the player's own inventory while our GUI is open
        }
        InventoryHolder holder = clicked.getHolder();
        if (!(holder instanceof KidMenuHolder menuHolder)) {
            return; // the storage GUI (KidStorageHolder) is a normal inventory -- let items move
        }
        event.setCancelled(true); // the button menu itself never gives up its items

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Entity kidEntity = Bukkit.getEntity(menuHolder.getKidId());
        if (!(kidEntity instanceof Villager kid) || !kidManager.isKid(kid)) {
            player.closeInventory();
            return;
        }

        switch (event.getSlot()) {
            case KidMenuGui.SLOT_INVENTORY -> {
                player.closeInventory();
                KidMenuGui.openStorage(player, kid, kidManager);
            }
            case KidMenuGui.SLOT_INFO -> {
                var mother = kidManager.getMother(kid);
                var father = kidManager.getFather(kid);
                String motherName = mother != null ? Bukkit.getOfflinePlayer(mother).getName() : "unknown";
                String fatherName = father != null ? Bukkit.getOfflinePlayer(father).getName() : "unknown";
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        "&b" + kidManager.getName(kid) + "&7 -- parents: &f" + motherName + " &7and &f" + fatherName
                                + (kid.isAdult() ? " &7(grown up)" : " &7(still growing up)")));
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof KidStorageHolder storageHolder)) {
            return;
        }
        Entity kidEntity = Bukkit.getEntity(storageHolder.getKidId());
        if (kidEntity instanceof Villager kid && kidManager.isKid(kid)) {
            kidManager.saveInventoryContents(kid, event.getInventory().getContents());
        }
    }
}
