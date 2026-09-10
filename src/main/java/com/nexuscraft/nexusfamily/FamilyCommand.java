package com.nexuscraft.nexusfamily;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;

public final class FamilyCommand implements CommandExecutor {

    private static final int LOOK_RANGE = 8;

    private final JavaPlugin plugin;
    private final KidManager kidManager;
    private final KidRegistry kidRegistry;
    private final DialogueEngine dialogueEngine;

    public FamilyCommand(JavaPlugin plugin, KidManager kidManager, KidRegistry kidRegistry, DialogueEngine dialogueEngine) {
        this.plugin = plugin;
        this.kidManager = kidManager;
        this.kidRegistry = kidRegistry;
        this.dialogueEngine = dialogueEngine;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /family.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(color("&cUsage: /family <talk <message>|addline <line>|rename <name>|info>"));
            return true;
        }

        Villager kid = findTargetKid(player);
        if (kid == null) {
            player.sendMessage(color(plugin.getConfig().getString("messages.no-target-kid",
                    "&cLook at one of your kids (within a few blocks) and try again.")));
            return true;
        }
        kidManager.catchUpGrowth(kid, kidRegistry);

        String sub = args[0].toLowerCase();
        boolean canManage = kidManager.isParent(kid, player.getUniqueId()) || player.hasPermission("nexusfamily.admin")
                || plugin.getConfig().getBoolean("kids.management-open-to-everyone", false);

        switch (sub) {
            case "talk" -> {
                if (args.length < 2) {
                    player.sendMessage(color("&cUsage: /family talk <message>"));
                    return true;
                }
                String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                String reply = dialogueEngine.reply(message);
                player.sendMessage(color("&f" + kidManager.getName(kid) + "&7: " + reply));
            }
            case "addline" -> {
                if (!canManage) {
                    player.sendMessage(color(plugin.getConfig().getString("messages.not-your-kid",
                            "&cThat's not your kid, and you don't have permission to manage it.")));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(color("&cUsage: /family addline <line>"));
                    return true;
                }
                String line = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                kidManager.addLine(kid, line);
                player.sendMessage(color(plugin.getConfig().getString("messages.line-added",
                                "&aAdded a new line for {name} to say.")
                        .replace("{name}", kidManager.getName(kid))));
            }
            case "rename" -> {
                if (!canManage) {
                    player.sendMessage(color(plugin.getConfig().getString("messages.not-your-kid",
                            "&cThat's not your kid, and you don't have permission to manage it.")));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(color("&cUsage: /family rename <name>"));
                    return true;
                }
                String oldName = kidManager.getName(kid);
                String newName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                kidManager.setName(kid, newName);
                player.sendMessage(color(plugin.getConfig().getString("messages.renamed",
                                "&aRenamed {old} to {new}.")
                        .replace("{old}", oldName).replace("{new}", newName)));
            }
            case "info" -> {
                var mother = kidManager.getMother(kid);
                var father = kidManager.getFather(kid);
                String motherName = mother != null ? Bukkit.getOfflinePlayer(mother).getName() : "unknown";
                String fatherName = father != null ? Bukkit.getOfflinePlayer(father).getName() : "unknown";
                player.sendMessage(color("&b" + kidManager.getName(kid) + "&7 -- parents: &f" + motherName
                        + " &7and &f" + fatherName + (kid.isAdult() ? " &7(grown up)" : " &7(still growing up)")));
            }
            default -> player.sendMessage(color("&cUsage: /family <talk <message>|addline <line>|rename <name>|info>"));
        }
        return true;
    }

    private Villager findTargetKid(Player player) {
        Entity target = player.getTargetEntity(LOOK_RANGE);
        if (target instanceof Villager villager && kidManager.isKid(villager)) {
            return villager;
        }
        return null;
    }

    private String color(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw);
    }
}
