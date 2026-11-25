// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.mixin;

import com.lewho.chat.ChatDataManager;
import com.lewho.chat.EntityChatData;
import com.lewho.utils.NbtCompoundHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

@Mixin(Entity.class)
public abstract class MixinEntityChatData {
    @Shadow
    public abstract UUID getUUID();

    @Inject(method = "saveWithoutId(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;", at = @At("TAIL"))
    private void writeChatData(CompoundTag nbt, CallbackInfoReturnable<CompoundTag> cir) {
        UUID currentUUID = this.getUUID();

        EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(currentUUID.toString());
        if (!chatData.characterSheet.isEmpty()) {
            CompoundTag returned = cir.getReturnValue();
            NbtCompoundHelper.putUuid(returned, "CCUUID", currentUUID);
        }
    }

    @Inject(method = "load(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void readChatData(CompoundTag nbt, CallbackInfo ci) {
        UUID currentUUID = this.getUUID();
        if (nbt.contains("CCUUID")) {
            UUID originalUUID = NbtCompoundHelper.getUuid(nbt, "CCUUID");
            if (!originalUUID.equals(currentUUID)) {
                ChatDataManager.getServerInstance().updateUUID(originalUUID.toString(), currentUUID.toString());
            }
        }
    }
}
