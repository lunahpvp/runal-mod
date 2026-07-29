package com.runal.client.emoji;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.components.EditBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public final class EmojiSuggestionProvider {
    private static final Pattern EMOJI_TOKEN = Pattern.compile("(?i):[a-z0-9_]*:?");
    private static final List<String> EMOJI_COMMANDS = List.of(
            "/ac ", "/gc ", "/grc ", "/msg ", "/r ", "/reply "
    );

    private EmojiSuggestionProvider() {
    }

    public static boolean isTypingEmoji(EditBox input) {
        if (input == null) return false;

        String text = input.getValue();
        int cursor = input.getCursorPosition();
        if (text == null || text.isEmpty() || cursor <= 0 || cursor > text.length()) {
            return false;
        }
        if (text.startsWith("/") && !isEmojiCommand(text)) return false;

        int openingColon = findOpeningColon(text, cursor);
        return openingColon >= 0
                && EMOJI_TOKEN.matcher(text.substring(openingColon, cursor)).matches();
    }

    public static CompletableFuture<Suggestions> provideSuggestions(String input, int cursor) {
        if (input == null || input.isEmpty() || cursor <= 0 || cursor > input.length()) {
            return Suggestions.empty();
        }

        int start = findOpeningColon(input, cursor);
        if (start < 0) return Suggestions.empty();

        String token = input.substring(start, cursor);
        String search = token.length() <= 1
                ? ""
                : token.substring(1).replaceFirst(":$", "").toLowerCase(Locale.ROOT);

        List<Suggestion> exact = new ArrayList<>();
        List<Suggestion> startsWith = new ArrayList<>();
        List<Suggestion> contains = new ArrayList<>();
        StringRange range = StringRange.between(start, cursor);

        for (EmojiShortcodes.EmojiData emoji : EmojiShortcodes.suggestions()) {
            Suggestion suggestion = new Suggestion(range, emoji.suggestionText());
            if (search.isEmpty()) contains.add(suggestion);
            else if (emoji.cleanName().equals(search)) exact.add(suggestion);
            else if (emoji.cleanName().startsWith(search)) startsWith.add(suggestion);
            else if (emoji.cleanName().contains(search)) contains.add(suggestion);
        }

        Comparator<Suggestion> byText = Comparator.comparing(Suggestion::getText);
        exact.sort(byText);
        startsWith.sort(byText);
        contains.sort(byText);

        List<Suggestion> combined = new ArrayList<>(
                exact.size() + startsWith.size() + contains.size()
        );
        combined.addAll(exact);
        combined.addAll(startsWith);
        combined.addAll(contains);

        if (combined.isEmpty()) return Suggestions.empty();
        if (combined.size() > 200) combined = combined.subList(0, 200);
        return CompletableFuture.completedFuture(new Suggestions(range, combined));
    }

    public static Suggestions merge(Suggestions server, Suggestions emojis) {
        List<Suggestion> merged = new ArrayList<>(emojis.getList());
        for (Suggestion suggestion : server.getList()) {
            if (!suggestion.getText().startsWith(":")) merged.add(suggestion);
        }
        return new Suggestions(emojis.getRange(), merged);
    }

    private static boolean isEmojiCommand(String text) {
        for (String prefix : EMOJI_COMMANDS) {
            if (text.regionMatches(true, 0, prefix, 0, prefix.length())) return true;
        }
        return false;
    }

    private static int findOpeningColon(String text, int cursor) {
        int index = cursor - 1;
        boolean endsWithColon = false;
        if (index >= 0 && text.charAt(index) == ':') {
            endsWithColon = true;
            index--;
        }

        while (index >= 0) {
            char character = text.charAt(index);
            if (character == ':') return index;
            if (Character.isWhitespace(character)) {
                return endsWithColon ? cursor - 1 : -1;
            }
            index--;
        }
        return endsWithColon ? cursor - 1 : -1;
    }
}
