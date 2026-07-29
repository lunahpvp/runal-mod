package com.runal.client.mixin;

import com.mojang.brigadier.suggestion.Suggestions;
import com.runal.client.emoji.EmojiSuggestionProvider;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class ChatCommandSuggestionsMixin {
    @Shadow private EditBox input;
    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow public abstract void showSuggestions(boolean narrateFirstSuggestion);

    @Inject(method = "updateCommandInfo", at = @At("RETURN"))
    private void runal$showEmojiSuggestions(CallbackInfo ci) {
        if (!EmojiSuggestionProvider.isTypingEmoji(input)) return;

        String text = input.getValue();
        CompletableFuture<Suggestions> emojiFuture =
                EmojiSuggestionProvider.provideSuggestions(
                        text,
                        input.getCursorPosition()
                );
        CompletableFuture<Suggestions> serverFuture = pendingSuggestions;

        if (serverFuture != null && serverFuture.isDone()) {
            pendingSuggestions = emojiFuture.thenApply(emojis ->
                    EmojiSuggestionProvider.merge(serverFuture.join(), emojis)
            );
        } else {
            pendingSuggestions = emojiFuture;
        }

        showSuggestions(false);
    }
}
