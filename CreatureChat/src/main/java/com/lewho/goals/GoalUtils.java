// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.goals;

import com.lewho.mixin.MixinMobEntityAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

/**
 * The {@code GoalUtils} class uses reflection to extend the MobEntity class
 * and add a public GoalSelector method.
 */
public class GoalUtils {

    public static GoalSelector getGoalSelector(Mob mobEntity) {
        MixinMobEntityAccessor mixingEntity = (MixinMobEntityAccessor)mobEntity;
        return mixingEntity.getGoalSelector();
    }
}
