// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.particle;

import static com.lewho.network.ServerPackets.*;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class ParticleEmitter {

    public static void emitCreatureParticle(
            ServerLevel world,
            Entity entity,
            ParticleOptions effect,
            double spawnSize,
            int count
    ) {
        float yawDeg = entity.getYHeadRot();
        float rad     = yawDeg * ((float)Math.PI / 180.0F);

        double offsetX = -Mth.sin(rad) * 0.9F;
        double offsetY = entity.getBbHeight() + 0.5;
        double offsetZ =  Mth.cos(rad) * 0.9F;

        double x = entity.getX() + offsetX;
        double y = entity.getY() + offsetY;
        double z = entity.getZ() + offsetZ;

        world.sendParticles(
                effect,
                x, y, z,
                count,
                spawnSize, spawnSize, spawnSize,
                0.1
        );

        if (effect.equals(HEART_BIG_PARTICLE) && count > 1) {
            world.playSound(entity, entity.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.4F, 1.0F);
        } else if (effect.equals(FIRE_BIG_PARTICLE) && count > 1) {
            world.playSound(entity, entity.blockPosition(),
                    SoundEvents.AXE_STRIP,
                    SoundSource.PLAYERS, 0.8F, 1.0F);
        } else if (effect.equals(FOLLOW_FRIEND_PARTICLE)
                || effect.equals(FOLLOW_ENEMY_PARTICLE)
                || effect.equals(LEAD_FRIEND_PARTICLE)
                || effect.equals(LEAD_ENEMY_PARTICLE)) {
            world.playSound(entity, entity.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_PLACE,
                    SoundSource.PLAYERS, 0.8F, 1.0F);
        } else if (effect.equals(PROTECT_PARTICLE)) {
            world.playSound(entity, entity.blockPosition(),
                    SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.PLAYERS, 0.8F, 1.0F);
        }
    }
}
