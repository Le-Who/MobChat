// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.chat.ChatDataManager;
import com.lewho.chat.EntityChatData;
import com.lewho.commands.ConfigurationHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AmbientRateLimitTests {

    @BeforeEach
    public void clearManagerState() {
        ChatDataManager.getServerInstance().clearData();
    }

    @Test
    public void playerAmbientBucketBlocksImmediateSecondMobResponse() {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setMaxPlayerAmbientResponses(1);
        config.setPlayerAmbientCooldownSeconds(60);
        config.setMaxEntityAmbientResponses(10);
        config.setEntityAmbientCooldownSeconds(60);

        UUID playerId = UUID.randomUUID();
        EntityChatData firstMob = new EntityChatData(UUID.randomUUID().toString());
        EntityChatData secondMob = new EntityChatData(UUID.randomUUID().toString());

        assertTrue(ChatDataManager.getServerInstance().handleAmbientResponse(firstMob, playerId, "Alex", config));
        assertFalse(ChatDataManager.getServerInstance().handleAmbientResponse(secondMob, playerId, "Alex", config));
    }

    @Test
    public void entityAmbientBucketBlocksImmediateSecondPlayerResponse() {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setMaxPlayerAmbientResponses(10);
        config.setPlayerAmbientCooldownSeconds(60);
        config.setMaxEntityAmbientResponses(1);
        config.setEntityAmbientCooldownSeconds(60);

        EntityChatData mob = new EntityChatData(UUID.randomUUID().toString());

        assertTrue(ChatDataManager.getServerInstance().handleAmbientResponse(mob, UUID.randomUUID(), "Alex", config));
        assertFalse(ChatDataManager.getServerInstance().handleAmbientResponse(mob, UUID.randomUUID(), "Sam", config));
    }

    @Test
    public void entityOnlyAmbientBucketCanThrottleMobToMobSource() {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setMaxEntityAmbientResponses(1);
        config.setEntityAmbientCooldownSeconds(60);

        EntityChatData sourceMob = new EntityChatData(UUID.randomUUID().toString());

        assertTrue(ChatDataManager.getServerInstance().handleAmbientEntityResponse(sourceMob, config));
        assertFalse(ChatDataManager.getServerInstance().handleAmbientEntityResponse(sourceMob, config));
    }
}
