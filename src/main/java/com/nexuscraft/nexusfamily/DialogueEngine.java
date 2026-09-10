package com.nexuscraft.nexusfamily;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * A kid's "basic AI" is plain keyword matching against config-defined phrase pairs -- no external
 * AI service, no network call, no per-message cost. The first configured entry whose keywords ALL
 * appear (case-insensitive, substring match) somewhere in the message wins; otherwise a random
 * fallback line is used.
 */
public final class DialogueEngine {

    private final JavaPlugin plugin;
    private final Random random = new Random();

    public DialogueEngine(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String greeting(boolean fromParent) {
        List<String> pool = fromParent
                ? plugin.getConfig().getStringList("dialogue.parent-greetings")
                : plugin.getConfig().getStringList("dialogue.greetings");
        return pickRandom(pool, "Hi!");
    }

    public String reply(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        List<Map<?, ?>> responses = plugin.getConfig().getMapList("dialogue.responses");
        if (responses != null) {
            for (Map<?, ?> entry : responses) {
                Object keywordsRaw = entry.get("keywords");
                if (!(keywordsRaw instanceof List<?> keywords) || keywords.isEmpty()) {
                    continue;
                }
                boolean allPresent = true;
                for (Object keyword : keywords) {
                    if (!lower.contains(String.valueOf(keyword).toLowerCase(Locale.ROOT))) {
                        allPresent = false;
                        break;
                    }
                }
                if (allPresent) {
                    Object repliesRaw = entry.get("replies");
                    if (repliesRaw instanceof List<?> replies && !replies.isEmpty()) {
                        return String.valueOf(replies.get(random.nextInt(replies.size())));
                    }
                }
            }
        }
        return pickRandom(plugin.getConfig().getStringList("dialogue.fallback"), "...");
    }

    private String pickRandom(List<String> pool, String fallback) {
        if (pool == null || pool.isEmpty()) {
            return fallback;
        }
        return pool.get(random.nextInt(pool.size()));
    }
}
