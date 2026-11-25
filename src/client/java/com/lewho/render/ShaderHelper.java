// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.GameRenderer;

public final class ShaderHelper {
    private ShaderHelper() {}

    public static void setTexturedShader() {
        RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
    }
}
