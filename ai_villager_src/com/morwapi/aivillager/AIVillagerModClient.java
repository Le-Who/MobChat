/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ClientModInitializer
 *  net.minecraft.class_3929
 */
package com.morwapi.aivillager;

import com.morwapi.aivillager.AIVillagerMod;
import com.morwapi.aivillager.client.KeyInputHandler;
import com.morwapi.aivillager.screen.VillagerControlScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.class_3929;

public class AIVillagerModClient
implements ClientModInitializer {
    public void onInitializeClient() {
        KeyInputHandler.register();
        class_3929.method_17542(AIVillagerMod.VILLAGER_CONTROL_SCREEN_HANDLER, VillagerControlScreen::new);
    }
}

