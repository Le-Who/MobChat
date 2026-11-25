// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.inventory;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

final class InventoryTooltip {
    private InventoryTooltip() {
    }

    static void render(GuiGraphics graphics, Font font, List<Component> lines, int mouseX, int mouseY) {
        graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }
}
