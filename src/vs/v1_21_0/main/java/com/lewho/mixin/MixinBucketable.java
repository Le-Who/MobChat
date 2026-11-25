// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.mixin;

import com.lewho.chat.ChatDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.lewho.utils.NbtCompoundHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

@Mixin(Bucketable.class)
public interface MixinBucketable {

    @Inject(
            method = "saveDefaultDataToBucketTag(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/item/ItemStack;)V",
            at     = @At("TAIL")
    )
    private static void captureCCUUID(Mob entity, ItemStack stack, CallbackInfo ci) {
        UUID oldId = entity.getUUID();

        CustomData.update(
                DataComponents.BUCKET_ENTITY_DATA,
                stack,
                tag -> tag.putUUID("CCUUID", oldId)
        );

        LoggerFactory.getLogger("creaturechat")
                .info("[Bucket-Capture] stored {}", oldId);
    }

    @Inject(
            method = "loadDefaultDataFromBucketTag(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/nbt/CompoundTag;)V",
            at     = @At("TAIL")
    )
    private static void restoreCCUUID(Mob entity, CompoundTag nbt, CallbackInfo ci) {
        if (!NbtCompoundHelper.containsUuid(nbt, "CCUUID")) return;

        UUID oldId = NbtCompoundHelper.getUuid(nbt, "CCUUID");
        UUID newId = entity.getUUID();

        ChatDataManager.getServerInstance()
                .updateUUID(oldId.toString(), newId.toString());

        LoggerFactory.getLogger("creaturechat")
                .info("[Bucket-Release] chat {} → {}", oldId, newId);
    }
}
