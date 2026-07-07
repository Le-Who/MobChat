// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.message;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@code MessageParser} class parses out behaviors that are included in messages, and outputs
 * a {@code ParsedMessage} result, which separates the cleaned message and the included behaviors.
 */
public class MessageParser {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private static final Gson GSON = new Gson();
    private static final Set<String> ALLOWED_BEHAVIORS = Set.of(
            "FOLLOW",
            "LEAD",
            "FLEE",
            "ATTACK",
            "PROTECT",
            "FRIENDSHIP",
            "UNFOLLOW",
            "UNLEAD",
            "UNPROTECT",
            "UNFLEE",
            "WAIT",
            "RETURN_HOME",
            "GUARD_HOME"
    );

    public static ParsedMessage parseMessage(String input) {
        LOGGER.debug("Parsing message: {}", input);
        ParsedMessage structured = parseStructuredMessage(input);
        if (structured != null) {
            return structured;
        }

        StringBuilder cleanedMessage = new StringBuilder();
        List<Behavior> behaviors = new ArrayList<>();
        Pattern pattern = Pattern.compile("[<*](FOLLOW|LEAD|FLEE|ATTACK|PROTECT|FRIENDSHIP|UNFOLLOW|UNLEAD|UNPROTECT|UNFLEE|WAIT|RETURN_HOME|GUARD_HOME)[:\\s]*(\\s*[+-]?\\d+)?[>*]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String behaviorName = matcher.group(1);
            Integer argument = null;
            if (matcher.group(2) != null) {
                argument = Integer.valueOf(matcher.group(2));
            }
            behaviors.add(new Behavior(behaviorName, argument));
            LOGGER.debug("Found behavior: {} with argument: {}", behaviorName, argument);

            matcher.appendReplacement(cleanedMessage, "");
        }
        matcher.appendTail(cleanedMessage);

        // Get final cleaned string
        String displayMessage = cleanedMessage.toString().trim();

        // Remove all occurrences of "<>" and "**" (if any)
        displayMessage = displayMessage.replaceAll("<>", "").replaceAll("\\*\\*", "").trim();
        LOGGER.debug("Cleaned message: {}", displayMessage);

        return new ParsedMessage(displayMessage, input.trim(), behaviors);
    }

    private static ParsedMessage parseStructuredMessage(String input) {
        if (input == null) {
            return null;
        }

        String trimmedInput = input.trim();
        if (trimmedInput.startsWith("{")) {
            ParsedMessage parsed = parseStructuredJson(trimmedInput, trimmedInput);
            if (parsed != null) {
                return parsed;
            }
            return new ParsedMessage("", "", new ArrayList<>());
        }

        if (isStructuredJsonBoilerplateOnly(trimmedInput)) {
            return new ParsedMessage("", "", new ArrayList<>());
        }

        ParsedMessage embedded = parseFirstStructuredJsonObject(trimmedInput);
        if (embedded != null) {
            return embedded;
        }
        if (isMalformedStructuredJsonAttempt(trimmedInput)) {
            return new ParsedMessage("", "", new ArrayList<>());
        }

        return null;
    }

    private static boolean isStructuredJsonBoilerplateOnly(String input) {
        if (input == null || input.contains("{")) {
            return false;
        }

        String normalized = input.trim()
                .toLowerCase(Locale.ENGLISH)
                .replace("```json", "")
                .replace("```", "")
                .replace(":", "")
                .replace(".", "")
                .trim();
        return normalized.equals("here is the json requested")
                || normalized.equals("here's the json requested")
                || normalized.equals("here is the json")
                || normalized.equals("here's the json")
                || normalized.equals("here is your json")
                || normalized.equals("here's your json")
                || normalized.equals("here is the requested json")
                || normalized.equals("here is the json request")
                || normalized.equals("here's the json request")
                || normalized.equals("here is your requested json")
                || normalized.equals("here's your requested json")
                || normalized.equals("the json")
                || normalized.equals("json")
                || normalized.equals("the requested json");
    }

    private static boolean isMalformedStructuredJsonAttempt(String input) {
        if (input == null || !input.contains("{")) {
            return false;
        }
        String normalized = input.toLowerCase(Locale.ENGLISH);
        return normalized.contains("\"message\"")
                || normalized.contains("\"actions\"")
                || normalized.contains("\"memory_updates\"")
                || normalized.contains("\"mood\"");
    }

    private static ParsedMessage parseStructuredJson(String json, String originalInput) {
        try {
            StructuredResponse response = GSON.fromJson(json, StructuredResponse.class);
            if (response == null || response.message == null || response.message.trim().isEmpty()) {
                return null;
            }

            List<Behavior> behaviors = new ArrayList<>();
            if (response.actions != null) {
                for (StructuredAction action : response.actions) {
                    if (action == null || action.type == null) {
                        continue;
                    }
                    String behaviorName = action.type.trim().toUpperCase(Locale.ENGLISH);
                    if (!ALLOWED_BEHAVIORS.contains(behaviorName)) {
                        LOGGER.warn("Ignoring unsupported structured behavior: {}", action.type);
                        continue;
                    }
                    Integer argument = action.value != null ? action.value : action.argument;
                    behaviors.add(new Behavior(behaviorName, argument));
                }
            }

            List<String> memoryUpdates = new ArrayList<>();
            if (response.memory_updates != null) {
                for (String memory : response.memory_updates) {
                    if (memory != null && !memory.trim().isEmpty()) {
                        memoryUpdates.add(memory.trim());
                    }
                }
            }

            return new ParsedMessage(response.message.trim(), originalInput.trim(), behaviors, response.mood, memoryUpdates);
        } catch (JsonSyntaxException e) {
            LOGGER.debug("Structured message parse failed; falling back to legacy parser", e);
            return null;
        }
    }

    private static ParsedMessage parseFirstStructuredJsonObject(String input) {
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
                    ParsedMessage parsed = parseStructuredJson(input.substring(start, i + 1), input);
                    if (parsed != null) {
                        return parsed;
                    }
                    start = -1;
                }
            }
        }

        return null;
    }

    private static class StructuredResponse {
        String message;
        String mood;
        List<String> memory_updates;
        List<StructuredAction> actions;
    }

    private static class StructuredAction {
        String type;
        Integer value;
        Integer argument;
    }
}
