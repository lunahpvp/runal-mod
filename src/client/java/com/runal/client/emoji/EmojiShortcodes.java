package com.runal.client.emoji;

import net.minecraft.client.gui.components.EditBox;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmojiShortcodes {
    private static final String RESOURCE = "/assets/runal/emoji/shortcodes.properties";
    private static final Pattern SUGGESTION_ARTIFACT =
            Pattern.compile("(:[A-Za-z0-9_]+:)\\*?\\s+[\\uF400-\\uF4FF]+");

    private static final Map<String, String> MAPPINGS = loadMappings();
    private static final Map<String, String> REVERSE_MAPPINGS = createReverseMappings();
    private static final List<EmojiData> SUGGESTIONS = createSuggestions();

    private EmojiShortcodes() {
    }

    public static Map<String, String> mappings() {
        return MAPPINGS;
    }

    public static List<EmojiData> suggestions() {
        return SUGGESTIONS;
    }

    public static String replaceGlyphsWithShortcodes(String input) {
        if (input == null || input.isEmpty()) return input == null ? "" : input;

        StringBuilder result = new StringBuilder(input.length());
        for (int index = 0; index < input.length(); index++) {
            String glyph = String.valueOf(input.charAt(index));
            String shortcode = REVERSE_MAPPINGS.get(glyph);
            result.append(shortcode == null ? glyph : shortcode);
        }
        return result.toString();
    }

    public static void processEditBox(EditBox input) {
        if (input == null) return;

        String original = input.getValue();
        if (original == null || original.isEmpty()) return;

        String text = original;
        int cursor = input.getCursorPosition();

        Matcher artifactMatcher = SUGGESTION_ARTIFACT.matcher(text);
        StringBuffer cleaned = new StringBuffer();
        while (artifactMatcher.find()) {
            String replacement = artifactMatcher.group(1);
            artifactMatcher.appendReplacement(cleaned, Matcher.quoteReplacement(replacement));

            if (cursor > artifactMatcher.start()) {
                int removed = artifactMatcher.group().length() - replacement.length();
                if (cursor >= artifactMatcher.end()) cursor -= removed;
                else cursor = artifactMatcher.start() + replacement.length();
            }
        }
        artifactMatcher.appendTail(cleaned);
        text = cleaned.toString();

        StringBuilder replaced = new StringBuilder(text.length());
        int newCursor = cursor;
        int index = 0;
        while (index < text.length()) {
            if (text.charAt(index) == ':') {
                int closingColon = findClosingColon(text, index);
                if (closingColon >= 0) {
                    String shortcode = text.substring(index, closingColon + 1);
                    String glyph = MAPPINGS.get(shortcode);
                    if (glyph != null) {
                        replaced.append(glyph);
                        int removed = shortcode.length() - glyph.length();
                        if (cursor > closingColon) newCursor -= removed;
                        else if (cursor > index) newCursor = replaced.length();
                        index = closingColon + 1;
                        continue;
                    }
                }
            }

            replaced.append(text.charAt(index));
            index++;
        }

        text = replaced.toString();
        if (!text.equals(original)) {
            int clampedCursor = Math.max(0, Math.min(newCursor, text.length()));
            input.setValue(text);
            input.setCursorPosition(clampedCursor);
            input.setHighlightPos(clampedCursor);
        }
    }

    private static int findClosingColon(String text, int openingColon) {
        int end = Math.min(text.length(), openingColon + 32);
        for (int index = openingColon + 1; index < end; index++) {
            char character = text.charAt(index);
            if (character == ':') return index;
            if (!(Character.isLetterOrDigit(character) || character == '_')) return -1;
        }
        return -1;
    }

    private static Map<String, String> loadMappings() {
        Map<String, String> mappings = new LinkedHashMap<>();
        try (InputStream stream = EmojiShortcodes.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) return Collections.emptyMap();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                    int equals = trimmed.indexOf('=');
                    if (equals <= 0) continue;

                    String shortcode = trimmed.substring(0, equals).trim();
                    String glyph = unescapeUnicode(trimmed.substring(equals + 1).trim());
                    if (!shortcode.isEmpty() && !glyph.isEmpty()) {
                        mappings.put(shortcode, glyph);
                    }
                }
            }
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(mappings);
    }

    private static Map<String, String> createReverseMappings() {
        Map<String, String> reverse = new LinkedHashMap<>();
        MAPPINGS.forEach((shortcode, glyph) -> reverse.put(glyph, shortcode));
        return Collections.unmodifiableMap(reverse);
    }

    private static List<EmojiData> createSuggestions() {
        List<EmojiData> suggestions = new ArrayList<>(MAPPINGS.size());
        MAPPINGS.forEach((shortcode, glyph) -> suggestions.add(new EmojiData(
                shortcode,
                shortcode.substring(1, shortcode.length() - 1).toLowerCase(),
                glyph,
                shortcode + " " + glyph
        )));
        return Collections.unmodifiableList(suggestions);
    }

    private static String unescapeUnicode(String input) {
        StringBuilder result = new StringBuilder(input.length());
        int index = 0;
        while (index < input.length()) {
            if (input.charAt(index) == '\\'
                    && index + 5 < input.length()
                    && input.charAt(index + 1) == 'u') {
                try {
                    result.append((char) Integer.parseInt(
                            input.substring(index + 2, index + 6),
                            16
                    ));
                    index += 6;
                    continue;
                } catch (NumberFormatException ignored) {
                }
            }
            result.append(input.charAt(index));
            index++;
        }
        return result.toString();
    }

    public record EmojiData(
            String shortcode,
            String cleanName,
            String glyph,
            String suggestionText
    ) {
    }
}
