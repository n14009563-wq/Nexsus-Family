package com.nexuscraft.nexusfamily;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/** Marks an open Inventory as a kid's actual armor/tools storage -- when this closes, its
 * contents get written back into the kid's PersistentDataContainer. */
public final class KidStorageHolder implements InventoryHolder {

    private final UUID kidId;
    private Inventory inventory;

    public KidStorageHolder(UUID kidId) {
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
