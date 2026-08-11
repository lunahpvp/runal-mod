package com.runal.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

/**
 * MageRPG's devs are adding a "RunalUtils" signal specifically for this mod: a chat message
 * tagged with a fixed signature color (#FF003E) that's sent only to the triggering player -
 * no player-name check needed, it's already private. This exists because vanilla's own
 * cooldown system only tracks one value per item, but weapons like Phoenix's Wrath have two
 * independently-timed techniques sharing the same item stack. Each known signal maps to a
 * named cooldown so both can be tracked and displayed at once. The message is hidden from
 * chat once recognized - it's a data signal, not something meant to be read.
 */
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
