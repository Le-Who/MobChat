// SPDX-FileCopyrightText: 2026 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.tests;

import com.owlmaddie.chat.BehaviorPolicy;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.message.Behavior;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BehaviorPolicyTests {

    @Test
    public void ambientContextCannotExecutePhysicalActionsOrFriendshipChanges() {
        PlayerData playerData = new PlayerData();
        List<Behavior> filtered = BehaviorPolicy.filterAllowed(
                List.of(
                        new Behavior("FOLLOW", null),
                        new Behavior("FRIENDSHIP", 3),
                        new Behavior("ATTACK", null)
                ),
                playerData,
                true,
                false
        );

        assertTrue(filtered.isEmpty());
    }

    @Test
    public void directAttackIsBlockedWhileFriendshipIsPositive() {
        PlayerData playerData = new PlayerData();
        playerData.friendship = 2;

        List<Behavior> filtered = BehaviorPolicy.filterAllowed(
                List.of(new Behavior("ATTACK", null)),
                playerData,
                false,
                false
        );

        assertTrue(filtered.isEmpty());
    }

    @Test
    public void directAttackCanFollowSameResponseHostileFriendshipShift() {
        PlayerData playerData = new PlayerData();
        playerData.friendship = 2;

        List<Behavior> filtered = BehaviorPolicy.filterAllowed(
                List.of(
                        new Behavior("FRIENDSHIP", -1),
                        new Behavior("ATTACK", null)
                ),
                playerData,
                false,
                false
        );

        assertEquals(2, filtered.size());
        assertEquals("FRIENDSHIP", filtered.get(0).getName());
        assertEquals("ATTACK", filtered.get(1).getName());
    }

    @Test
    public void homeActionsRequireBestFriendOrExistingHome() {
        PlayerData playerData = new PlayerData();

        List<Behavior> blocked = BehaviorPolicy.filterAllowed(
                List.of(new Behavior("RETURN_HOME", null)),
                playerData,
                false,
                false
        );
        assertTrue(blocked.isEmpty());

        playerData.friendship = 3;
        List<Behavior> allowed = BehaviorPolicy.filterAllowed(
                List.of(new Behavior("RETURN_HOME", null)),
                playerData,
                false,
                false
        );
        assertEquals(1, allowed.size());
    }
}
