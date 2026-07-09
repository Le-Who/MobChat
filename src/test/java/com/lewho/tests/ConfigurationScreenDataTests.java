// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.commands.ConfigurationHandler;
import com.lewho.commands.ConfigurationScreenData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigurationScreenDataTests {

    @Test
    public void openDataDoesNotExposeStoredApiKeys() {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setApiKey("sk-secret-1234567890,AI-secret-0987654321");
        config.setModel("gpt-4o-mini,gemini-3.5-flash");

        ConfigurationScreenData.OpenData data = ConfigurationScreenData.fromConfig(config);

        assertEquals("", data.apiKeys());
        assertTrue(data.maskedApiKeys().contains("2 key(s)"));
        assertFalse(data.maskedApiKeys().contains("secret"));
        assertEquals("gpt-4o-mini\ngemini-3.5-flash", data.models());
        assertEquals(config.getMaxOutputTokens(), data.maxOutputTokens());
    }

    @Test
    public void saveDraftKeepsExistingApiKeysWhenFieldIsBlank() {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setApiKey("existing-key");

        ConfigurationScreenData.SaveData draft = new ConfigurationScreenData.SaveData(
                "openai",
                "https://api.openai.com/v1/chat/completions",
                "   ",
                "gpt-4o-mini",
                10
        );

        ConfigurationScreenData.applyToConfig(config, draft);

        assertEquals("existing-key", config.getApiKey());
    }

    @Test
    public void saveDraftNormalizesMultilineKeysAndModels() {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();

        ConfigurationScreenData.SaveData draft = new ConfigurationScreenData.SaveData(
                "ai-studio",
                "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
                "key-one\nkey-two, key-three",
                "gemini-3.5-flash\ngemini-3.5-pro, gemini-custom",
                15,
                1024,
                "high"
        );

        ConfigurationScreenData.applyToConfig(config, draft);

        assertEquals("key-one,key-two,key-three", config.getApiKey());
        assertEquals("gemini-3.5-flash,gemini-3.5-pro,gemini-custom", config.getModel());
        assertEquals("high", config.getThinkingLevel());
        assertEquals(1024, config.getMaxOutputTokens());
        assertEquals(3, config.getApiKeyCount());
        assertEquals(3, config.getModelCount());
    }

    @Test
    public void openDataIncludesStoredThinkingLevel() {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setThinkingLevel("minimal");

        ConfigurationScreenData.OpenData data = ConfigurationScreenData.fromConfig(config);

        assertEquals("minimal", data.thinkingLevel());
    }

    @Test
    public void saveDraftRejectsTooSmallOutputTokenBudget() {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();

        ConfigurationScreenData.SaveData draft = new ConfigurationScreenData.SaveData(
                "custom",
                "https://api.openai.com/v1/chat/completions",
                "",
                "gpt-4o-mini",
                10,
                63,
                "auto"
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> ConfigurationScreenData.applyToConfig(config, draft)
        );
        assertTrue(error.getMessage().contains("Output tokens"));
    }

    @Test
    public void saveDraftRejectsMissingEndpointOrModel() {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();

        ConfigurationScreenData.SaveData draft = new ConfigurationScreenData.SaveData(
                "custom",
                "",
                "",
                "",
                10
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> ConfigurationScreenData.applyToConfig(config, draft)
        );
        assertTrue(error.getMessage().contains("URL"));
    }
}
