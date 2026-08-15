package com.runal.client;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerDeathController {
    // "immediateRespawn" is on for this server, so the death screen never shows - can't
    // detect death that way. The server broadcasts "X has died!" to everyone nearby instead,
    // but only during raid fights (not every death server-wide), so this tracks raid deaths
    // specifically. Only reports it when the named player is this client's own, same as scrolls.
    private static final Pattern DEATH_PATTERN = Pattern.compile("^(.+) has died!$");

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            handleMessage(message.getString().trim());
        });
    }

    private static void handleMessage(String text) {
        Matcher matcher = DEATH_PATTERN.matcher(text);
        if (!matcher.matches()) return;

        String who = matcher.group(1).trim();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!who.equalsIgnoreCase(mc.player.getGameProfile().name())) return;

        RunalPresenceClient.sendRaidDeath();
    }
}
