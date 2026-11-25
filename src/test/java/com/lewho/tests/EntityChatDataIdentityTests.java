// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.chat.EntityChatData;
import com.lewho.chat.PlayerData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    public void loginProjectionPreservesLegacyEntryForLaterUuidMigration() {
        EntityChatData data = new EntityChatData("entity-id");
        PlayerData legacy = new PlayerData();
        legacy.friendship = 2;
        data.players.put("Alex", legacy);

        assertEquals(0, data.toLightVersion("player-uuid").players.get("player-uuid").friendship);
        assertFalse(data.players.containsKey("player-uuid"));

        assertSame(legacy, data.getPlayerData("player-uuid", "Alex"));
        assertEquals(2, data.toLightVersion("player-uuid").players.get("player-uuid").friendship);
    }

    @Test
    public void loginProjectionKeepsTheLegacyBlankPlayerFallback() {
        EntityChatData data = new EntityChatData("entity-id");
        PlayerData legacy = new PlayerData();
        legacy.friendship = 2;
        data.players.put("", legacy);

        assertEquals(2, data.toLightVersion("player-uuid").players.get("player-uuid").friendship);
        assertFalse(data.players.containsKey("player-uuid"));
    }

    @Test
    public void describeMaturityUsesAgeableBabyState() {
        assertTrue(EntityChatData.describeMaturity(true).equals("Baby"));
        assertTrue(EntityChatData.describeMaturity(false).equals("Adult"));
    }
}
