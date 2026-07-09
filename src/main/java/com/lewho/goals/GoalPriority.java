// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.goals;

/**
 * The {@code GoalPriority} enum sets the priorities of each type of custom Goal used in this mod.
 * For example, talking to a player is higher priority than following a player.
 */
public enum GoalPriority {
    // Enum constants (Goal Types) with their corresponding priority values
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

    // Constructor for the enum to set the priority value
    GoalPriority(int priority) {
        this.priority = priority;
    }

    // Getter method to access the priority value
    public int getPriority() {
        return this.priority;
    }
}
