// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.goals;

import com.lewho.chat.ChatDataManager;
import com.lewho.chat.EntityChatData;
import com.lewho.chat.AdvancementHelper;
import com.lewho.controls.LookControls;
import com.lewho.network.ServerPackets;
import com.lewho.particle.LeadParticleEffect;
import com.lewho.utils.RandomTargetFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Random;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class LeadPlayerGoal extends PlayerBaseGoal {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private final Mob entity;
    private final double speed;
    private final Random random = new Random();
    private int currentWaypoint = 0;
    private int totalWaypoints;
    private Vec3 currentTarget = null;
    private boolean foundWaypoint = false;
    private int ticksSinceLastWaypoint = 0;
    private final Vec3 startPos;

    public LeadPlayerGoal(ServerPlayer player, Mob entity, double speed) {
        super(player, entity);
        this.entity = entity;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.totalWaypoints = random.nextInt(14) + 6;
        this.startPos = player.position();
    }

    @Override
    public boolean canUse() {
        return !foundWaypoint && super.canUse() && this.entity.distanceToSqr(this.targetEntity) <= 16 * 16;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        this.entity.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!canContinueToUse()) {
            stop();
            return;
        }
        ticksSinceLastWaypoint++;

        if (currentWaypoint >= totalWaypoints) {
            foundWaypoint = true;
            ServerPlayer player = (ServerPlayer) this.targetEntity;
            this.targetEntity = null;
            double distance = this.startPos.distanceTo(player.position());
            if (distance >= 64) {
                AdvancementHelper.guidedTour(player);
            }
            LOGGER.info("Tick: You have ARRIVED at your destination");

            Level arrivalLevel = this.entity.level();
            ServerPackets.scheduler.scheduleTask(() -> {
                if (!this.entity.isAlive() || this.entity.level() != arrivalLevel
                        || !GoalTarget.isCurrent(this.entity, player)) {
                    return;
                }
                String arrivedMessage = "<You have arrived at your destination>";

                ChatDataManager chatDataManager = ChatDataManager.getServerInstance();
                EntityChatData chatData = chatDataManager.getOrCreateChatData(this.entity.getStringUUID());
                if (!chatData.characterSheet.isEmpty()) {
                    ServerPackets.generate_chat("N/A", chatData, player, this.entity, arrivedMessage, true);
                }
            });

            this.entity.getNavigation().stop();

        } else if (this.currentTarget == null || this.entity.distanceToSqr(this.currentTarget) < 2 * 2 || ticksSinceLastWaypoint >= 20 * 10) {
            setNewTarget();
            moveToTarget();
            ticksSinceLastWaypoint = 0;

        } else {
            moveToTarget();
        }
    }

    private void moveToTarget() {
        if (this.currentTarget != null) {
            if (this.entity instanceof PathfinderMob) {
                 if (!this.entity.getNavigation().isInProgress()) {
                     Path path = this.entity.getNavigation().createPath(this.currentTarget.x, this.currentTarget.y, this.currentTarget.z, 1);
                     if (path != null) {
                         LOGGER.debug("Start moving along path");
                         this.entity.getNavigation().moveTo(path, this.speed);
                     }
                 }
            } else {
                LookControls.lookAtPosition(this.currentTarget, this.entity);

                Vec3 entityPos = this.entity.position();
                Vec3 moveDirection = this.currentTarget.subtract(entityPos).normalize();

                double currentSpeed = this.entity.getDeltaMovement().horizontalDistance();

                currentSpeed = Mth.approach((float) currentSpeed, (float) this.speed, (float) (0.005 * (this.speed / Math.max(currentSpeed, 0.1))));

                Vec3 newVelocity = new Vec3(moveDirection.x * currentSpeed, moveDirection.y * currentSpeed, moveDirection.z * currentSpeed);

                this.entity.setDeltaMovement(newVelocity);
                this.entity.hurtMarked = true;
            }
        }
    }

    private void setNewTarget() {
        currentWaypoint++;
        LOGGER.info("Waypoint " + currentWaypoint + " / " + this.totalWaypoints);
        this.currentTarget = RandomTargetFinder.findRandomTarget(this.entity, 30, 24, 36);
        if (this.currentTarget != null) {
            emitParticlesAlongRaycast(this.entity.position(), this.currentTarget);
        }

        this.entity.getNavigation().stop();
    }

    private void emitParticleAt(Vec3 position, double angle) {
        if (this.entity.level() instanceof ServerLevel) {
            ServerLevel serverWorld = (ServerLevel) this.entity.level();

            LeadParticleEffect effect = new LeadParticleEffect((float)angle);
            serverWorld.sendParticles(effect, position.x, position.y + 0.05, position.z, 1, 0, 0, 0, 0);
        }
    }

    private void emitParticlesAlongRaycast(Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start);

        double angleRadians = Math.atan2(direction.z, direction.x);

        double angleDegrees = Math.toDegrees(angleRadians);

        double minecraftYaw = (360 - (angleDegrees + 90)) % 360;

        minecraftYaw = (minecraftYaw + 180) % 360;
        if (minecraftYaw < 0) {
            minecraftYaw += 360;
        }

        double distance = start.distanceTo(end);
        double startRange = Math.min(5, distance);;
        double endRange = Math.min(startRange + 10, distance);
        for (double d = startRange; d <= endRange; d += 5) {
            Vec3 pos = start.add(direction.normalize().scale(d));
            emitParticleAt(pos, Math.toRadians(minecraftYaw));
        }
    }
}