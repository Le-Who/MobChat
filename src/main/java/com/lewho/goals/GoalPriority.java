// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.goals;

public enum GoalPriority {
    TALK_PLAYER(2),
    PROTECT_PLAYER(2),
    LEAD_PLAYER(3),
    FOLLOW_PLAYER(3),
    FLEE_PLAYER(3),
    ATTACK_PLAYER(3),
    WAIT_HERE(4),
    RETURN_HOME(4),
    GUARD_HOME(4);

    private final int priority;

    GoalPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return this.priority;
    }
}
