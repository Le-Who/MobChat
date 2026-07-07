// SPDX-FileCopyrightText: 2026 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.tests;

import com.owlmaddie.message.ParsedMessage;
import com.owlmaddie.message.MessageParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StructuredResponseParserTests {

    @Test
    public void parseStructuredResponseAsMessageAndBehaviors() {
        String response = """
                {
                  "message": "Пошли, я за тобой!",
                  "actions": [
                    { "type": "FOLLOW" },
                    { "type": "FRIENDSHIP", "value": 2 }
                  ]
                }
                """;

        ParsedMessage parsed = MessageParser.parseMessage(response);

        assertEquals("Пошли, я за тобой!", parsed.getCleanedMessage());
        assertEquals(2, parsed.getBehaviors().size());
        assertEquals("FOLLOW", parsed.getBehaviors().get(0).getName());
        assertEquals(null, parsed.getBehaviors().get(0).getArgument());
        assertEquals("FRIENDSHIP", parsed.getBehaviors().get(1).getName());
        assertEquals(2, parsed.getBehaviors().get(1).getArgument());
    }

    @Test
    public void parseStructuredMoodAndMemoryUpdates() {
        String response = """
                {
                  "message": "Я запомню это.",
                  "mood": "grateful",
                  "memory_updates": [
                    "Player gave me a golden apple.",
                    "Player asked me to guard their home."
                  ],
                  "actions": []
                }
                """;

        ParsedMessage parsed = MessageParser.parseMessage(response);

        assertEquals("grateful", parsed.getMood());
        assertEquals(2, parsed.getMemoryUpdates().size());
        assertEquals("Player gave me a golden apple.", parsed.getMemoryUpdates().get(0));
    }

    @Test
    public void parseStructuredResponseWrappedInModelPreamble() {
        String response = """
                Here is the JSON requested:
                ```json
                {
                  "message": "Йоу! Чем займемся?",
                  "mood": "friendly",
                  "memory_updates": [],
                  "actions": []
                }
                ```
                """;

        ParsedMessage parsed = MessageParser.parseMessage(response);

        assertEquals("Йоу! Чем займемся?", parsed.getCleanedMessage());
        assertEquals("friendly", parsed.getMood());
        assertTrue(parsed.getMemoryUpdates().isEmpty());
        assertTrue(parsed.getBehaviors().isEmpty());
    }

    @Test
    public void preambleOnlyStructuredResponseIsSuppressed() {
        ParsedMessage parsed = MessageParser.parseMessage("Here is the JSON requested");

        assertEquals("", parsed.getCleanedMessage());
        assertEquals("", parsed.getOriginalMessage());
        assertTrue(parsed.getBehaviors().isEmpty());
    }

    @Test
    public void legacyResponseHasNoStructuredMetadata() {
        ParsedMessage parsed = MessageParser.parseMessage("Okay, I'll follow. <FOLLOW>");

        assertEquals("", parsed.getMood());
        assertTrue(parsed.getMemoryUpdates().isEmpty());
    }

    @Test
    public void parseHomeAndWaitStructuredActions() {
        String response = """
                {
                  "message": "I'll hold this place.",
                  "mood": "focused",
                  "memory_updates": [],
                  "actions": [
                    { "type": "WAIT" },
                    { "type": "RETURN_HOME" },
                    { "type": "GUARD_HOME" }
                  ]
                }
                """;

        ParsedMessage parsed = MessageParser.parseMessage(response);

        assertEquals(3, parsed.getBehaviors().size());
        assertEquals("WAIT", parsed.getBehaviors().get(0).getName());
        assertEquals("RETURN_HOME", parsed.getBehaviors().get(1).getName());
        assertEquals("GUARD_HOME", parsed.getBehaviors().get(2).getName());
    }
}
