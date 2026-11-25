// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.ui;

import com.lewho.render.BlendHelper;
import com.lewho.utils.TextureLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public abstract class ScreenHelper extends VersionedScreen {
    protected int BG_WIDTH, BG_HEIGHT, bgX, bgY, TITLE_OFFSET;
    private boolean skipNextBackground = false;
    protected static final TextureLoader textures = new TextureLoader();

    protected ScreenHelper(Component title) {
        super(title);
    }

    protected abstract EditBox getTextField();

    protected abstract Component getLabelText();

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);

        ResourceLocation bgTex = textures.GetUI("chat-background");
        if (bgTex != null) {
            BlendHelper.enableBlend();
            BlendHelper.defaultBlendFunc();
            context.blit(
                    RenderType::guiTextured,
                    bgTex,
                    bgX, bgY,
                    0f, 0f,
                    BG_WIDTH, BG_HEIGHT,
                    BG_WIDTH, BG_HEIGHT
            );
            BlendHelper.disableBlend();
        }

        skipNextBackground = true;
        super.render(context, mouseX, mouseY, delta);
        skipNextBackground = false;

        EditBox tf = getTextField();
        Component           lbl = getLabelText();
        int lw = font.width(lbl);
        int lx = (this.width - lw) / 2;
        int ly = tf.getY() - TITLE_OFFSET;
        context.drawString(font, lbl, lx, ly, 0xFFFFFF);
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (!skipNextBackground) {
            super.renderBackground(context, mouseX, mouseY, delta);
        }
    }
}
