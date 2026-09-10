package com.nexuscraft.nexusfamily;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A kid is a real Villager entity, identified and fully described by its own
 * PersistentDataContainer -- marker, parents, display name, growth deadline, its own personal
 * lines, and its stored inventory contents all travel with the entity itself and survive chunk
 * unloads and restarts with no extra code. KidRegistry (kids.yml) is only a supplementary index
 * for cross-chunk bookkeeping (see its own javadoc) -- this class is the source of truth for an
 * actual loaded kid.
 */
public final class KidManager {

    // A control character that will never appear in a typed line, used to join/split the
    // kid's personal line list into a single PDC STRING value.
    private static final String LINE_DELIM = "␟";

    private final JavaPlugin plugin;
    private final NamespacedKey markerKey;
    private final NamespacedKey motherKey;
    private final NamespacedKey fatherKey;
    private final NamespacedKey nameKey;
    private final NamespacedKey adulthoodAtKey;
    private final NamespacedKey linesKey;
    private final NamespacedKey inventoryKey;

    public KidManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.markerKey = new NamespacedKey(plugin, "kid");
        this.motherKey = new NamespacedKey(plugin, "mother");
        this.fatherKey = new NamespacedKey(plugin, "father");
        this.nameKey = new NamespacedKey(plugin, "kid_name");
        this.adulthoodAtKey = new NamespacedKey(plugin, "adulthood_at");
        this.linesKey = new NamespacedKey(plugin, "kid_lines");
        this.inventoryKey = new NamespacedKey(plugin, "kid_inventory");
    }

    public boolean isKid(Entity entity) {
        if (!(entity instanceof Villager)) {
            return false;
        }
        Byte marker = entity.getPersistentDataContainer().get(markerKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    public Villager spawnKid(Location location, UUID motherId, UUID fatherId, String name,
                              long growthMinutes, KidRegistry registry) {
        Villager kid = location.getWorld().spawn(location, Villager.class);
        kid.setBaby();
        kid.setCustomName(name);
        kid.setCustomNameVisible(true);

        long adulthoodAt = System.currentTimeMillis() + growthMinutes * 60_000L;

        var pdc = kid.getPersistentDataContainer();
        pdc.set(markerKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(motherKey, PersistentDataType.STRING, motherId.toString());
        pdc.set(fatherKey, PersistentDataType.STRING, fatherId.toString());
        pdc.set(nameKey, PersistentDataType.STRING, name);
        pdc.set(adulthoodAtKey, PersistentDataType.LONG, adulthoodAt);

        registry.register(kid.getUniqueId(), motherId, fatherId, name, adulthoodAt);
        scheduleGrowth(kid.getUniqueId(), adulthoodAt, registry);
        return kid;
    }

    public void scheduleGrowth(UUID kidId, long adulthoodAtMillis, KidRegistry registry) {
        long delayTicks = Math.max(0L, (adulthoodAtMillis - System.currentTimeMillis()) / 50L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Entity entity = Bukkit.getEntity(kidId);
            if (entity instanceof Villager villager && isKid(villager) && !villager.isAdult()) {
                villager.setAdult();
            }
            registry.markGrown(kidId);
        }, delayTicks);
    }

    /** Re-arms growth timers for kids whose chunk wasn't loaded when the plugin started (their
     * delayed task from before the restart never got the chance to run). Kids in loaded chunks
     * whose time already passed are also caught the moment anyone next interacts with them, via
     * {@link #catchUpGrowth}. */
    public void rescheduleGrowthOnEnable(KidRegistry registry) {
        for (UUID kidId : registry.pendingGrowth()) {
            long adulthoodAt = registry.getAdulthoodAt(kidId);
            scheduleGrowth(kidId, adulthoodAt, registry);
        }
    }

    /** Defensive catch-up: call whenever a kid is actually interacted with, in case its own
     * scheduled growth task never got to run (e.g. its chunk was unloaded at the exact moment). */
    public void catchUpGrowth(Villager kid, KidRegistry registry) {
        Long adulthoodAt = kid.getPersistentDataContainer().get(adulthoodAtKey, PersistentDataType.LONG);
        if (adulthoodAt != null && adulthoodAt > 0 && System.currentTimeMillis() >= adulthoodAt && !kid.isAdult()) {
            kid.setAdult();
            registry.markGrown(kid.getUniqueId());
        }
    }

    public UUID getMother(Villager kid) {
        return parseUuid(kid.getPersistentDataContainer().get(motherKey, PersistentDataType.STRING));
    }

    public UUID getFather(Villager kid) {
        return parseUuid(kid.getPersistentDataContainer().get(fatherKey, PersistentDataType.STRING));
    }

    public boolean isParent(Villager kid, UUID playerId) {
        return playerId.equals(getMother(kid)) || playerId.equals(getFather(kid));
    }

    public String getName(Villager kid) {
        String name = kid.getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
        return name != null ? name : "Kid";
    }

    public void setName(Villager kid, String name) {
        kid.getPersistentDataContainer().set(nameKey, PersistentDataType.STRING, name);
        kid.setCustomName(name);
    }

    public List<String> getLines(Villager kid) {
        String raw = kid.getPersistentDataContainer().get(linesKey, PersistentDataType.STRING);
        List<String> lines = new ArrayList<>();
        if (raw != null && !raw.isEmpty()) {
            for (String line : raw.split(LINE_DELIM)) {
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    public void addLine(Villager kid, String line) {
        List<String> lines = getLines(kid);
        lines.add(line);
        kid.getPersistentDataContainer().set(linesKey, PersistentDataType.STRING, String.join(LINE_DELIM, lines));
    }

    public ItemStack[] getInventoryContents(Villager kid, int size) {
        byte[] raw = kid.getPersistentDataContainer().get(inventoryKey, PersistentDataType.BYTE_ARRAY);
        if (raw == null) {
            return new ItemStack[size];
        }
        ItemStack[] stored = ItemSerialization.fromBytes(raw);
        if (stored.length == size) {
            return stored;
        }
        ItemStack[] resized = new ItemStack[size];
        System.arraycopy(stored, 0, resized, 0, Math.min(stored.length, size));
        return resized;
    }

    public void saveInventoryContents(Villager kid, ItemStack[] contents) {
        kid.getPersistentDataContainer().set(inventoryKey, PersistentDataType.BYTE_ARRAY, ItemSerialization.toBytes(contents));
    }

    private UUID parseUuid(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
