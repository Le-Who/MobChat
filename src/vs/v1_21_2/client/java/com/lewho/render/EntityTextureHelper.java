// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.render;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class EntityTextureHelper {
    private EntityTextureHelper() {}

    public static ResourceLocation getTexture(EntityRenderer<?, ?> renderer, Entity entity) {
        if (renderer instanceof LivingEntityRenderer livingRenderer
                && entity   instanceof LivingEntity       living) {

            EntityRenderState rawState = livingRenderer.createRenderState(living, 0.0f);
            LivingEntityRenderState state = (LivingEntityRenderState) rawState;

            @SuppressWarnings("unchecked")
            ResourceLocation tex = ((LivingEntityRenderer) livingRenderer).getTextureLocation(state);

            return tex;
        }

        return null;
    }
}
