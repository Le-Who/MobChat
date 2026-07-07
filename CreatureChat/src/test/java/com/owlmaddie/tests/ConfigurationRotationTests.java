// SPDX-FileCopyrightText: 2026 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.tests;

import com.owlmaddie.commands.ConfigurationHandler;
import com.owlmaddie.commands.ConfigurationPresets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConfigurationRotationTests {

    @Test
    public void testApiKeyRotation() {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        
        // Single key behavior
        config.setApiKey("cc_singlekey");
        assertEquals(1, config.getApiKeyCount());
        assertEquals("cc_singlekey", config.getActiveApiKey());
        
        // Multiple keys behavior
        config.setApiKey("sk-key1,sk-key2,sk-key3");
        assertEquals(3, config.getApiKeyCount());
        
        // Start at first key
        assertEquals("sk-key1", config.getActiveApiKey());
        
        // Rotate to second key
        config.rotateApiKey();
        assertEquals("sk-key2", config.getActiveApiKey());
        
        // Rotate to third key
        config.rotateApiKey();
        assertEquals("sk-key3", config.getActiveApiKey());
        
        // Wrap around back to first key
        config.rotateApiKey();
        assertEquals("sk-key1", config.getActiveApiKey());
    }

    @Test
    public void testApiKeyRotationWithWhitespace() {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setApiKey(" sk-key1 ,  sk-key2 , sk-key3  ");
        assertEquals(3, config.getApiKeyCount());
        assertEquals("sk-key1", config.getActiveApiKey());
        
        config.rotateApiKey();
        assertEquals("sk-key2", config.getActiveApiKey());
        
        config.rotateApiKey();
        assertEquals("sk-key3", config.getActiveApiKey());
    }

    @Test
    public void testOpenRouterKeyDoesNotResetPresetEndpoint() {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setUrl("https://openrouter.ai/api/v1/chat/completions");

        config.setApiKey("sk-or-v1-secret");

        assertEquals("https://openrouter.ai/api/v1/chat/completions", config.getUrl());
    }

    @Test
    public void testModelRotationWithWhitespace() {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setModel(" gemini-2.5-flash , gpt-4o-mini , llama-3.1-8b-instant ");

        assertEquals(3, config.getModelCount());
        assertEquals("gemini-2.5-flash", config.getActiveModel());

        config.rotateModel();
        assertEquals("gpt-4o-mini", config.getActiveModel());

        config.rotateModel();
        assertEquals("llama-3.1-8b-instant", config.getActiveModel());

        config.rotateModel();
        assertEquals("gemini-2.5-flash", config.getActiveModel());
    }

    @Test
    public void testAiStudioPresetUsesOpenAiCompatibleEndpoint() {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setMaxOutputTokens(200);
        ConfigurationPresets.ProviderPreset preset = ConfigurationPresets.find("ai-studio").orElseThrow();

        ConfigurationPresets.applyPreset(config, preset);

        assertEquals("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions", config.getUrl());
        assertEquals(preset.defaultModel(), config.getActiveModel());
        assertEquals("gemini-3.1-flash-lite", config.getActiveModel());
        assertEquals("minimal", config.getThinkingLevel());
        assertEquals(ConfigurationHandler.Config.DEFAULT_MAX_OUTPUT_TOKENS, config.getMaxOutputTokens());

        config.setModel("gemini-custom-exact, gemini-another-exact");
        assertEquals(2, config.getModelCount());
        assertEquals("gemini-custom-exact", config.getActiveModel());
    }

    @Test
    public void testApiKeyMaskDoesNotExposeSecrets() {
        String masked = ConfigurationPresets.describeApiKeys("sk-secret-1234567890, AIza-secret-0987654321");

        assertTrue(masked.contains("2 key(s)"));
        assertFalse(masked.contains("secret"));
        assertFalse(masked.contains("1234567890"));
        assertFalse(masked.contains("0987654321"));
    }
}
