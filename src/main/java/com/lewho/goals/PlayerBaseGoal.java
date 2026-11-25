// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

public abstract class PlayerBaseGoal extends Goal {
    protected LivingEntity targetEntity;
    private final GoalTarget<LivingEntity> target;

    protected PlayerBaseGoal(LivingEntity targetEntity, Mob owner) {
        this.target = GoalTarget.track(owner, targetEntity);
    }

    @Override
    public boolean canUse() {
        return refreshTarget();
    }

    protected final boolean refreshTarget() {
        LivingEntity previousTarget = targetEntity;
        targetEntity = resolveTarget();
        if (targetEntity != previousTarget) {
            onTargetChanged(previousTarget);
        }
        return targetEntity != null;
    }

    protected LivingEntity resolveTarget() {
        return target.resolve();
    }

    protected void onTargetChanged(LivingEntity previousTarget) {
    }
}
