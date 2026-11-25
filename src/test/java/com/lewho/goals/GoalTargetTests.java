// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.goals;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class GoalTargetTests {
    private static final UUID PLAYER = UUID.fromString("0fdf0228-ae43-4865-8386-0224454303a1");
    private static final UUID OTHER_PLAYER = UUID.fromString("e76e33ef-5b14-4f63-95b2-67a0b8f978e4");

    private final Map<UUID, TestPlayer> roster = new HashMap<>();
    private TestLevel ownerLevel = TestLevel.OVERWORLD;
    private final GoalTarget<TestPlayer> target = GoalTarget.trackPlayer(
            PLAYER, roster::get, player -> player.alive && player.level == ownerLevel);

    @Test
    void disconnectPausesEvenIfTheOldEntityIsAliveAndReconnectUsesTheNewEntity() {
        TestPlayer original = new TestPlayer(true, TestLevel.OVERWORLD);
        roster.put(PLAYER, original);
        assertSame(original, target.resolve());

        roster.remove(PLAYER);
        roster.put(OTHER_PLAYER, new TestPlayer(true, TestLevel.OVERWORLD));
        assertNull(target.resolve());

        TestPlayer replacement = new TestPlayer(true, TestLevel.OVERWORLD);
        roster.put(PLAYER, replacement);
        assertSame(replacement, target.resolve());
    }

    @Test
    void deathAndRespawnInAnotherDimensionPauseUntilThePlayerReturns() {
        TestPlayer original = new TestPlayer(true, TestLevel.OVERWORLD);
        roster.put(PLAYER, original);
        assertSame(original, target.resolve());

        original.alive = false;
        assertNull(target.resolve());

        TestPlayer respawned = new TestPlayer(true, TestLevel.NETHER);
        roster.put(PLAYER, respawned);
        assertNull(target.resolve());

        respawned.level = TestLevel.OVERWORLD;
        assertSame(respawned, target.resolve());
    }

    @Test
    void changingDimensionOnTheSameEntityPausesUntilTheMobAndPlayerShareALevel() {
        TestPlayer player = new TestPlayer(true, TestLevel.OVERWORLD);
        roster.put(PLAYER, player);
        assertSame(player, target.resolve());

        player.level = TestLevel.NETHER;
        assertNull(target.resolve());

        ownerLevel = TestLevel.NETHER;
        assertSame(player, target.resolve());
    }

    private enum TestLevel { OVERWORLD, NETHER }

    private static final class TestPlayer {
        private boolean alive;
        private TestLevel level;

        private TestPlayer(boolean alive, TestLevel level) {
            this.alive = alive;
            this.level = level;
        }
    }
}
