package com.nexuscraft.nexusfamily;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Periodically checks every online married couple that's currently standing near each other and,
 * at a configurable random chance, has them a kid -- fully passive, no command or item needed.
 */
public final class KidSpawnTask extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final MarriageManager marriageManager;
    private final KidManager kidManager;
    private final KidRegistry kidRegistry;
    private final Random random = new Random();

    public KidSpawnTask(JavaPlugin plugin, MarriageManager marriageManager, KidManager kidManager, KidRegistry kidRegistry) {
        this.plugin = plugin;
        this.marriageManager = marriageManager;
        this.kidManager = kidManager;
        this.kidRegistry = kidRegistry;
    }

    @Override
    public void run() {
        double proximityRadius = plugin.getConfig().getDouble("kids.proximity-radius", 10);
        int chancePercent = plugin.getConfig().getInt("kids.chance-percent", 5);
        int maxKids = plugin.getConfig().getInt("kids.max-kids-per-couple", 3);
        long growthMinutes = plugin.getConfig().getLong("kids.growth-minutes", 20);

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID spouseId = marriageManager.getSpouse(player.getUniqueId());
            if (spouseId == null) {
                continue;
            }
            // Only process each married pair once per sweep, not once per spouse.
            if (spouseId.compareTo(player.getUniqueId()) <= 0) {
                continue;
            }
            Player spouse = Bukkit.getPlayer(spouseId);
            if (spouse == null) {
                continue;
            }
            Location playerLoc = player.getLocation();
            Location spouseLoc = spouse.getLocation();
            if (playerLoc.getWorld() == null || spouseLoc.getWorld() == null
                    || !playerLoc.getWorld().equals(spouseLoc.getWorld())) {
                continue;
            }
            if (playerLoc.distance(spouseLoc) > proximityRadius) {
                continue;
            }
            if (kidRegistry.countKidsOf(player.getUniqueId(), spouseId) >= maxKids) {
                continue;
            }
            if (random.nextInt(100) >= chancePercent) {
                continue;
            }

            String name = randomDefaultName();
            Villager kid = kidManager.spawnKid(playerLoc.clone(), player.getUniqueId(), spouseId, name, growthMinutes, kidRegistry);

            String broadcast = ChatColor.translateAlternateColorCodes('&', plugin.getConfig()
                    .getString("messages.kid-born", "&d{player1} and {player2} just had a kid: {name}!")
                    .replace("{player1}", player.getName())
                    .replace("{player2}", spouse.getName())
                    .replace("{name}", name));
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(broadcast);
            }
        }
    }

    private String randomDefaultName() {
        List<String> names = plugin.getConfig().getStringList("kids.default-names");
        if (names == null || names.isEmpty()) {
            return "Kid";
        }
        return names.get(random.nextInt(names.size()));
    }
}
