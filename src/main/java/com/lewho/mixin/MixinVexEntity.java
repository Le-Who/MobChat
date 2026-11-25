// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.mixin;

import com.lewho.chat.ChatDataManager;
import com.lewho.chat.EntityChatData;
import net.minecraft.world.entity.monster.Vex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Vex.class)
public abstract class MixinVexEntity {
    @Inject(method = "tick", at = @At("HEAD"))
    private void preventStarvationIfChatData(CallbackInfo ci) {
        Vex vex = (Vex) (Object) this;

        EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(vex.getStringUUID());
        if (!chatData.characterSheet.isEmpty()) {
            vex.setLimitedLife(20);
        }
    }
}
