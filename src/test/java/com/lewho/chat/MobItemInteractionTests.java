// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.chat;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MobItemInteractionTests {
    @BeforeAll
    public static void bootstrapItems() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void acceptingTheLastItemInThePlayersHandCountsAsAGift() {
        assertEquals(1, MobInteractionHooks.giftAmount(new ItemStack(Items.WHEAT), ItemStack.EMPTY, true, false));
    }

    @Test
    public void giftAmountCountsConsumedItems() {
        ItemStack before = new ItemStack(Items.WHEAT, 8);
        ItemStack after = new ItemStack(Items.WHEAT, 7);

        assertEquals(1, MobInteractionHooks.giftAmount(before, after, true, false));
        assertEquals(0, MobInteractionHooks.giftAmount(before, before, false, false));
        assertEquals(0, MobInteractionHooks.giftAmount(before, before, true, false));
        assertEquals(1, MobInteractionHooks.giftAmount(before, before, true, true));
    }
}
