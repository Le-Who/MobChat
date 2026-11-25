// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.inventory;

import net.minecraft.world.item.ItemStack;

final class ItemStackVariant {
    private ItemStackVariant() {
    }

    static boolean matches(ItemStack first, ItemStack second) {
        return ItemStack.isSameItemSameTags(first, second);
    }
}
