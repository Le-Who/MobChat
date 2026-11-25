// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;

public class BehaviorParticle extends TextureSheetParticle {
    protected BehaviorParticle(ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.scale(2f);
        this.setLifetime(35);

        this.yd = 0.1;
        this.xd *= 0.1;
        this.zd *= 0.1;
        this.hasPhysics = false;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public int getLightColor(float tint) {
        return 0xF000F0;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.yd > 0) {
            this.yd -= 0.002;
        }

        if (this.yd < 0) {
            this.yd = 0;
        }
    }
}
