// SPDX-FileCopyrightText: 2026 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.tests;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.commands.ConfigurationHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DamageReactionRateLimitTests {

    @BeforeEach
    public void clearManagerState() {
        ChatDataManager.getServerInstance().clearData();
    }

    @Test
    public void firstDamageReactionIsAllowedThenImmediateSecondIsSuppressed() {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setDamageReactionCooldownSeconds(30);
        EntityChatData mob = new EntityChatData(UUID.randomUUID().toString());
        PlayerData playerData = mob.getPlayerData("player-uuid", "Alex");

        assertTrue(ChatDataManager.getServerInstance().handleDamageReaction(mob, playerData, "Alex", config));
        assertFalse(ChatDataManager.getServerInstance().handleDamageReaction(mob, playerData, "Alex", config));
        assertEquals(1, playerData.suppressedDamageReactionCount);
    }

    @Test
    public void damageReactionIsAllowedAgainAfterCooldownAndKeepsSuppressedHitsUntilConsumed() {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setDamageReactionCooldownSeconds(30);
        EntityChatData mob = new EntityChatData(UUID.randomUUID().toString());
        PlayerData playerData = mob.getPlayerData("player-uuid", "Alex");

        assertTrue(ChatDataManager.getServerInstance().handleDamageReaction(mob, playerData, "Alex", config));
        assertFalse(ChatDataManager.getServerInstance().handleDamageReaction(mob, playerData, "Alex", config));
        playerData.lastDamageReactionAt -= 31_000L;

        assertTrue(ChatDataManager.getServerInstance().handleDamageReaction(mob, playerData, "Alex", config));
        assertEquals(1, playerData.suppressedDamageReactionCount);
        String summary = playerData.consumeSuppressedDamageReactionSummary();
        assertTrue(summary.contains("1 additional hit"));
        assertEquals(0, playerData.suppressedDamageReactionCount);
    }

    @Test
    public void directPlayerChatResetsDamageReactionCooldown() {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setDamageReactionCooldownSeconds(60);
        EntityChatData mob = new EntityChatData(UUID.randomUUID().toString());
        PlayerData playerData = mob.getPlayerData("player-uuid", "Alex");

        assertTrue(ChatDataManager.getServerInstance().handleDamageReaction(mob, playerData, "Alex", config));
        assertFalse(ChatDataManager.getServerInstance().handleDamageReaction(mob, playerData, "Alex", config));

        ChatDataManager.getServerInstance().resetReactiveCooldowns(mob, playerData);

        assertTrue(ChatDataManager.getServerInstance().handleDamageReaction(mob, playerData, "Alex", config));
    }
}
