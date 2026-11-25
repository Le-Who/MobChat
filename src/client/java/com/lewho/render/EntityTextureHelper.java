// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.render;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public final class EntityTextureHelper {
    private EntityTextureHelper() {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static ResourceLocation getTexture(EntityRenderer renderer, Entity entity) {
        return renderer.getTextureLocation(entity);
    }
}
