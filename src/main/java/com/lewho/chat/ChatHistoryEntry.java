// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.chat;

import com.lewho.message.MessageParser;
import com.lewho.message.ParsedMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public record ChatHistoryEntry(
        String message,
        ChatDataManager.ChatSender sender,
        String name,
        long timestamp
) {
    public static List<ChatHistoryEntry> fromMessages(List<ChatMessage> messages, int maxEntries) {
        if (messages == null || messages.isEmpty() || maxEntries <= 0) {
            return Collections.emptyList();
        }

        List<ChatHistoryEntry> entries = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0 && entries.size() < maxEntries; i--) {
            ChatMessage message = messages.get(i);
            if (message == null) {
                continue;
            }
            String displayMessage = toDisplayMessage(message);
            if (displayMessage == null || displayMessage.isEmpty()) {
                continue;
            }
            entries.add(new ChatHistoryEntry(
                    displayMessage,
                    message.sender == null ? ChatDataManager.ChatSender.ASSISTANT : message.sender,
                    message.name == null ? "" : message.name,
                    message.timestamp == null ? 0L : message.timestamp
            ));
        }
        Collections.reverse(entries);
        return entries;
    }

    private static String toDisplayMessage(ChatMessage message) {
        String rawMessage = message.message == null
                ? ""
                : message.message.replace('\n', ' ').replace('\r', ' ').trim();
        if (rawMessage.isEmpty() || isInternalConversationNote(rawMessage)) {
            return "";
        }

        ChatDataManager.ChatSender sender = message.sender == null
                ? ChatDataManager.ChatSender.ASSISTANT
                : message.sender;
        if (sender != ChatDataManager.ChatSender.ASSISTANT) {
            return rawMessage;
        }

        ParsedMessage parsed = MessageParser.parseMessage(rawMessage);
        String cleanedMessage = parsed.getCleanedMessage();
        if (!cleanedMessage.isEmpty()) {
            return cleanedMessage;
        }
        return isLikelyTechnicalAssistantMessage(rawMessage) ? "" : rawMessage;
    }

    private static boolean isInternalConversationNote(String message) {
        String normalized = message.toLowerCase(Locale.ENGLISH);
        return normalized.startsWith("<returning player:")
                || normalized.startsWith("<a new player has joined the conversation:");
    }

    private static boolean isLikelyTechnicalAssistantMessage(String message) {
        String normalized = message.toLowerCase(Locale.ENGLISH);
        return normalized.startsWith("{")
                || normalized.contains("\"message\"")
                || normalized.contains("\"actions\"")
                || normalized.contains("\"memory_updates\"")
                || normalized.contains("\"mood\"")
                || normalized.equals("here is the json requested")
                || normalized.equals("here is the json");
    }
}
