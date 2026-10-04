// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** A container slot that mirrors a mob's equipped hand stack. */
public final class MobHandSlot extends Slot {
    private final Predicate<ItemStack> canHold;
    private final Consumer<ItemStack> equip;

    public MobHandSlot(Container container, int index, int x, int y,
                       Predicate<ItemStack> canHold, Consumer<ItemStack> equip) {
        super(container, index, x, y);
        this.canHold = canHold;
        this.equip = equip;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.isEmpty() || canHold.test(stack);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        // Shift-click merges mutate an existing stack and only notify this method.
        // Keep equipment as a separate copy, just as ordinary slot replacement does.
        equip.accept(getItem().copy());
    }
}
