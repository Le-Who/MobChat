// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;

public class LeadParticleFactory implements ParticleProvider<LeadParticleEffect> {
    private final SpriteSet spriteProvider;

    public LeadParticleFactory(SpriteSet spriteProvider) {
        this.spriteProvider = spriteProvider;
    }

    @Override
    public LeadParticle createParticle(LeadParticleEffect effect, ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        double angle = effect.angle();;
        return new LeadParticle(world, x, y, z, 0, 0, 0, this.spriteProvider, angle);
    }
}
