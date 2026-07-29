package com.runal.client.mixin;

import com.runal.client.emoji.EmojiReplacer;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public abstract class ChatComponentEmojiMixin {
    @ModifyVariable(
            method = "addMessage",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0
    )
    private Component runal$renderEmojiShortcodes(Component message) {
        return EmojiReplacer.replaceIn(message);
    }
}
