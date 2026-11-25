// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.ui;

import com.lewho.render.BlendHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ButtonHelper {

    public static Button createImageButton(
            int x, int y,
            int width, int height,
            ResourceLocation normalTex,
            ResourceLocation hoverTex,
            Button.OnPress onPress,
            Button.CreateNarration narrate
    ) {
        return new Button(x, y, width, height, Component.empty(), onPress, narrate) {
            @Override
            protected void renderWidget(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
                BlendHelper.enableBlend();
                BlendHelper.defaultBlendFunc();

                ResourceLocation tex = isHovered() ? hoverTex : normalTex;

                ctx.blit(
                        RenderPipelines.GUI_TEXTURED,
                        tex,
                        getX(), getY(),
                        0f, 0f,
                        width, height,
                        width, height
                );

                BlendHelper.disableBlend();
            }
        };
    }
}
