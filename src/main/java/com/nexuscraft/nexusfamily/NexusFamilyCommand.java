package com.nexuscraft.nexusfamily;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class NexusFamilyCommand implements CommandExecutor {

    private final NexusFamily plugin;
    private final RingItem ringItem;

    public NexusFamilyCommand(NexusFamily plugin, RingItem ringItem) {
        this.plugin = plugin;
        this.ringItem = ringItem;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            ringItem.loadFromConfig();
            sender.sendMessage("NexusFamily config reloaded.");
            return true;
        }
        sender.sendMessage("Usage: /nexusfamily reload");
        return true;
    }
}
