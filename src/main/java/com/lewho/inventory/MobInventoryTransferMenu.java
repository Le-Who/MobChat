// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.inventory;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Transfer policy shared by the mob inventory's slot operations. */
public abstract class MobInventoryTransferMenu extends AbstractContainerMenu {
    protected MobInventoryTransferMenu(MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    @Override
    protected boolean moveItemStackTo(ItemStack stack, int start, int end, boolean reverse) {
        boolean moved = false;
        int first = reverse ? end - 1 : start;
        int step = reverse ? -1 : 1;

        // Vanilla checks mayPlace for empty slots, but ignores it during stack merges.
        // Filter occupied destinations too, keeping its merge-before-insert ordering.
        if (stack.isStackable()) {
            for (int index = first; index >= start && index < end && !stack.isEmpty(); index += step) {
                Slot slot = slots.get(index);
                if (slot.hasItem() && slot.mayPlace(stack)) {
                    moved |= super.moveItemStackTo(stack, index, index + 1, false);
                }
            }
        }

        for (int index = first; index >= start && index < end && !stack.isEmpty(); index += step) {
            Slot slot = slots.get(index);
            if (!slot.hasItem() && slot.mayPlace(stack)
                    && super.moveItemStackTo(stack, index, index + 1, false)) {
                moved = true;
                break;
            }
        }
        return moved;
    }
}
