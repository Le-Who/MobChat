/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1646
 *  net.minecraft.class_5132$class_5133
 *  net.minecraft.class_5134
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.morwapi.aivillager.mixin;

import net.minecraft.class_1646;
import net.minecraft.class_5132;
import net.minecraft.class_5134;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_1646.class})
public class VillagerEntityAttributeMixin {
    @Inject(method={"createVillagerAttributes"}, at={@At(value="RETURN")}, cancellable=true)
    private static void addAttackAttribute(CallbackInfoReturnable<class_5132.class_5133> cir) {
        cir.setReturnValue((Object)((class_5132.class_5133)cir.getReturnValue()).method_26868(class_5134.field_23721, 1.0));
    }
}

