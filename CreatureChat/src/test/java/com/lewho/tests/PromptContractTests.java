// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PromptContractTests {

    @Test
    public void chatPromptAllowsAwakenedAnimalsToSpeak() throws IOException {
        String prompt = normalizedPrompt("system-chat");

        assertTrue(prompt.contains("awakened roleplay character"));
        assertTrue(prompt.contains("do not answer with only animal noises"));
        assertTrue(prompt.contains("brief animal sound is allowed only if"));
    }

    @Test
    public void chatPromptRequiresSpokenDirectReplies() throws IOException {
        String prompt = normalizedPrompt("system-chat");

        assertTrue(prompt.contains("direct player messages are addressed to you"));
        assertTrue(prompt.contains("answer greetings and friendship attempts with a spoken sentence"));
    }

    @Test
    public void characterPromptMakesAnimalSoundsFlavorOnly() throws IOException {
        String prompt = normalizedPrompt("system-character");

        assertTrue(prompt.contains("awakened roleplay character"));
        assertTrue(prompt.contains("animal sounds and physical traits are flavor"));
        assertTrue(prompt.contains("not only an animal noise"));
        assertTrue(prompt.contains("brief animal sound is allowed only if"));
    }

    private static String normalizedPrompt(String name) throws IOException {
        return readPrompt(name).toLowerCase().replaceAll("\\s+", " ");
    }

    private static String readPrompt(String name) throws IOException {
        return Files.readString(Path.of("src/main/resources/data/creaturechat/prompts", name));
    }
}
