// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.inventory;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MobInventoryStackTests {
    private static final int MAIN_HAND = 2;
    private static final int OFF_HAND = 3;

    @BeforeAll
    public static void bootstrapItems() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void reopeningRefreshesAMarkedMirrorAfterEquipmentDamageWithoutDuplicatingIt() {
        MobInventory inventory = new MobInventory();
        List<ItemStack> dropped = new ArrayList<>();
        ItemStack equipped = sword(0);
        MobInventoryMenu.syncHandSlots(inventory, MAIN_HAND, OFF_HAND, equipped, ItemStack.EMPTY, dropped::add);

        equipped.setDamageValue(9);
        MobInventoryMenu.syncHandSlots(inventory, MAIN_HAND, OFF_HAND, equipped, ItemStack.EMPTY, dropped::add);

        assertEquals(9, inventory.getItem(MAIN_HAND).getDamageValue());
        assertTrue(inventory.getPersistentItem(MAIN_HAND).isEmpty());
        assertTrue(inventory.getItem(0).isEmpty());
        assertTrue(dropped.isEmpty());
    }

    @Test
    public void independentlyPlacedItemInAHandSlotRemainsPersistentUntilRelocated() {
        MobInventory inventory = new MobInventory();
        MobInventoryMenu.syncHandSlots(inventory, MAIN_HAND, OFF_HAND, sword(0), ItemStack.EMPTY, ignored -> {});
        inventory.setItem(MAIN_HAND, sword(17));

        assertEquals(17, inventory.getPersistentItem(MAIN_HAND).getDamageValue());
        MobInventoryMenu.syncHandSlots(inventory, MAIN_HAND, OFF_HAND, sword(0), ItemStack.EMPTY, ignored -> {});

        assertEquals(17, inventory.getPersistentItem(0).getDamageValue());
        assertTrue(inventory.getPersistentItem(MAIN_HAND).isEmpty());
    }

    @Test
    public void openingMovesDifferentVariantOfEquippedItemIntoStash() {
        SimpleContainer inventory = new SimpleContainer(4);
        ItemStack equipped = sword(0);
        inventory.setItem(MAIN_HAND, sword(17));
        List<ItemStack> dropped = new ArrayList<>();

        MobInventoryMenu.syncHandSlots(inventory, MAIN_HAND, OFF_HAND, equipped, ItemStack.EMPTY, dropped::add);

        assertEquals(17, inventory.getItem(0).getDamageValue());
        assertEquals(Items.IRON_SWORD, inventory.getItem(MAIN_HAND).getItem());
        assertEquals(0, inventory.getItem(MAIN_HAND).getDamageValue());
        assertNotSame(equipped, inventory.getItem(MAIN_HAND));
        assertTrue(inventory.getItem(OFF_HAND).isEmpty());
        assertTrue(dropped.isEmpty());
    }

    @Test
    public void openingDropsDisplacedVariantWhenStashIsFull() {
        SimpleContainer inventory = new SimpleContainer(4);
        inventory.setItem(0, new ItemStack(Items.POTATO));
        inventory.setItem(1, new ItemStack(Items.POTATO));
        inventory.setItem(MAIN_HAND, sword(17));
        List<ItemStack> dropped = new ArrayList<>();

        MobInventoryMenu.syncHandSlots(inventory, MAIN_HAND, OFF_HAND, sword(0), ItemStack.EMPTY, dropped::add);

        assertEquals(1, dropped.size());
        assertEquals(Items.IRON_SWORD, dropped.get(0).getItem());
        assertEquals(17, dropped.get(0).getDamageValue());
        assertEquals(0, inventory.getItem(MAIN_HAND).getDamageValue());
        assertTrue(inventory.getItem(OFF_HAND).isEmpty());
    }

    @Test
    public void openingRefreshesCountOfSameVariantMirror() {
        SimpleContainer inventory = new SimpleContainer(4);
        inventory.setItem(MAIN_HAND, new ItemStack(Items.DIAMOND, 5));
        List<ItemStack> dropped = new ArrayList<>();

        MobInventoryMenu.syncHandSlots(inventory, MAIN_HAND, OFF_HAND, new ItemStack(Items.DIAMOND, 3),
                ItemStack.EMPTY, dropped::add);

        assertEquals(3, inventory.getItem(MAIN_HAND).getCount());
        assertTrue(inventory.getItem(0).isEmpty());
        assertTrue(dropped.isEmpty());
    }

    @Test
    public void replacingVariantInPlaceRecordsGiftAndRemovalAtEqualBaseItemCount() {
        SimpleContainer inventory = new SimpleContainer(4);
        ItemStack stack = sword(17);
        inventory.setItem(0, stack);
        MobInventorySnapshot initial = snapshot(inventory, ItemStack.EMPTY, ItemStack.EMPTY);

        stack.setDamageValue(0);
        MobInventorySnapshot.Changes changes = initial.changesTo(snapshot(inventory, ItemStack.EMPTY, ItemStack.EMPTY));

        assertEquals(1, changes.added().size());
        var added = changes.added().entrySet().iterator().next();
        assertEquals(1, added.getKey().countIn(sword(0)));
        assertEquals(1, added.getValue());
        assertEquals(1, changes.removed().size());
        var removed = changes.removed().entrySet().iterator().next();
        assertEquals(1, removed.getKey().countIn(sword(17)));
        assertEquals(1, removed.getValue());
    }

    @Test
    public void swappingDifferentVariantsOfSameItemBetweenHandsIsRecognized() {
        SimpleContainer inventory = new SimpleContainer(4);
        ItemStack first = sword(17);
        ItemStack second = sword(6);
        MobInventorySnapshot initial = snapshot(inventory, first, second);
        MobInventorySnapshot.Changes changes = initial.changesTo(snapshot(inventory, second, first));

        assertTrue(changes.handsSwapped());
        assertTrue(changes.added().isEmpty());
        assertTrue(changes.removed().isEmpty());
        assertTrue(changes.disarmedToInventory().isEmpty());
        assertTrue(changes.disarmedTaken().isEmpty());
    }

    @Test
    public void unchangedStashCannotMaskTakingMatchingHandStack() {
        SimpleContainer inventory = new SimpleContainer(4);
        ItemStack equipped = new ItemStack(Items.DIAMOND, 3);
        inventory.setItem(0, new ItemStack(Items.DIAMOND));
        inventory.setItem(MAIN_HAND, equipped.copy());
        MobInventorySnapshot initial = snapshot(inventory, equipped, ItemStack.EMPTY);
        MobInventorySnapshot.Changes changes = initial.changesTo(snapshot(inventory, ItemStack.EMPTY, ItemStack.EMPTY));

        assertEquals(List.of("3 " + equipped.getHoverName().getString()), changes.disarmedTaken());
        assertTrue(changes.diamondsTaken());
        assertTrue(changes.disarmedToInventory().isEmpty());
        assertTrue(changes.removed().isEmpty());
    }

    @Test
    public void disarmedStackCanBePartlyStashedAndPartlyTaken() {
        SimpleContainer inventory = new SimpleContainer(4);
        ItemStack equipped = new ItemStack(Items.DIAMOND, 4);
        inventory.setItem(0, new ItemStack(Items.DIAMOND));
        MobInventorySnapshot initial = snapshot(inventory, equipped, ItemStack.EMPTY);

        inventory.getItem(0).grow(2);
        MobInventorySnapshot.Changes changes = initial.changesTo(snapshot(inventory, ItemStack.EMPTY, ItemStack.EMPTY));

        List<String> twoDiamonds = List.of("2 " + equipped.getHoverName().getString());
        assertEquals(twoDiamonds, changes.disarmedToInventory());
        assertEquals(twoDiamonds, changes.disarmedTaken());
        assertTrue(changes.removed().isEmpty());
    }

    @Test
    public void movingOneOfTwoMatchingHandStacksToStashIsNotTheft() {
        SimpleContainer inventory = new SimpleContainer(4);
        ItemStack equipped = new ItemStack(Items.DIAMOND);
        MobInventorySnapshot initial = snapshot(inventory, equipped, equipped);

        inventory.setItem(0, equipped.copy());
        MobInventorySnapshot.Changes changes = initial.changesTo(snapshot(inventory, equipped, ItemStack.EMPTY));

        assertEquals(List.of(equipped.getHoverName().getString()), changes.disarmedToInventory());
        assertTrue(changes.disarmedTaken().isEmpty());
        assertTrue(changes.removed().isEmpty());
        assertFalse(changes.diamondsTaken());
    }

    private static MobInventorySnapshot snapshot(SimpleContainer inventory, ItemStack mainHand, ItemStack offHand) {
        return new MobInventorySnapshot(inventory, MAIN_HAND, OFF_HAND, mainHand, offHand);
    }

    private static ItemStack sword(int damage) {
        ItemStack stack = new ItemStack(Items.IRON_SWORD);
        stack.setDamageValue(damage);
        return stack;
    }
}
