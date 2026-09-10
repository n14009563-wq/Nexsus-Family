package com.nexuscraft.nexusfamily;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Marriage state lives entirely in each player's own PersistentDataContainer (reached via
 * Bukkit#getOfflinePlayer) -- the same PDC trick NexusWarbeasts uses on mobs. It's saved as part
 * of the player's own data automatically, works whether they're online or not, and needs no
 * separate flat file or database.
 *
 * Pending proposals (not yet accepted) are NOT persisted -- short-lived, in-memory only, keyed by
 * the RECIPIENT's UUID so /marry accept always resolves the proposal sent TO the caller. Both
 * the /marry command and a ring right-click go through the same propose()/accept()/deny() here,
 * so the two delivery methods can never disagree about state.
 */
public final class MarriageManager {

    public enum ProposeResult {OK, RECIPROCAL_ACCEPTED, SELF, ALREADY_MARRIED, TARGET_ALREADY_MARRIED}

    public enum RespondResult {ACCEPTED, DENIED, NONE_PENDING, ALREADY_MARRIED}

    private static final class Proposal {
        final UUID proposer;
        final long expiresAtMillis;

        Proposal(UUID proposer, long expiresAtMillis) {
            this.proposer = proposer;
            this.expiresAtMillis = expiresAtMillis;
        }
    }

    private final JavaPlugin plugin;
    private final NamespacedKey spouseKey;
    private final NamespacedKey marriedSinceKey;
    private final Map<UUID, Proposal> pendingByTarget = new HashMap<>();
    private BukkitRunnable expiryTask;

