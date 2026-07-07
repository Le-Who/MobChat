// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.goals;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Moves a mob toward a stored CreatureChat home position.
 */
public class ReturnHomeGoal extends Goal {
    protected final Mob entity;
    protected final BlockPos home;
    protected final double speed;
    protected final double activeDistanceSqr;
    protected final boolean persistent;

    public ReturnHomeGoal(Mob entity, BlockPos home, double speed) {
        this(entity, home, speed, 16, false);
    }

    protected ReturnHomeGoal(Mob entity, BlockPos home, double speed, double activeDistanceSqr, boolean persistent) {
        this.entity = entity;
        this.home = home;
        this.speed = speed;
        this.activeDistanceSqr = activeDistanceSqr;
        this.persistent = persistent;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return persistent || distanceToHomeSqr() > activeDistanceSqr;
    }

    @Override
    public boolean canContinueToUse() {
        return persistent || distanceToHomeSqr() > activeDistanceSqr;
    }

    @Override
    public void tick() {
        if (distanceToHomeSqr() > activeDistanceSqr) {
            moveToHome();
        } else {
            this.entity.getNavigation().stop();
        }
    }

    protected double distanceToHomeSqr() {
        return this.entity.distanceToSqr(Vec3.atCenterOf(this.home));
    }

    protected void moveToHome() {
        if (this.entity instanceof PathfinderMob) {
            if (!this.entity.getNavigation().isInProgress()) {
                Path path = this.entity.getNavigation().createPath(this.home, 1);
                if (path != null) {
                    this.entity.getNavigation().moveTo(path, this.speed);
                }
            }
        } else {
            Vec3 moveDirection = Vec3.atCenterOf(this.home).subtract(this.entity.position()).normalize();
            this.entity.setDeltaMovement(moveDirection.x * this.speed, moveDirection.y * this.speed, moveDirection.z * this.speed);
            this.entity.hurtMarked = true;
        }
    }
}
