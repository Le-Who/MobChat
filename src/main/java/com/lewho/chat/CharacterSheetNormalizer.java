// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.chat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts structured character JSON into the legacy dash-list character sheet
 * format consumed by existing UI and prompt context code.
 */
public class CharacterSheetNormalizer {
    private static final Gson GSON = new Gson();
    private static final String MISSING = "N/A";

    public static String normalize(String response) {
        if (response == null) {
            return "";
        }

        String trimmed = response.trim();
        JsonObject character = parseFirstJsonObject(trimmed);
        if (character == null) {
            String partial = normalizePartialCharacterJson(trimmed);
            return partial == null ? trimmed : partial;
        }
        if (!looksLikeCharacterJson(character)) {
            return trimmed;
        }

        return String.join("\n",
                "- Name: " + stringValue(character, "name"),
                "- Personality: " + stringValue(character, "personality"),
                "- Speaking Style / Tone: " + stringValue(character, "speaking_style"),
                "- Class: " + firstStringValue(character, "class_name", "class"),
                "- Skills: " + listValue(character, "skills"),
                "- Likes: " + listValue(character, "likes"),
                "- Dislikes: " + listValue(character, "dislikes"),
                "- Alignment: " + stringValue(character, "alignment"),
                "- Background: " + stringValue(character, "background"),
                "- Short Greeting: \"" + firstStringValue(character, "short_greeting", "greeting") + "\""
        );
    }

    private static boolean looksLikeCharacterJson(JsonObject object) {
        return object.has("name")
                && (object.has("short_greeting") || object.has("greeting"))
                && (object.has("speaking_style") || object.has("class_name") || object.has("personality"));
    }

    private static String normalizePartialCharacterJson(String input) {
        String name = extractJsonStringField(input, "name");
        String shortGreeting = firstExtractedString(input, "short_greeting", "greeting");
        if (MISSING.equals(name) || MISSING.equals(shortGreeting)) {
            return null;
        }

        return String.join("\n",
                "- Name: " + name,
                "- Personality: " + extractJsonStringField(input, "personality"),
                "- Speaking Style / Tone: " + extractJsonStringField(input, "speaking_style"),
                "- Class: " + firstExtractedString(input, "class_name", "class"),
                "- Skills: " + MISSING,
                "- Likes: " + MISSING,
                "- Dislikes: " + MISSING,
                "- Alignment: " + extractJsonStringField(input, "alignment"),
                "- Background: " + extractJsonStringField(input, "background"),
                "- Short Greeting: \"" + shortGreeting + "\""
        );
    }

    private static String firstExtractedString(String input, String... keys) {
        for (String key : keys) {
            String value = extractJsonStringField(input, key);
            if (!MISSING.equals(value)) {
                return value;
            }
        }
        return MISSING;
    }

    private static String extractJsonStringField(String input, String fieldName) {
        if (input == null || fieldName == null || fieldName.isEmpty()) {
            return MISSING;
        }

        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) {
            return MISSING;
        }

        try {
            String value = GSON.fromJson("\"" + matcher.group(1) + "\"", String.class);
            return clean(value);
        } catch (JsonSyntaxException ignored) {
            return MISSING;
        }
    }

    private static JsonObject parseFirstJsonObject(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        int start = -1;
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);

            if (start == -1) {
                if (current == '{') {
                    start = i;
                    depth = 1;
                }
                continue;
            }

            if (escaped) {
                escaped = false;
                continue;
            }

            if (inString && current == '\\') {
                escaped = true;
                continue;
            }

            if (current == '"') {
                inString = !inString;
                continue;
            }

            if (inString) {
                continue;
            }

            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    JsonObject parsed = parseJsonObject(input.substring(start, i + 1));
                    if (parsed != null) {
                        return parsed;
                    }
                    start = -1;
                }
            }
        }

        return null;
    }

    private static JsonObject parseJsonObject(String json) {
        try {
            JsonElement element = GSON.fromJson(json, JsonElement.class);
            if (element != null && element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        } catch (JsonSyntaxException ignored) {
            return null;
        }
        return null;
    }

    private static String firstStringValue(JsonObject object, String... keys) {
        for (String key : keys) {
            String value = stringValue(object, key);
            if (!MISSING.equals(value)) {
                return value;
            }
        }
        return MISSING;
    }

    private static String stringValue(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return MISSING;
        }

        JsonElement element = object.get(key);
        if (element.isJsonArray()) {
            return joinArray(element.getAsJsonArray());
        }
        if (!element.isJsonPrimitive()) {
            return MISSING;
        }

        String value = element.getAsString();
        return clean(value);
    }

    private static String listValue(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return MISSING;
        }

        JsonElement element = object.get(key);
        if (element.isJsonArray()) {
            return joinArray(element.getAsJsonArray());
        }
        if (element.isJsonPrimitive()) {
            return clean(element.getAsString());
        }
        return MISSING;
    }

    private static String joinArray(JsonArray array) {
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (element != null && element.isJsonPrimitive()) {
                String value = clean(element.getAsString());
                if (!MISSING.equals(value)) {
                    values.add(value);
                }
            }
        }
        return values.isEmpty() ? MISSING : String.join(", ", values);
    }

    private static String clean(String value) {
        if (value == null) {
            return MISSING;
        }
        String cleaned = value.trim().replace("\r", " ").replace("\n", " ");
        while (cleaned.contains("  ")) {
            cleaned = cleaned.replace("  ", " ");
        }
        return cleaned.isEmpty() ? MISSING : cleaned;
    }
}
