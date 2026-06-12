/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
 *  net.fabricmc.fabric.api.networking.v1.PacketByteBufs
 *  net.minecraft.class_1297
 *  net.minecraft.class_1646
 *  net.minecraft.class_238
 *  net.minecraft.class_239$class_240
 *  net.minecraft.class_243
 *  net.minecraft.class_2540
 *  net.minecraft.class_2960
 *  net.minecraft.class_304
 *  net.minecraft.class_310
 *  net.minecraft.class_3675$class_307
 *  net.minecraft.class_3959
 *  net.minecraft.class_3959$class_242
 *  net.minecraft.class_3959$class_3960
 *  net.minecraft.class_3965
 */
package com.morwapi.aivillager.client;

import com.morwapi.aivillager.AIVillagerMod;
import com.morwapi.aivillager.networking.ModMessages;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.class_1297;
import net.minecraft.class_1646;
import net.minecraft.class_238;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3675;
import net.minecraft.class_3959;
import net.minecraft.class_3965;

public class KeyInputHandler {
    public static final String KEY_CATEGORY_AI_VILLAGER = "key.category.morwapi_ai_villager.tutorial";
    public static final String KEY_OPEN_INVENTORY = "key.morwapi_ai_villager.open_inventory";
    public static class_304 openInventoryKey;

    public static void registerKeyInputs() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openInventoryKey.method_1436()) {
                KeyInputHandler.openVillagerInventory(client);
            }
        });
    }

    private static void openVillagerInventory(class_310 client) {
        if (client.field_1724 == null || client.field_1687 == null) {
            return;
        }
        AIVillagerMod.LOGGER.info("Key pressed. Searching for villager...");
        double range = 20.0;
        class_238 searchBox = client.field_1724.method_5829().method_1014(range);
        List villagers = client.field_1687.method_8390(class_1646.class, searchBox, v -> true);
        if (villagers.isEmpty()) {
            AIVillagerMod.LOGGER.info("No villagers found in {} block radius.", (Object)range);
            return;
        }
        class_1646 bestTarget = null;
        double bestDot = -1.0;
        class_243 cameraPos = client.field_1724.method_5836(1.0f);
        class_243 lookVec = client.field_1724.method_5828(1.0f).method_1029();
        for (class_1646 villager : villagers) {
            class_243 toVillager = villager.method_5829().method_1005().method_1020(cameraPos).method_1029();
            double dot = lookVec.method_1026(toVillager);
            if (dot > 0.5 && dot > bestDot) {
                class_3959 context = new class_3959(cameraPos, villager.method_5829().method_1005(), class_3959.class_3960.field_23142, class_3959.class_242.field_1348, (class_1297)client.field_1724);
                class_3965 blockHit = client.field_1687.method_17742(context);
                if (blockHit.method_17783() == class_239.class_240.field_1333) {
                    bestTarget = villager;
                    bestDot = dot;
                    continue;
                }
                AIVillagerMod.LOGGER.info("Villager {} ignored (blocked by wall). Dot: {}", (Object)villager.method_5628(), (Object)dot);
                continue;
            }
            AIVillagerMod.LOGGER.info("Villager {} ignored (not looking at). Dot: {}", (Object)villager.method_5628(), (Object)dot);
        }
        if (bestTarget != null) {
            AIVillagerMod.LOGGER.info("Found target villager! ID: {} (Dot: {})", (Object)bestTarget.method_5628(), (Object)bestDot);
            class_2540 buf = PacketByteBufs.create();
            buf.writeInt(bestTarget.method_5628());
            ClientPlayNetworking.send((class_2960)ModMessages.OPEN_INVENTORY_ID, (class_2540)buf);
        } else {
            AIVillagerMod.LOGGER.info("No valid target found in cone of vision.");
        }
    }

    public static void register() {
        openInventoryKey = KeyBindingHelper.registerKeyBinding((class_304)new class_304(KEY_OPEN_INVENTORY, class_3675.class_307.field_1668, 86, KEY_CATEGORY_AI_VILLAGER));
        KeyInputHandler.registerKeyInputs();
    }
}

