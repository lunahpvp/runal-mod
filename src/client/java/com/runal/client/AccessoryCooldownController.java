package com.runal.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.util.LinkedHashMap;
import java.util.Map;

public class AccessoryCooldownController {

    private record AbilityInfo(String itemName, double cooldownSeconds) {
    }

    private static final Map<String, AbilityInfo> ABILITIES = new LinkedHashMap<>();

    static {
        ABILITIES.put("Necrotic Power", new AbilityInfo("Shaman Lord Cloak", 100));
        ABILITIES.put("Poseidon's Blessing", new AbilityInfo("Poseidon's Blessing", 150));
        ABILITIES.put("Arcane Surge", new AbilityInfo("Ascendant Heart", 75));
        ABILITIES.put("Divine Link", new AbilityInfo("Elysian Cross", 120));
        ABILITIES.put("Emerald Mana", new AbilityInfo("Evergreen Crown", 60));
        ABILITIES.put("Forest Power", new AbilityInfo("Verdant Heart", 60));
        ABILITIES.put("Thunderflame Impact", new AbilityInfo("Thunderflame Gloves", 15));
        ABILITIES.put("Beast Rage", new AbilityInfo("Beast Cloak", 60));
        ABILITIES.put("Harmonial Energy", new AbilityInfo("Belt of Harmony", 50));
        ABILITIES.put("Night Tether", new AbilityInfo("Belt of Night", 1.5));
        ABILITIES.put("Golden Revival", new AbilityInfo("Dune Necklace", 60));
        ABILITIES.put("Imperial Magic", new AbilityInfo("Imperial Bracelet", 1));
        ABILITIES.put("Sunken Shield", new AbilityInfo("Sunken Pauldrons", 60));
        // TODO: fishing drop accessories not yet mapped
        ABILITIES.put("Solarflame Impact", new AbilityInfo("Solar Gauntlets", 15));
        ABILITIES.put("Divine Protection", new AbilityInfo("Goldshine Armbrand", 45));
        ABILITIES.put("Divine Spirit", new AbilityInfo("Helios Cloak", 30));
        ABILITIES.put("Solarblessed Mana", new AbilityInfo("Helios Pendant", 60));
        ABILITIES.put("Dune Strength", new AbilityInfo("Sable Necklace", 10));
        ABILITIES.put("Ancient Defense", new AbilityInfo("Ruin Pauldrons", 50));
        ABILITIES.put("Windflow Escape", new AbilityInfo("Belt of Winds", 50));
        ABILITIES.put("Terra Power", new AbilityInfo("Terra Amulet", 50));
        ABILITIES.put("Conqueror of Magic", new AbilityInfo("Crown of Elements", 50));
    }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            handleMessage(message.getString());
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> AccessoryCooldownState.tick());
    }

    private static void handleMessage(String text) {
        for (Map.Entry<String, AbilityInfo> entry : ABILITIES.entrySet()) {
            if (text.contains(entry.getKey())) {
                AbilityInfo info = entry.getValue();
                AccessoryCooldownState.start(info.itemName(), info.cooldownSeconds());
            }
        }
    }
}