    public MarriageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.spouseKey = new NamespacedKey(plugin, "spouse");
        this.marriedSinceKey = new NamespacedKey(plugin, "married_since");
    }

    public void start() {
        expiryTask = new BukkitRunnable() {
            @Override
            public void run() {
                sweepExpiredProposals();
            }
        };
        expiryTask.runTaskTimer(plugin, 20L * 5, 20L * 5);
    }

    public void stop() {
        if (expiryTask != null) {
            try {
                expiryTask.cancel();
            } catch (IllegalStateException ignored) {
                // already cancelled / scheduler already shutting down
            }
        }
    }

    private void sweepExpiredProposals() {
        long now = System.currentTimeMillis();
        pendingByTarget.entrySet().removeIf(entry -> {
            if (entry.getValue().expiresAtMillis > now) {
                return false;
            }
            Player proposer = Bukkit.getPlayer(entry.getValue().proposer);
            if (proposer != null) {
                proposer.sendMessage(msg("messages.proposal-expired",
                        "&cYour proposal to {target} expired.", "{target}", nameOf(entry.getKey())));
            }
            return true;
        });
    }

    // ---- status ----

    public boolean isMarried(UUID playerId) {
        return getSpouse(playerId) != null;
    }

    public UUID getSpouse(UUID playerId) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
        String raw = offline.getPersistentDataContainer().get(spouseKey, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public long getMarriedSince(UUID playerId) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
        Long value = offline.getPersistentDataContainer().get(marriedSinceKey, PersistentDataType.LONG);
        return value != null ? value : 0L;
    }

    // ---- propose / accept / deny ----

    /**
     * Used by both /marry propose and a ring right-click. If the target already has a pending
     * proposal sent TO the proposer (i.e. they rang the proposer first), this is treated as an
     * acceptance of that instead of a fresh proposal -- so two players tapping each other with
     * rings, in either order, just gets them married.
     */
    public ProposeResult propose(Player proposer, Player target) {
        Proposal reciprocal = pendingByTarget.get(proposer.getUniqueId());
        if (reciprocal != null && reciprocal.proposer.equals(target.getUniqueId())
                && reciprocal.expiresAtMillis > System.currentTimeMillis()) {
            pendingByTarget.remove(proposer.getUniqueId());
            if (isMarried(proposer.getUniqueId()) || isMarried(target.getUniqueId())) {
                return ProposeResult.ALREADY_MARRIED;
            }
            marry(proposer, target);
            return ProposeResult.RECIPROCAL_ACCEPTED;
        }

        if (proposer.getUniqueId().equals(target.getUniqueId())) {
            return ProposeResult.SELF;
        }
        if (isMarried(proposer.getUniqueId())) {
            return ProposeResult.ALREADY_MARRIED;
        }
        if (isMarried(target.getUniqueId())) {
            return ProposeResult.TARGET_ALREADY_MARRIED;
        }

        int timeoutSeconds = plugin.getConfig().getInt("marriage.proposal-timeout-seconds", 60);
        pendingByTarget.put(target.getUniqueId(),
                new Proposal(proposer.getUniqueId(), System.currentTimeMillis() + timeoutSeconds * 1000L));

        proposer.sendMessage(msg("messages.proposal-sent",
                "&aProposal sent to {target}. They have {seconds}s to /marry accept.",
                "{target}", target.getName(), "{seconds}", String.valueOf(timeoutSeconds)));
        target.sendMessage(msg("messages.proposal-received",
                "&a{proposer} wants to marry you! Run /marry accept or /marry deny.",
                "{proposer}", proposer.getName()));
        return ProposeResult.OK;
    }

    public RespondResult accept(Player target) {
        Proposal proposal = pendingByTarget.get(target.getUniqueId());
        if (proposal == null || proposal.expiresAtMillis <= System.currentTimeMillis()) {
            pendingByTarget.remove(target.getUniqueId());
            return RespondResult.NONE_PENDING;
        }
        if (isMarried(proposal.proposer) || isMarried(target.getUniqueId())) {
            pendingByTarget.remove(target.getUniqueId());
            return RespondResult.ALREADY_MARRIED;
        }
        pendingByTarget.remove(target.getUniqueId());
        Player proposer = Bukkit.getPlayer(proposal.proposer);
        if (proposer != null) {
            marry(proposer, target);
        } else {
            marryOffline(proposal.proposer, target.getUniqueId());
        }
        return RespondResult.ACCEPTED;
    }

    public RespondResult deny(Player target) {
        Proposal removed = pendingByTarget.remove(target.getUniqueId());
        if (removed == null) {
            return RespondResult.NONE_PENDING;
        }
        Player proposer = Bukkit.getPlayer(removed.proposer);
        if (proposer != null) {
            proposer.sendMessage(msg("messages.proposal-denied", "&cThat proposal was denied.", null, null));
        }
        return RespondResult.DENIED;
    }

    private void marry(Player a, Player b) {
        marryOffline(a.getUniqueId(), b.getUniqueId());
        String broadcast = msg("messages.now-married", "&a{player1} and {player2} are now married!",
                "{player1}", a.getName(), "{player2}", b.getName());
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(broadcast);
        }
    }

    private void marryOffline(UUID aId, UUID bId) {
        long now = System.currentTimeMillis();
        OfflinePlayer a = Bukkit.getOfflinePlayer(aId);
        OfflinePlayer b = Bukkit.getOfflinePlayer(bId);
        a.getPersistentDataContainer().set(spouseKey, PersistentDataType.STRING, bId.toString());
        a.getPersistentDataContainer().set(marriedSinceKey, PersistentDataType.LONG, now);
        b.getPersistentDataContainer().set(spouseKey, PersistentDataType.STRING, aId.toString());
        b.getPersistentDataContainer().set(marriedSinceKey, PersistentDataType.LONG, now);
    }

    public boolean divorce(UUID playerId) {
        UUID spouseId = getSpouse(playerId);
        if (spouseId == null) {
            return false;
        }
        OfflinePlayer a = Bukkit.getOfflinePlayer(playerId);
        OfflinePlayer b = Bukkit.getOfflinePlayer(spouseId);
        a.getPersistentDataContainer().remove(spouseKey);
        a.getPersistentDataContainer().remove(marriedSinceKey);
        b.getPersistentDataContainer().remove(spouseKey);
        b.getPersistentDataContainer().remove(marriedSinceKey);

        String broadcast = msg("messages.now-divorced", "&e{player1} and {player2} are no longer married.",
                "{player1}", nameOf(playerId), "{player2}", nameOf(spouseId));
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(broadcast);
        }
        return true;
    }

    private String nameOf(UUID playerId) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
        String name = offline.getName();
        return name != null ? name : "someone";
    }

    private String msg(String path, String def, String key1, String val1) {
        String raw = plugin.getConfig().getString(path, def);
        if (key1 != null) {
            raw = raw.replace(key1, val1);
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    private String msg(String path, String def, String key1, String val1, String key2, String val2) {
        return msg(path, def, key1, val1).replace(key2, val2);
    }
}
