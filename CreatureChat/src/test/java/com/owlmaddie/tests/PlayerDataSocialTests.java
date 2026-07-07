// SPDX-FileCopyrightText: 2026 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.tests;

import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.chat.SocialEventType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class PlayerDataSocialTests {

    @Test
    public void socialEventsUpdateReputationWithoutGeneratingMessages() {
        PlayerData data = new PlayerData();

        data.recordSocialEvent(SocialEventType.GIFT_GIVEN, "Player gave useful food.");
        data.recordSocialEvent(SocialEventType.DAMAGE_DEALT, "Player hit me.");

        assertEquals(0, data.socialReputation);
        assertEquals(1, data.helpfulActions);
        assertEquals(1, data.harmfulActions);
        assertEquals(2, data.socialEventCount);
        assertFalse(data.socialSummary.isEmpty());
    }

    @Test
    public void friendshipChangesContributeToLocalReputation() {
        PlayerData data = new PlayerData();

        data.recordFriendshipShift(0, 2);
        data.recordFriendshipShift(2, -1);

        assertEquals(-1, data.socialReputation);
        assertEquals(1, data.helpfulActions);
        assertEquals(1, data.harmfulActions);
    }
}
