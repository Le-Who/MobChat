// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.utils;

import com.lewho.ui.ClickHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class UseItemCallbackHelper {
    private UseItemCallbackHelper() {}

    public static InteractionResultHolder<ItemStack> handleUseItemAction(
            Player player,
            Level world,
            InteractionHand hand
    ) {
        if (shouldCancelAction(world)) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    private static boolean shouldCancelAction(Level world) {
        return ClickHandler.shouldCancelAction(world);
    }
}
