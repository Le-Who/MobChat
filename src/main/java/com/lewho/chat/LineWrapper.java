// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.chat;

import java.util.ArrayList;
import java.util.List;

public class LineWrapper {

    public static List<String> wrapLines(String text, int maxWidth) {
        List<String> wrappedLines = new ArrayList<>();
        if (text == null || text.isEmpty()) return wrappedLines;

        int lineStart = 0;
        int lastSpace = -1;
        int lineWidth = 0;

        for (int i = 0; i < text.length();) {
            int codePoint = text.codePointAt(i);
            int charWidth = isWide(codePoint) ? 2 : 1;

            if (Character.isWhitespace(codePoint)) {
                lastSpace = i;
            }

            if (lineWidth + charWidth > maxWidth) {
                int breakPos;
                if (lastSpace >= lineStart) {
                    breakPos = lastSpace;
                } else {
                    breakPos = i;
                }

                wrappedLines.add(text.substring(lineStart, breakPos).trim());
                lineStart = breakPos;
                lineWidth = 0;
                lastSpace = -1;

                if (lineStart < text.length() && Character.isWhitespace(text.codePointAt(lineStart))) {
                    lineStart += Character.charCount(text.codePointAt(lineStart));
                }
                i = lineStart;
                continue;
            }

            lineWidth += charWidth;
            i += Character.charCount(codePoint);
        }

        if (lineStart < text.length()) {
            wrappedLines.add(text.substring(lineStart).trim());
        }

        return wrappedLines;
    }

    private static boolean isWide(int codePoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA
                || block == Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS
                || block == Character.UnicodeBlock.HANGUL_SYLLABLES
                || block == Character.UnicodeBlock.HANGUL_JAMO
                || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }
}
