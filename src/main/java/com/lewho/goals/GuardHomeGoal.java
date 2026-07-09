// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;

/**
 * Keeps a mob assigned to its CreatureChat home area.
 */
public class GuardHomeGoal extends ReturnHomeGoal {
    public GuardHomeGoal(Mob entity, BlockPos home, double speed) {
        super(entity, home, speed, 64, true);
    }
}
