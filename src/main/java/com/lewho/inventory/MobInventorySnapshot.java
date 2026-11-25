// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class MobInventorySnapshot {
    private final Map<Variant, Integer> counts = new LinkedHashMap<>();
    private final ItemStack mainHand;
    private final ItemStack offHand;

    MobInventorySnapshot(Container inventory, int mainHandSlot, int offHandSlot, ItemStack mainHand, ItemStack offHand) {
        this.mainHand = mainHand.copy();
        this.offHand = offHand.copy();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (i != mainHandSlot && i != offHandSlot) {
                add(inventory.getItem(i));
            }
        }
        add(this.mainHand);
        add(this.offHand);
    }

    private void add(ItemStack stack) {
        if (!stack.isEmpty()) {
            counts.merge(new Variant(stack.copy()), stack.getCount(), Integer::sum);
        }
    }

    Changes changesTo(MobInventorySnapshot current) {
        Set<Variant> variants = new LinkedHashSet<>(counts.keySet());
        variants.addAll(current.counts.keySet());
        Map<Variant, Integer> added = new LinkedHashMap<>();
        Map<Variant, Integer> removed = new LinkedHashMap<>();
        List<String> disarmedToInventory = new ArrayList<>();
        List<String> disarmedTaken = new ArrayList<>();
        boolean diamondsTaken = false;
        for (Variant variant : variants) {
            int netAdded = current.counts.getOrDefault(variant, 0) - counts.getOrDefault(variant, 0);
            int taken = Math.max(0, -netAdded);
            int disarmed = Math.max(0, handCount(variant) - current.handCount(variant));
            int takenFromHands = Math.min(disarmed, taken);
            int movedToInventory = disarmed - takenFromHands;
            if (netAdded > 0) {
                added.put(variant, netAdded);
            }
            if (taken > takenFromHands) {
                removed.put(variant, taken - takenFromHands);
            }
            if (movedToInventory > 0) {
                disarmedToInventory.add(variant.format(movedToInventory, false));
            }
            if (takenFromHands > 0) {
                disarmedTaken.add(variant.format(takenFromHands, false));
            }
            diamondsTaken |= taken > 0 && variant.item() == Items.DIAMOND;
        }
        boolean mainChanged = !ItemStackVariant.matches(mainHand, current.mainHand);
        boolean offChanged = !ItemStackVariant.matches(offHand, current.offHand);
        boolean swapped = (mainChanged || offChanged)
                && ItemStackVariant.matches(mainHand, current.offHand)
                && ItemStackVariant.matches(offHand, current.mainHand);
        boolean handChanged = (mainChanged && !current.mainHand.isEmpty()) || (offChanged && !current.offHand.isEmpty());
        return new Changes(added, removed, disarmedToInventory, disarmedTaken, diamondsTaken, swapped, handChanged);
    }

    private int handCount(Variant variant) {
        return variant.countIn(mainHand) + variant.countIn(offHand);
    }

    record Changes(Map<Variant, Integer> added, Map<Variant, Integer> removed, List<String> disarmedToInventory,
                   List<String> disarmedTaken, boolean diamondsTaken, boolean handsSwapped, boolean handChanged) {
    }

    static final class Variant {
        private final ItemStack stack;

        private Variant(ItemStack stack) {
            this.stack = stack;
        }

        Item item() {
            return stack.getItem();
        }

        int countIn(ItemStack other) {
            return ItemStackVariant.matches(stack, other) ? other.getCount() : 0;
        }

        String format(int count, boolean showSingleCount) {
            String name = stack.copy().getHoverName().getString();
            return count == 1 && !showSingleCount ? name : count + " " + name;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Variant variant && ItemStackVariant.matches(stack, variant.stack);
        }

        @Override
        public int hashCode() {
            return item().hashCode();
        }
    }
}
