// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.controls;

import net.minecraft.world.phys.Vec3;

public interface ISquidEntity {
    void forceSwimVector(Vec3 vec);
}