package com.perigrine3.createcybernetics.item.generic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class InfologFormatting {
    public static final char EDITOR_PREFIX = '&';
    public static final char SAVED_PREFIX = '\u00A7';

    private InfologFormatting() {
    }

    public static String toEditorText(
            String text
    ) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return text.replace(
                SAVED_PREFIX,
                EDITOR_PREFIX
        );
    }

    public static String toSavedText(
            String text
    ) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder converted =
                new StringBuilder(text.length());

        for (int index = 0;
             index < text.length();
             index++) {
            char current =
                    text.charAt(index);

            if (current == EDITOR_PREFIX &&
                    index + 1 < text.length()) {
                char code =
                        Character.toLowerCase(
                                text.charAt(index + 1)
                        );

                if (isValidCode(code)) {
                    converted.append(
                            SAVED_PREFIX
                    );

                    converted.append(code);

                    index++;
                    continue;
                }
            }

            converted.append(current);
        }

        return converted.toString();
    }

    public static Component parseSavedText(
            String text
    ) {
        return parse(
                text,
                SAVED_PREFIX
        );
    }

    public static Component parseEditorText(
            String text
    ) {
        return parse(
                text,
                EDITOR_PREFIX
        );
    }

    private static Component parse(
            String text,
            char prefix
    ) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        MutableComponent result =
                Component.empty();

        StringBuilder segment =
                new StringBuilder();

        Style currentStyle =
                Style.EMPTY;

        for (int index = 0;
             index < text.length();
             index++) {
            char current =
                    text.charAt(index);

            if (current == prefix &&
                    index + 1 < text.length()) {
                char code =
                        Character.toLowerCase(
                                text.charAt(index + 1)
                        );

                ChatFormatting formatting =
                        ChatFormatting.getByCode(code);

                if (formatting != null) {
                    appendSegment(
                            result,
                            segment,
                            currentStyle
                    );

                    currentStyle =
                            applyFormatting(
                                    currentStyle,
                                    formatting
                            );

                    index++;
                    continue;
                }
            }

            segment.append(current);
        }

        appendSegment(
                result,
                segment,
                currentStyle
        );

        return result;
    }

    private static void appendSegment(
            MutableComponent result,
            StringBuilder segment,
            Style style
    ) {
        if (segment.isEmpty()) {
            return;
        }

        result.append(
                Component.literal(
                        segment.toString()
                ).setStyle(style)
        );

        segment.setLength(0);
    }

    private static Style applyFormatting(
            Style currentStyle,
            ChatFormatting formatting
    ) {
        if (formatting == ChatFormatting.RESET) {
            return Style.EMPTY;
        }

        if (formatting.isColor()) {
            return Style.EMPTY.withColor(
                    formatting
            );
        }

        return switch (formatting) {
            case BOLD ->
                    currentStyle.withBold(true);

            case ITALIC ->
                    currentStyle.withItalic(true);

            case UNDERLINE ->
                    currentStyle.withUnderlined(true);

            case STRIKETHROUGH ->
                    currentStyle.withStrikethrough(true);

            case OBFUSCATED ->
                    currentStyle.withObfuscated(true);

            default ->
                    currentStyle;
        };
    }

    public static boolean isValidCode(
            char code
    ) {
        return ChatFormatting.getByCode(
                Character.toLowerCase(code)
        ) != null;
    }
}