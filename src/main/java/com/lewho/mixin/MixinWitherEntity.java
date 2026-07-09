// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.mixin;

import com.lewho.utils.WitherEntityAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Mixin to expose the protected dropEquipment method from WitherEntity.
 */
@Mixin(WitherBoss.class)
public abstract class MixinWitherEntity implements WitherEntityAccessor {

    @Shadow
    protected abstract void dropCustomDeathLoot(DamageSource source, int lootingMultiplier, boolean allowDrops);

    @Override
    public void callDropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        dropCustomDeathLoot(source, lootingMultiplier, allowDrops);
    }
}
