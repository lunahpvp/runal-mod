package com.runal.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

// Signal messages are tagged with a fixed color, only sent to the triggering player.
// They track multi-technique weapon cooldowns since vanilla only tracks one value per item.
public class WeaponAbilityController {
    private static final int SIGNATURE_COLOR = 0xFF003E;

    private record Signal(String triggerText, String abilityName, double cooldownSeconds) {
    }

    private static final Signal[] SIGNALS = {
            new Signal("Phoe used", "Phoenix's Flames", 30),
    };

    public static void register() {
        ClientReceiveMessageEvents.ALLOW_GAME.register(WeaponAbilityController::handleMessage);
        ClientTickEvents.END_CLIENT_TICK.register(client -> WeaponCooldownState.tick());
    }

    private static boolean handleMessage(Component message, boolean overlay) {
        if (overlay) return true;

        Integer color = styleColor(message);
        if (color == null || color != SIGNATURE_COLOR) return true;

        String text = message.getString().trim();
        for (Signal signal : SIGNALS) {
            if (text.equals(signal.triggerText())) {
                WeaponCooldownState.start(signal.abilityName(), signal.cooldownSeconds());
                return false;
            }
        }
        return true;
    }

    private static Integer styleColor(Component message) {
        var color = message.getStyle().getColor();
        if (color != null) return color.getValue();
        for (Component sibling : message.getSiblings()) {
            var siblingColor = sibling.getStyle().getColor();
            if (siblingColor != null) return siblingColor.getValue();
        }
        return null;
    }
}
