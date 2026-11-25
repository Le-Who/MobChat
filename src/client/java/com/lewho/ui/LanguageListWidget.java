// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;

abstract class LanguageListWidget<E extends ObjectSelectionList.Entry<E>> extends ObjectSelectionList<E> {
    protected LanguageListWidget(Minecraft client, int width, int top, int bottom, int itemHeight) {
        super(client, width, bottom - top, top, bottom, itemHeight);
        setRenderBackground(false);
        setRenderTopAndBottom(false);
    }
}
