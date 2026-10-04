// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.inventory.MobHandSlot;
import com.lewho.inventory.MobInventoryTransferMenu;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MobHandSlotTests {
    @BeforeAll
    public static void bootstrapItems() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void partialHandExtractionKeepsEquipmentCountAndReopenConsistent() {
        SimpleContainer hand = new SimpleContainer(1);
        SimpleContainer player = new SimpleContainer(1);
        hand.setItem(0, new ItemStack(Items.DIAMOND, 64));
        player.setItem(0, new ItemStack(Items.DIAMOND, 63));
        AtomicReference<ItemStack> equipment = new AtomicReference<>(hand.getItem(0).copy());
        MobHandSlot handSlot = new MobHandSlot(hand, 0, 0, 0, stack -> true, equipment::set);
        TransferMenu menu = new TransferMenu(handSlot, new Slot(player, 0, 0, 0));

        assertTrue(menu.transfer(handSlot.getItem(), 1, 2));
        handSlot.setChanged();

        assertEquals(63, hand.getItem(0).getCount());
        assertEquals(64, player.getItem(0).getCount());
        assertEquals(63, equipment.get().getCount());
        assertNotSame(hand.getItem(0), equipment.get());
        hand.setItem(0, equipment.get().copy());
        assertEquals(63, hand.getItem(0).getCount());
        assertEquals(127, hand.getItem(0).getCount() + player.getItem(0).getCount());
    }

    @Test
    public void destinationHandMergeKeepsEquipmentCountAndReopenConsistent() {
        SimpleContainer hand = new SimpleContainer(1);
        SimpleContainer player = new SimpleContainer(1);
        hand.setItem(0, new ItemStack(Items.DIAMOND, 63));
        player.setItem(0, new ItemStack(Items.DIAMOND, 2));
        AtomicReference<ItemStack> equipment = new AtomicReference<>(hand.getItem(0).copy());
        MobHandSlot handSlot = new MobHandSlot(hand, 0, 0, 0, stack -> true, equipment::set);
        TransferMenu menu = new TransferMenu(handSlot, new Slot(player, 0, 0, 0));

        assertTrue(menu.transfer(player.getItem(0), 0, 1));

        assertEquals(64, hand.getItem(0).getCount());
        assertEquals(1, player.getItem(0).getCount());
        assertEquals(64, equipment.get().getCount());
        hand.setItem(0, equipment.get().copy());
        assertEquals(65, hand.getItem(0).getCount() + player.getItem(0).getCount());
    }

    @Test
    public void takingWholeHandStackClearsEquipment() {
        SimpleContainer hand = new SimpleContainer(1);
        AtomicReference<ItemStack> equipment = new AtomicReference<>(ItemStack.EMPTY);
        MobHandSlot handSlot = new MobHandSlot(hand, 0, 0, 0, stack -> true, equipment::set);
        handSlot.set(new ItemStack(Items.DIAMOND, 4));

        ItemStack taken = handSlot.remove(4);
        handSlot.onTake(null, taken);

        assertEquals(4, taken.getCount());
        assertTrue(hand.getItem(0).isEmpty());
        assertTrue(equipment.get().isEmpty());
    }

    private static final class TransferMenu extends MobInventoryTransferMenu {
        private TransferMenu(Slot hand, Slot player) {
            super(null, 0);
            addSlot(hand);
            addSlot(player);
        }

        private boolean transfer(ItemStack stack, int start, int end) {
            return moveItemStackTo(stack, start, end, false);
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
