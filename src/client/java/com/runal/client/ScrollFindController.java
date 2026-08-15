package com.runal.client;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScrollFindController {
    // The broadcast is one multi-line message ("Scroll Found!\nX has found an Ancient
    // Scroll!\n...") visible to everyone nearby, not just the finder - only report it when
    // the named finder is this client's own player, matching how boss kills/deaths are
    // self-reported rather than trusting a client to vouch for someone else's stats.
    private static final Pattern SCROLL_FOUND_PATTERN =
            Pattern.compile("(?m)^(.+) has found an Ancient Scroll!$");

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            handleMessage(message.getString());
        });
    }

    private static void handleMessage(String text) {
        Matcher matcher = SCROLL_FOUND_PATTERN.matcher(text);
        if (!matcher.find()) return;

        String finder = matcher.group(1).trim();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!finder.equalsIgnoreCase(mc.player.getGameProfile().name())) return;

        RunalPresenceClient.sendScrollFound();
    }
}
