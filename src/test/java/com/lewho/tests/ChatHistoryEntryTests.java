// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.chat.ChatDataManager;
import com.lewho.chat.ChatHistoryEntry;
import com.lewho.chat.ChatMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChatHistoryEntryTests {

    @Test
    public void fromMessagesReturnsMostRecentEntriesInReadingOrder() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("old user message", ChatDataManager.ChatSender.USER, "Alex"));
        messages.add(new ChatMessage("old npc reply", ChatDataManager.ChatSender.ASSISTANT, "Alex"));
        messages.add(new ChatMessage("recent user message", ChatDataManager.ChatSender.USER, "Sam"));
        messages.add(new ChatMessage("recent npc reply", ChatDataManager.ChatSender.ASSISTANT, "Sam"));

        List<ChatHistoryEntry> entries = ChatHistoryEntry.fromMessages(messages, 2);

        assertEquals(2, entries.size());
        assertEquals("recent user message", entries.get(0).message());
        assertEquals(ChatDataManager.ChatSender.USER, entries.get(0).sender());
        assertEquals("Sam", entries.get(0).name());
        assertEquals("recent npc reply", entries.get(1).message());
        assertEquals(ChatDataManager.ChatSender.ASSISTANT, entries.get(1).sender());
        assertEquals("Sam", entries.get(1).name());
    }

    @Test
    public void fromMessagesShowsStructuredAssistantMessageInsteadOfJson() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("Не подходи ко мне", ChatDataManager.ChatSender.USER, "danoner1397"));
        messages.add(new ChatMessage("""
                {
                  "message": "Сам хотел это предложить. Не буду приближаться.",
                  "mood": "indifferent",
                  "memory_updates": [],
                  "actions": []
                }
                """, ChatDataManager.ChatSender.ASSISTANT, "danoner1397"));

        List<ChatHistoryEntry> entries = ChatHistoryEntry.fromMessages(messages, 10);

        assertEquals(2, entries.size());
        assertEquals("Сам хотел это предложить. Не буду приближаться.", entries.get(1).message());
    }

    @Test
    public void fromMessagesShowsLegacyAssistantMessageWithoutBehaviorTags() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("Follow me", ChatDataManager.ChatSender.USER, "Alex"));
        messages.add(new ChatMessage("Sure, I'll follow. <FOLLOW>", ChatDataManager.ChatSender.ASSISTANT, "Alex"));

        List<ChatHistoryEntry> entries = ChatHistoryEntry.fromMessages(messages, 10);

        assertEquals(2, entries.size());
        assertEquals("Sure, I'll follow.", entries.get(1).message());
    }

    @Test
    public void fromMessagesHidesInternalConversationNotes() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("Hello", ChatDataManager.ChatSender.USER, "Alex"));
        messages.add(new ChatMessage("<returning player: Sam resumes the conversation>", ChatDataManager.ChatSender.USER, "Sam"));
        messages.add(new ChatMessage("Привет", ChatDataManager.ChatSender.USER, "Sam"));
        messages.add(new ChatMessage("Рад снова тебя видеть.", ChatDataManager.ChatSender.ASSISTANT, "Sam"));

        List<ChatHistoryEntry> entries = ChatHistoryEntry.fromMessages(messages, 10);

        assertEquals(3, entries.size());
        assertEquals("Hello", entries.get(0).message());
        assertEquals("Привет", entries.get(1).message());
        assertEquals("Рад снова тебя видеть.", entries.get(2).message());
    }
}
