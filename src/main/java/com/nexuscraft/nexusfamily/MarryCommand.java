package com.nexuscraft.nexusfamily;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

public final class MarryCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final MarriageManager marriageManager;
    private final RingItem ringItem;

    public MarryCommand(JavaPlugin plugin, MarriageManager marriageManager, RingItem ringItem) {
        this.plugin = plugin;
        this.marriageManager = marriageManager;
        this.ringItem = ringItem;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /marry.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(color(plugin.getConfig().getString("messages.marry-usage",
                    "&cUsage: /marry <propose <player>|accept|deny|divorce|status|ring <player>>")));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "propose" -> {
                if (args.length < 2) {
                    player.sendMessage(color("&cUsage: /marry propose <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(color("&cThat player isn't online."));
                    return true;
                }
                handleProposeResult(player, target, marriageManager.propose(player, target));
            }
            case "accept" -> {
                switch (marriageManager.accept(player)) {
                    case NONE_PENDING -> player.sendMessage(color(plugin.getConfig().getString(
                            "messages.proposal-no-pending", "&cYou don't have a pending proposal to respond to.")));
                    case ALREADY_MARRIED -> player.sendMessage(color(plugin.getConfig().getString(
                            "messages.already-married", "&cYou're already married. Run /marry divorce first.")));
                    case ACCEPTED -> {
                        // broadcast already sent inside MarriageManager
                    }
                    default -> {
                    }
                }
            }
            case "deny" -> {
                if (marriageManager.deny(player) == MarriageManager.RespondResult.NONE_PENDING) {
                    player.sendMessage(color(plugin.getConfig().getString(
                            "messages.proposal-no-pending", "&cYou don't have a pending proposal to respond to.")));
                } else {
                    player.sendMessage(color(plugin.getConfig().getString(
                            "messages.proposal-denied", "&cThat proposal was denied.")));
                }
            }
            case "divorce" -> {
                if (!marriageManager.divorce(player.getUniqueId())) {
                    player.sendMessage(color(plugin.getConfig().getString("messages.not-married", "&cYou're not married.")));
                }
            }
            case "status" -> {
                var spouseId = marriageManager.getSpouse(player.getUniqueId());
                if (spouseId == null) {
                    player.sendMessage(color(plugin.getConfig().getString("messages.not-married", "&cYou're not married.")));
                } else {
                    String spouseName = Bukkit.getOfflinePlayer(spouseId).getName();
                    long marriedSince = marriageManager.getMarriedSince(player.getUniqueId());
                    long days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - marriedSince);
                    player.sendMessage(color("&aMarried to &f" + spouseName + " &afor &f" + days + " &aday(s)."));
                }
            }
            case "ring" -> {
                if (!player.hasPermission("nexusfamily.admin")) {
                    player.sendMessage(color("&cYou don't have permission to hand out rings."));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(color("&cUsage: /marry ring <player> [amount]"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(color("&cThat player isn't online."));
                    return true;
                }
                int amount = 1;
                if (args.length >= 3) {
                    try {
                        amount = Math.max(1, Integer.parseInt(args[2]));
                    } catch (NumberFormatException ignored) {
                    }
                }
                target.getInventory().addItem(ringItem.create(amount));
                player.sendMessage(color("&aGave " + target.getName() + " " + amount + " Wedding Ring(s)."));
            }
            default -> player.sendMessage(color(plugin.getConfig().getString("messages.marry-usage",
                    "&cUsage: /marry <propose <player>|accept|deny|divorce|status|ring <player>>")));
        }
        return true;
    }

    public void handleProposeResult(Player player, Player target, MarriageManager.ProposeResult result) {
        switch (result) {
            case SELF -> player.sendMessage(color("&cYou can't marry yourself."));
            case ALREADY_MARRIED -> player.sendMessage(color(plugin.getConfig().getString(
                    "messages.already-married", "&cYou're already married. Run /marry divorce first.")));
            case TARGET_ALREADY_MARRIED -> player.sendMessage(color(plugin.getConfig().getString(
                            "messages.target-already-married", "&c{target} is already married to someone else.")
                    .replace("{target}", target.getName())));
            case OK, RECIPROCAL_ACCEPTED -> {
                // messages/broadcast already sent inside MarriageManager
            }
        }
    }

    private String color(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw);
    }
}
