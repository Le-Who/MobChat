// SPDX-FileCopyrightText: 2026 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.tests;

import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.message.ParsedMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EntityChatDataMetadataTests {

    @Test
    public void applyStructuredMetadataStoresMoodAndMemories() {
        EntityChatData data = new EntityChatData("entity-id");
        ParsedMessage parsed = new ParsedMessage(
                "Thanks",
                "{\"message\":\"Thanks\"}",
                new ArrayList<>(),
                "grateful",
                List.of("Player gave me a golden apple.")
        );

        data.applyStructuredMetadata(parsed);

        assertEquals("grateful", data.mood);
        assertEquals(1, data.memories.size());
        assertEquals("Player gave me a golden apple.", data.memories.get(0));
    }

    @Test
    public void applyStructuredMetadataCapsMemoryCount() {
        EntityChatData data = new EntityChatData("entity-id");
        List<String> updates = new ArrayList<>();
        for (int i = 0; i < EntityChatData.MAX_MEMORY_ENTRIES + 5; i++) {
            updates.add("memory-" + i);
        }

        data.applyStructuredMetadata(new ParsedMessage("ok", "ok", new ArrayList<>(), "", updates));

        assertEquals(EntityChatData.MAX_MEMORY_ENTRIES, data.memories.size());
        assertEquals("memory-5", data.memories.get(0));
    }
}
