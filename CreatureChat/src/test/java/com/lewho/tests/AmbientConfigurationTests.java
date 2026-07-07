// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.commands.ConfigurationHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AmbientConfigurationTests {

    @Test
    public void ambientConversationDefaultsAreConservative() {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();

        assertTrue(config.getProximityChatEnabled());
        assertEquals(12, config.getProximityChatRadius());
        assertEquals(1, config.getMaxProximityResponsesPerMessage());
        assertEquals(1, config.getMaxPlayerAmbientResponses());
        assertEquals(45, config.getPlayerAmbientCooldownSeconds());
        assertEquals(1, config.getMaxEntityAmbientResponses());
        assertEquals(90, config.getEntityAmbientCooldownSeconds());
        assertEquals(25, config.getDamageReactionCooldownSeconds());
        assertTrue(config.getGeminiUsageLimitsEnabled());
        assertEquals(14, config.getGeminiRequestsPerMinute());
        assertEquals(450, config.getGeminiRequestsPerDay());
        assertEquals("per_key", config.getGeminiUsageLimitScope());
        assertTrue(config.getMobToMobChatEnabled());
        assertEquals(10, config.getMobToMobChatRadius());
        assertEquals(1, config.getMaxMobToMobResponsesPerMessage());
    }
}
