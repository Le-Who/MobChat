// SPDX-FileCopyrightText: 2026 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.tests;

import com.owlmaddie.chat.CharacterSheetNormalizer;
import com.owlmaddie.chat.EntityChatData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EntityChatDataCharacterSheetTests {

    @Test
    public void shortGreetingFallsBackWhenPropertyIsMissing() {
        EntityChatData data = new EntityChatData("entity-id");
        data.characterSheet = """
                - Name: Yen
                - Personality: Stoic
                - Speaking Style / Tone: Calm
                """;

        assertEquals("Hello there.", data.getShortGreetingOrFallback("Hello there."));
    }

    @Test
    public void shortGreetingUsesGreetingAliasWhenModelOmitsShortPrefix() {
        EntityChatData data = new EntityChatData("entity-id");
        data.characterSheet = """
                - Name: Yen
                - Greeting: "Stand steady. Speak clearly."
                """;

        assertEquals("Stand steady. Speak clearly.", data.getShortGreetingOrFallback("Hello there."));
    }

    @Test
    public void structuredCharacterJsonNormalizesToLegacyCharacterSheet() {
        String normalized = CharacterSheetNormalizer.normalize("""
                {
                  "name": "Ysolara-Keth",
                  "personality": "Principled, severe, quietly protective",
                  "speaking_style": "Stoic and formal",
                  "class_name": "Villain",
                  "skills": ["strategy", "contract law"],
                  "likes": ["order", "clear terms"],
                  "dislikes": ["chaos", "broken oaths"],
                  "alignment": "Lawful Neutral",
                  "background": "exiled magistrate",
                  "short_greeting": "State your business plainly."
                }
                """);

        EntityChatData data = new EntityChatData("entity-id");
        data.characterSheet = normalized;

        assertTrue(normalized.contains("- Speaking Style / Tone: Stoic and formal"));
        assertTrue(normalized.contains("- Class: Villain"));
        assertEquals("Ysolara-Keth", data.getCharacterProp("Name"));
        assertEquals("strategy, contract law", data.getCharacterProp("Skills"));
        assertEquals("State your business plainly.", data.getShortGreetingOrFallback("Hello there."));
    }

    @Test
    public void legacyCharacterSheetIsPreservedWhenResponseIsNotJson() {
        String legacySheet = """
                - Name: Yen
                - Personality: Stoic
                - Short Greeting: "Hold your ground."
                """;

        assertEquals(legacySheet.trim(), CharacterSheetNormalizer.normalize(legacySheet));
    }
}
