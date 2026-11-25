// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.utils;

import net.minecraft.world.entity.TamableAnimal;

public final class TameableHelper {
    private TameableHelper() {}

    public static void clearOwner(TamableAnimal entity) {
        entity.setTame(false, true);
        entity.setOwnerUUID(null);
    }
}
