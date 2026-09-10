package com.nexuscraft.nexusfamily;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Right-clicking another player while holding a Wedding Ring proposes to them (or, if they had
 * already rung the clicker first, accepts that reciprocal proposal instead). */
public final class MarriageListener implements Listener {

    private final RingItem ringItem;
    private final MarriageManagerBridge bridge;

    public MarriageListener(RingItem ringItem, MarriageManagerBridge bridge) {
        this.ringItem = ringItem;
        this.bridge = bridge;
    }

    /** Small seam so this listener doesn't need to duplicate MarryCommand's result-message logic. */
    public interface MarriageManagerBridge {
        void handleRingPropose(Player proposer, Player target);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // avoid double-firing on the off-hand copy of this event
        }
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Player target)) {
            return;
        }
        Player proposer = event.getPlayer();
        ItemStack inHand = proposer.getInventory().getItemInMainHand();
        if (!ringItem.isRing(inHand)) {
            return;
        }
        event.setCancelled(true);
        bridge.handleRingPropose(proposer, target);
    }
}
