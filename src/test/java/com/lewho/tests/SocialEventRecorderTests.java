// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.chat.ChatDataManager;
import com.lewho.chat.EntityChatData;
import com.lewho.chat.PlayerData;
import com.lewho.chat.SocialEventRecorder;
import com.lewho.chat.SocialEventType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class SocialEventRecorderTests {

    @Test
    public void recorderUpdatesPlayerDataWithoutChangingChatStatus() {
        EntityChatData chatData = new EntityChatData("entity-id");

        PlayerData data = SocialEventRecorder.record(chatData, "player-uuid", "Alex",
                SocialEventType.GIFT_GIVEN, "Player gave useful food.");

        assertSame(data, chatData.players.get("player-uuid"));
        assertEquals(1, data.socialReputation);
        assertEquals(1, data.helpfulActions);
        assertEquals(ChatDataManager.ChatStatus.NONE, chatData.status);
        assertEquals("", chatData.currentMessage);
    }
}
