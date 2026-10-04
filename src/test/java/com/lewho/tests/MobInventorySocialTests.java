// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.chat.ChatDataManager;
import com.lewho.chat.EntityChatData;
import com.lewho.chat.PlayerData;
import com.lewho.inventory.MobInventoryMenu;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MobInventorySocialTests {
    @BeforeAll
    public static void bootstrapItems() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void takingOnlyDisarmedItemStillRecordsTheftAfterCountNormalization() {
        EntityChatData data = new EntityChatData("mob");
        PlayerData player = data.getPlayerData("player-uuid", "Alex");

        assertTrue(MobInventoryMenu.recordTakenItems(data, "player-uuid", "Alex",
                Map.of(), List.of("Diamond Sword")));

        assertEquals(1, player.harmfulActions);
        assertEquals(1, player.socialEventCount);
        assertEquals(ChatDataManager.ChatStatus.NONE, data.status);
    }

    @Test
    public void ordinaryRemovalAndDisarmTogetherCountAsOneInventoryEvent() {
        EntityChatData data = new EntityChatData("mob");
        PlayerData player = data.getPlayerData("player-uuid", "Alex");

        assertTrue(MobInventoryMenu.recordTakenItems(data, "player-uuid", "Alex",
                Map.of(Items.DIAMOND, 2), List.of("Diamond Sword")));

        assertEquals(1, player.harmfulActions);
        assertEquals(1, player.socialEventCount);
    }

    @Test
    public void movingDisarmedItemIntoMobInventoryDoesNotRecordTheft() {
        EntityChatData data = new EntityChatData("mob");
        PlayerData player = data.getPlayerData("player-uuid", "Alex");

        assertFalse(MobInventoryMenu.recordTakenItems(data, "player-uuid", "Alex", Map.of(), List.of()));

        assertEquals(0, player.harmfulActions);
        assertEquals(0, player.socialEventCount);
    }
}
