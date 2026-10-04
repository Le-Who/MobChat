// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.inventory;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MobInventoryAccessTests {
    @BeforeAll
    public static void bootstrapItems() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void lockedOccupiedDestinationRejectsShiftClickMerge() {
        SimpleContainer inventory = new SimpleContainer(1);
        inventory.setItem(0, new ItemStack(Items.DIAMOND, 63));
        ItemStack source = new ItemStack(Items.DIAMOND, 2);
        TransferMenu menu = new TransferMenu(new MobInventoryMenu.LockedSlot(inventory, 0, 0, 0));

        assertFalse(menu.transfer(source, false));

        assertEquals(63, inventory.getItem(0).getCount());
        assertEquals(2, source.getCount());
    }

    @Test
    public void lockedOccupiedSlotIsSkippedBeforeAllowedDestination() {
        SimpleContainer inventory = new SimpleContainer(2);
        inventory.setItem(0, new ItemStack(Items.DIAMOND, 63));
        inventory.setItem(1, new ItemStack(Items.DIAMOND, 62));
        ItemStack source = new ItemStack(Items.DIAMOND, 2);
        TransferMenu menu = new TransferMenu(new MobInventoryMenu.LockedSlot(inventory, 0, 0, 0),
                new Slot(inventory, 1, 0, 0));

        assertTrue(menu.transfer(source, false));

        assertEquals(63, inventory.getItem(0).getCount());
        assertEquals(64, inventory.getItem(1).getCount());
        assertTrue(source.isEmpty());
    }

    @Test
    public void reverseTransferSkipsLockedDestination() {
        SimpleContainer inventory = new SimpleContainer(2);
        inventory.setItem(0, new ItemStack(Items.DIAMOND, 62));
        inventory.setItem(1, new ItemStack(Items.DIAMOND, 63));
        ItemStack source = new ItemStack(Items.DIAMOND, 2);
        TransferMenu menu = new TransferMenu(new Slot(inventory, 0, 0, 0),
                new MobInventoryMenu.LockedSlot(inventory, 1, 0, 0));

        assertTrue(menu.transfer(source, true));

        assertEquals(64, inventory.getItem(0).getCount());
        assertEquals(63, inventory.getItem(1).getCount());
        assertTrue(source.isEmpty());
    }

    @Test
    public void allowedMergeIsPreferredOverEarlierEmptySlot() {
        SimpleContainer inventory = new SimpleContainer(2);
        inventory.setItem(1, new ItemStack(Items.DIAMOND, 63));
        ItemStack source = new ItemStack(Items.DIAMOND, 1);
        TransferMenu menu = new TransferMenu(new Slot(inventory, 0, 0, 0), new Slot(inventory, 1, 0, 0));

        assertTrue(menu.transfer(source, false));

        assertTrue(inventory.getItem(0).isEmpty());
        assertEquals(64, inventory.getItem(1).getCount());
    }

    @Test
    public void lockedEmptySlotAlsoRejectsTransfer() {
        SimpleContainer inventory = new SimpleContainer(1);
        ItemStack source = new ItemStack(Items.DIAMOND, 2);
        TransferMenu menu = new TransferMenu(new MobInventoryMenu.LockedSlot(inventory, 0, 0, 0));

        assertFalse(menu.transfer(source, false));

        assertTrue(inventory.getItem(0).isEmpty());
        assertEquals(2, source.getCount());
    }

    private static final class TransferMenu extends MobInventoryTransferMenu {
        private TransferMenu(Slot... slots) {
            super(null, 0);
            for (Slot slot : slots) {
                addSlot(slot);
            }
        }

        private boolean transfer(ItemStack stack, boolean reverse) {
            return moveItemStackTo(stack, 0, slots.size(), reverse);
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }
    }
}
