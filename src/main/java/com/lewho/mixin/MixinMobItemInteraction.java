// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.mixin;

import com.lewho.chat.MobInteractionHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Mob.class, priority = 900)
public class MixinMobItemInteraction {
    @Unique
    private ItemStack creaturechat$heldBeforeInteraction;

    @Inject(method = "interact", at = @At("HEAD"))
    private void creaturechat$captureHeldItem(Player player, InteractionHand hand,
                                              CallbackInfoReturnable<InteractionResult> cir) {
        Mob mob = (Mob) (Object) this;
        creaturechat$heldBeforeInteraction = MobInteractionHooks.handlesInteraction(mob, player, hand)
                ? player.getItemInHand(hand).copy() : null;
    }

    @Inject(method = "interact", at = @At("RETURN"))
    private void creaturechat$recordItemInteraction(Player player, InteractionHand hand,
                                                  CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack before = creaturechat$heldBeforeInteraction;
        creaturechat$heldBeforeInteraction = null;
        if (before != null && player instanceof ServerPlayer serverPlayer) {
            MobInteractionHooks.onItemGiven((Mob) (Object) this, serverPlayer, before,
                    player.getItemInHand(hand), cir.getReturnValue());
        }
    }
}
