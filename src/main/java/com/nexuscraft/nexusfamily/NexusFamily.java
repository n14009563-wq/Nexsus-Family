package com.nexuscraft.nexusfamily;

import org.bukkit.plugin.java.JavaPlugin;

public final class NexusFamily extends JavaPlugin {

    private MarriageManager marriageManager;
    private KidManager kidManager;
    private KidRegistry kidRegistry;
    private RingItem ringItem;
    private KidSpawnTask kidSpawnTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ringItem = new RingItem(this);
        ringItem.loadFromConfig();

        marriageManager = new MarriageManager(this);
        marriageManager.start();

        kidManager = new KidManager(this);
        kidRegistry = new KidRegistry(this);
        kidRegistry.load();
        kidManager.rescheduleGrowthOnEnable(kidRegistry);

        DialogueEngine dialogueEngine = new DialogueEngine(this);

        MarryCommand marryCommand = new MarryCommand(this, marriageManager, ringItem);
        getCommand("marry").setExecutor(marryCommand);
        getCommand("family").setExecutor(new FamilyCommand(this, kidManager, kidRegistry, dialogueEngine));
        getCommand("nexusfamily").setExecutor(new NexusFamilyCommand(this, ringItem));

        getServer().getPluginManager().registerEvents(new MarriageListener(ringItem, (proposer, target) ->
                marryCommand.handleProposeResult(proposer, target, marriageManager.propose(proposer, target))), this);
        getServer().getPluginManager().registerEvents(new KidInteractListener(this, kidManager, kidRegistry, dialogueEngine), this);
        getServer().getPluginManager().registerEvents(new KidMenuListener(kidManager), this);

        new RingRecipeRegistrar(this, ringItem).register();

        long intervalTicks = Math.max(1L, getConfig().getLong("kids.check-interval-minutes", 30)) * 60L * 20L;
        kidSpawnTask = new KidSpawnTask(this, marriageManager, kidManager, kidRegistry);
        kidSpawnTask.runTaskTimer(this, intervalTicks, intervalTicks);
    }

    @Override
    public void onDisable() {
        if (marriageManager != null) {
            marriageManager.stop();
        }
        if (kidSpawnTask != null) {
            try {
                kidSpawnTask.cancel();
            } catch (IllegalStateException ignored) {
                // already cancelled / scheduler already shutting down
            }
        }
    }
}
