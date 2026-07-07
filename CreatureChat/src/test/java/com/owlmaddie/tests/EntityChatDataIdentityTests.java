// SPDX-FileCopyrightText: 2026 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.tests;

import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EntityChatDataIdentityTests {

    @Test
    public void getPlayerDataMigratesLegacyNameEntryToStableUuidKey() {
        EntityChatData data = new EntityChatData("entity-id");
        PlayerData legacy = new PlayerData();
        legacy.friendship = 2;
        data.players.put("Alex", legacy);

        PlayerData migrated = data.getPlayerData("player-uuid", "Alex");

        assertSame(legacy, migrated);
        assertTrue(data.players.containsKey("player-uuid"));
        assertFalse(data.players.containsKey("Alex"));
    }

    @Test
    public void describeMaturityUsesAgeableBabyState() {
        assertTrue(EntityChatData.describeMaturity(true).equals("Baby"));
        assertTrue(EntityChatData.describeMaturity(false).equals("Adult"));
    }
}
