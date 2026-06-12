/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1646
 *  net.minecraft.class_1799
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.morwapi.aivillager.mixin;

import net.minecraft.class_1646;
import net.minecraft.class_1799;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_1646.class})
public abstract class VillagerEntityInventoryMixin {
    @Inject(method={"canGather"}, at={@At(value="HEAD")}, cancellable=true)
    private void allowGatheringAnyItem(class_1799 stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue((Object)true);
    }
}

