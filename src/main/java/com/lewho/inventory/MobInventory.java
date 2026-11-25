// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.inventory;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public final class MobInventory extends SimpleContainer {
    private ItemStack[] handMirrors;

    public MobInventory() {
        super(15);
    }

    void markHandMirror(int slot) {
        if (handMirrors == null) {
            handMirrors = new ItemStack[getContainerSize()];
        }
        handMirrors[slot] = getItem(slot);
    }

    boolean isHandMirror(int slot) {
        return handMirrors != null && getItem(slot) == handMirrors[slot];
    }

    public ItemStack getPersistentItem(int slot) {
        return isHandMirror(slot) ? ItemStack.EMPTY : getItem(slot);
    }
}
