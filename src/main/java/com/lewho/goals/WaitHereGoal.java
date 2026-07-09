// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.goals;

import java.util.EnumSet;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps a mob near the position where the wait command was issued.
 */
public class WaitHereGoal extends net.minecraft.world.entity.ai.goal.Goal {
    private final Mob entity;
    private final Vec3 anchor;
    private final double speed;

    public WaitHereGoal(Mob entity, double speed) {
        this.entity = entity;
        this.anchor = entity.position();
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return true;
    }

    @Override
    public void tick() {
        if (this.entity.distanceToSqr(this.anchor) > 9) {
            moveToAnchor();
        } else {
            this.entity.getNavigation().stop();
            this.entity.setDeltaMovement(this.entity.getDeltaMovement().multiply(0.2, 1.0, 0.2));
        }
    }

    private void moveToAnchor() {
        if (this.entity instanceof PathfinderMob) {
            if (!this.entity.getNavigation().isInProgress()) {
                Path path = this.entity.getNavigation().createPath(this.anchor.x, this.anchor.y, this.anchor.z, 1);
                if (path != null) {
                    this.entity.getNavigation().moveTo(path, this.speed);
                }
            }
        } else {
            Vec3 moveDirection = this.anchor.subtract(this.entity.position()).normalize();
            this.entity.setDeltaMovement(moveDirection.x * this.speed, moveDirection.y * this.speed, moveDirection.z * this.speed);
            this.entity.hurtMarked = true;
        }
    }
}
