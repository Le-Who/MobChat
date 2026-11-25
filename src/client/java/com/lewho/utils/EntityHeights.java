// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.utils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

public class EntityHeights {
    public static float getAdjustedEntityHeight(Entity entity) {
        float entityHeight = entity.getBbHeight();
        if (entity instanceof EnderDragon) {
            entityHeight = 3F;
        }
        return entityHeight;
    }
}
