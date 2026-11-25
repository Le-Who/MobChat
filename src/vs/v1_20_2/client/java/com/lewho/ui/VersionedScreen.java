// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class VersionedScreen extends Screen {
    protected VersionedScreen(Component title) {
        super(title);
    }

    protected final void renderModalBackground(GuiGraphics graphics) {
        // Screen.renderWithTooltip renders the background before the screen content.
    }

    protected boolean scrollVertically(double mouseX, double mouseY, double amount) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return scrollVertically(mouseX, mouseY, vertical) || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }
}
