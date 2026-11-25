// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.goals;

import com.lewho.controls.LookControls;
import java.util.EnumSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

public class TalkPlayerGoal extends Goal {
    private final Mob entity;
    private ServerPlayer targetPlayer;
    private final PathNavigation navigation;
    private final double seconds;
    private long startTime;

    public TalkPlayerGoal(ServerPlayer player, Mob entity, double seconds) {
        this.targetPlayer = player;
        this.entity = entity;
        this.seconds = seconds;
        this.navigation = entity.getNavigation();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!GoalTarget.isCurrent(this.entity, this.targetPlayer)) {
            this.targetPlayer = null;
            return false;
        }
        this.startTime = System.currentTimeMillis();
        this.entity.getNavigation().stop();
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return GoalTarget.isCurrent(this.entity, this.targetPlayer) &&
                (System.currentTimeMillis() - this.startTime) < (this.seconds * 1000);
    }

    @Override
    public void stop() {
        this.targetPlayer = null;
    }

    @Override
    public void tick() {
        if (!canContinueToUse()) {
            return;
        }
        LookControls.lookAtPlayer(this.targetPlayer, this.entity);
        this.navigation.stop();
    }
}