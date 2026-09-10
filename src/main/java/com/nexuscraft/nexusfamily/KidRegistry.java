package com.nexuscraft.nexusfamily;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A kid villager's live state (name, parents, growth timer, personal lines, inventory) lives in
 * the villager entity's own PersistentDataContainer, same as everything else in this plugin.
 *
 * This registry is a small supplementary index (kids.yml) that exists ONLY for two things a PDC
 * can't do on its own: counting how many kids a couple already has (max-kids-per-couple) when
 * some of those kids might be sitting in an unloaded chunk right now, and re-scheduling a baby's
 * "grow up" timer after a server restart for the same reason. If a kid's chunk is loaded, its PDC
 * is still the source of truth for everything else about it.
 */
public final class KidRegistry {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration data;

    public KidRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kids.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
        }
        data = YamlConfiguration.loadConfiguration(file);
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save kids.yml: " + e.getMessage());
        }
    }

    public void register(UUID kidId, UUID motherId, UUID fatherId, String name, long adulthoodAtMillis) {
        String path = "kids." + kidId;
        data.set(path + ".mother", motherId.toString());
        data.set(path + ".father", fatherId.toString());
        data.set(path + ".name", name);
        data.set(path + ".adulthood-at", adulthoodAtMillis);
        save();
    }

    public void markGrown(UUID kidId) {
        data.set("kids." + kidId + ".adulthood-at", 0L);
        save();
    }

    public void forget(UUID kidId) {
        data.set("kids." + kidId, null);
        save();
    }

    public int countKidsOf(UUID a, UUID b) {
        ConfigurationSection kids = data.getConfigurationSection("kids");
        if (kids == null) {
            return 0;
        }
        int count = 0;
        for (String kidId : kids.getKeys(false)) {
            String mother = data.getString("kids." + kidId + ".mother");
            String father = data.getString("kids." + kidId + ".father");
            boolean matchesForward = a.toString().equals(mother) && b.toString().equals(father);
            boolean matchesReverse = a.toString().equals(father) && b.toString().equals(mother);
            if (matchesForward || matchesReverse) {
                count++;
            }
        }
        return count;
    }

    /** Kid IDs whose stored growth timer hasn't fired yet (adulthood-at > 0), for re-scheduling
     * on plugin enable in case their chunk wasn't loaded when the timer would have fired. */
    public List<UUID> pendingGrowth() {
        List<UUID> result = new ArrayList<>();
        ConfigurationSection kids = data.getConfigurationSection("kids");
        if (kids == null) {
            return result;
        }
        for (String kidId : kids.getKeys(false)) {
            long adulthoodAt = data.getLong("kids." + kidId + ".adulthood-at", 0L);
            if (adulthoodAt > 0) {
                result.add(UUID.fromString(kidId));
            }
        }
        return result;
    }

    public long getAdulthoodAt(UUID kidId) {
        return data.getLong("kids." + kidId + ".adulthood-at", 0L);
    }
}
