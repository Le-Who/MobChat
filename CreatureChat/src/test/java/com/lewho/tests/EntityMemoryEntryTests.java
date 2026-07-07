// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.chat.EntityChatData;
import com.lewho.chat.MemoryEntry;
import com.lewho.chat.MemoryType;
import com.lewho.message.ParsedMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class EntityMemoryEntryTests {

    @Test
    public void applyStructuredMetadataCreatesTypedMemoryEntries() {
        EntityChatData data = new EntityChatData("entity-id");
        ParsedMessage parsed = new ParsedMessage(
                "Thanks",
                "{\"message\":\"Thanks\"}",
                new ArrayList<>(),
                "grateful",
                List.of("Player gave me a golden apple.")
        );

        data.applyStructuredMetadata(parsed);

        assertEquals(1, data.memoryEntries.size());
        MemoryEntry entry = data.memoryEntries.get(0);
        assertEquals(MemoryType.GIFT, entry.type);
        assertEquals("Player gave me a golden apple.", entry.text);
        assertEquals(1, entry.salience);
        assertFalse(entry.createdAt <= 0);
    }

    @Test
    public void typedMemoryEntriesReplaceDuplicatesAndKeepLegacyMemoryStringsInSync() {
        EntityChatData data = new EntityChatData("entity-id");
        List<String> updates = List.of(
                "Player promised me a present.",
                "Player promised me a present."
        );

        data.applyStructuredMetadata(new ParsedMessage("ok", "ok", new ArrayList<>(), "", updates));

        assertEquals(1, data.memoryEntries.size());
        assertEquals(1, data.memories.size());
        assertEquals("Player promised me a present.", data.memories.get(0));
        assertEquals(MemoryType.PROMISE, data.memoryEntries.get(0).type);
    }

    @Test
    public void rememberRumorStoresTypedRumorWithoutChangingCurrentMessage() {
        EntityChatData data = new EntityChatData("entity-id");

        data.rememberRumor("Nearby sheep said the player hides gold.");

        assertEquals(1, data.memoryEntries.size());
        assertEquals(MemoryType.RUMOR, data.memoryEntries.get(0).type);
        assertEquals("", data.currentMessage);
    }
}
