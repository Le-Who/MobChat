// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.ui;

import com.lewho.utils.TextureLoader;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public abstract class ScreenHelper extends VersionedScreen {
    protected int BG_WIDTH, BG_HEIGHT, bgX, bgY, TITLE_OFFSET;
    protected static final TextureLoader textures = new TextureLoader();
    private boolean skipNextBackground = false;

    protected ScreenHelper(Component title) {
        super(title);
    }

    protected abstract EditBox getTextField();

    protected abstract Component getLabelText();

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        ResourceLocation bgTex = textures.GetUI("chat-background");
        if (bgTex != null) {
            context.blit(
                    bgTex,
                    bgX, bgY,
                    0,   0,
                    BG_WIDTH, BG_HEIGHT,
                    BG_WIDTH, BG_HEIGHT
            );
        }

        skipNextBackground = true;
        super.render(context, mouseX, mouseY, delta);
        skipNextBackground = false;

        EditBox tf = getTextField();
        Component label = getLabelText();
        Font renderer = this.font;
        int lw = renderer.width(label);
        int lx = (this.width - lw) / 2;
        int ly = tf.getY() - TITLE_OFFSET;
        context.drawString(renderer, label, lx, ly, 0xFFFFFF);
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (!skipNextBackground) {
            super.renderBackground(context, mouseX, mouseY, delta);
        }
    }
}
