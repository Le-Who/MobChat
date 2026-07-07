// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.mixin;

import com.lewho.controls.ISquidEntity;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Exposes forceSwimVector(...) by writing the private swimVec field. */
@Mixin(Squid.class)
public abstract class MixinSquidEntity implements ISquidEntity {
    // shadow the private field
    @Shadow private Vec3 movementVector;

    @Override
    public void forceSwimVector(Vec3 vec) {
        this.movementVector = vec;
    }
}
