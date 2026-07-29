package com.runal.client.mixin;

import com.runal.client.emoji.EmojiShortcodes;
//? if 1.21.4 || 1.21.11 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenEmojiMixin {
    @Shadow protected EditBox input;

    //? if 1.21.4 || 1.21.11 {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void runal$processEmojiInput(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float deltaTicks,
            CallbackInfo ci
    ) {
        EmojiShortcodes.processEditBox(input);
    }
    *///?} else {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void runal$processEmojiInput(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float deltaTicks,
            CallbackInfo ci
    ) {
        EmojiShortcodes.processEditBox(input);
    }
    //?}

    @ModifyVariable(
            method = "handleChatInput",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private String runal$sendEmojiShortcodes(String message) {
        return EmojiShortcodes.replaceGlyphsWithShortcodes(message);
    }
}
