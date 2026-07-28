// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.mixin;

import com.lewho.chat.ChatDataManager;
import com.lewho.chat.EntityChatData;
import net.minecraft.world.entity.npc.WanderingTrader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents WanderingTraderEntity from despawning if it has chat data or a character sheet.
 */
@Mixin(WanderingTrader.class)
public abstract class MixinWanderingTrader {

    @Inject(method = "maybeDespawn", at = @At("HEAD"), cancellable = true)
    private void preventTraderDespawn(CallbackInfo ci) {
        WanderingTrader trader = (WanderingTrader) (Object) this;

        // Get chat data for this trader
        EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(trader.getStringUUID());

        // If the character sheet is not empty, cancel the function to prevent despawning
        if (!chatData.characterSheet.isEmpty()) {
            ci.cancel();
        }
    }
}
