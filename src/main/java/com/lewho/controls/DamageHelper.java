// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.controls;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class DamageHelper {
    private DamageHelper() {}

    public static boolean applyLeapDamage(LivingEntity attacker, LivingEntity target, float amount) {
        DamageSource src = attacker.damageSources().generic();
        return target.hurt(src, amount);
    }
}
