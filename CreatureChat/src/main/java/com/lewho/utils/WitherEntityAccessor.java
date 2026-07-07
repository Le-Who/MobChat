// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.utils;

import net.minecraft.world.damagesource.DamageSource;

/**
 * Accessor interface for WitherEntity to allow calling dropEquipment externally.
 */
public interface WitherEntityAccessor {
    void callDropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops);
}
