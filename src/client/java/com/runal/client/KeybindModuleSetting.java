package com.runal.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

//? if 1.21.4 {
//?} else {
import net.minecraft.client.input.KeyEvent;
//?}

public class KeybindModuleSetting implements ModuleSetting {

    private final KeyMapping keyMapping;
    private final String label;
    private boolean listening = false;

    public KeybindModuleSetting(KeyMapping keyMapping) {
        this("Keybind", keyMapping);
    }

    public KeybindModuleSetting(String label, KeyMapping keyMapping) {
        this.label = label;
        this.keyMapping = keyMapping;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDisplayValue() {
        if (listening) return "Press key...";
        if (keyMapping == null) return "Unavailable";
        return keyMapping.getTranslatedKeyMessage().getString();
    }

    @Override
    public void onClick() {
        if (keyMapping != null) listening = true;
    }

    public boolean isListening() {
        return listening;
    }

    public void clear() {
        if (keyMapping == null) return;
        keyMapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_UNKNOWN));
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        listening = false;
    }

    @Override
    public void resetToDefault() {
        if (keyMapping == null) return;
        keyMapping.setKey(keyMapping.getDefaultKey());
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        listening = false;
    }

    public boolean handleKeyPress(int keyCode, int scanCode) {
        if (!listening) return false;

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            listening = false;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
            clear();
            return true;
        }

        //? if 1.21.4 {
        /*keyMapping.setKey(InputConstants.getKey(keyCode, scanCode));
        *///?} else {
        keyMapping.setKey(InputConstants.getKey(new KeyEvent(keyCode, scanCode, 0)));
        //?}
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        listening = false;
        return true;
    }
}
