// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.controls;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.animal.HappyGhast;

/**
 * 1.21.7 adds {@link HappyGhast}; include it in ghast-like check.
 */
public class LookControlsHelper {
    public static boolean isGhast(Mob entity) {
        return entity instanceof Ghast || entity instanceof HappyGhast;
    }
}
