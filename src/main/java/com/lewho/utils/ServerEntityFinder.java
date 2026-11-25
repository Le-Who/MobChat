// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.utils;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class ServerEntityFinder {
    public static LivingEntity getEntityByUUID(ServerLevel world, UUID uuid) {
        for (Entity entity : world.getAllEntities()) {
            if (entity.getUUID().equals(uuid) && entity instanceof LivingEntity) {
                return (LivingEntity) entity;
            }
        }
        return null;
    }
}