package com.nexuscraft.nexusfamily;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/** Marks an open Inventory as this plugin's top-level "manage this kid" menu (Inventory/Info
 * buttons), and remembers which kid it's about -- so the click handler never has to guess which
 * GUI is open from its title or contents. */
public final class KidMenuHolder implements InventoryHolder {

    private final UUID kidId;
    private Inventory inventory;

    public KidMenuHolder(UUID kidId) {
        this.kidId = kidId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public UUID getKidId() {
        return kidId;
    }
}
