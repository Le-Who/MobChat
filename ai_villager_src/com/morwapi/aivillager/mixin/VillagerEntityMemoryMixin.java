/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1646
 *  net.minecraft.class_2487
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.morwapi.aivillager.mixin;

import com.morwapi.aivillager.AIVillagerMod;
import com.morwapi.aivillager.access.VillagerMemoryAccessor;
import com.morwapi.aivillager.memory.VillagerMemory;
import net.minecraft.class_1646;
import net.minecraft.class_2487;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_1646.class})
public class VillagerEntityMemoryMixin
implements VillagerMemoryAccessor {
    @Unique
    private final VillagerMemory ai_memory = new VillagerMemory();

    @Override
    public VillagerMemory getAiMemory() {
        return this.ai_memory;
    }

    @Inject(method={"writeCustomDataToNbt"}, at={@At(value="TAIL")})
    public void writeCustomDataToNbt(class_2487 nbt, CallbackInfo ci) {
        AIVillagerMod.LOGGER.info("Saving Villager Memory to NBT");
        this.ai_memory.writeNbt(nbt);
    }

    @Inject(method={"readCustomDataFromNbt"}, at={@At(value="TAIL")})
    public void readCustomDataFromNbt(class_2487 nbt, CallbackInfo ci) {
        AIVillagerMod.LOGGER.info("Loading Villager Memory from NBT");
        this.ai_memory.readNbt(nbt);
    }
}

