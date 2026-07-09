// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.utils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/**
 * The {@code EntityHeightMap} class returns an adjusted entity height (which fixes certain MobEntity's with
 * unusually tall heights)
 */
public class EntityHeights {
    public static float getAdjustedEntityHeight(Entity entity) {
        // Get entity height (adjust for specific classes)
        float entityHeight = entity.getBbHeight();
        if (entity instanceof EnderDragon) {
            entityHeight = 3F;
        }
        return entityHeight;
    }
}
