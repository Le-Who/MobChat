// SPDX-FileCopyrightText: 2026 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.tests;

import com.owlmaddie.commands.ConfigurationHandler;
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
}
