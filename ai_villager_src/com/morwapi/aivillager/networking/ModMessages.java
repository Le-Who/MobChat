/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
 *  net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
 *  net.minecraft.class_1263
 *  net.minecraft.class_1297
 *  net.minecraft.class_1646
 *  net.minecraft.class_1657
 *  net.minecraft.class_1661
 *  net.minecraft.class_1703
 *  net.minecraft.class_2540
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_3222
 *  net.minecraft.class_3908
 */
package com.morwapi.aivillager.networking;

import com.morwapi.aivillager.ai.AIController;
import com.morwapi.aivillager.screen.VillagerControlScreenHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.class_1263;
import net.minecraft.class_1297;
import net.minecraft.class_1646;
import net.minecraft.class_1657;
import net.minecraft.class_1661;
import net.minecraft.class_1703;
import net.minecraft.class_2540;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_3908;

public class ModMessages {
    public static final class_2960 OPEN_INVENTORY_ID = new class_2960("morwapi_ai_villager", "open_inventory");
    public static final class_2960 VILLAGER_INSTRUCTION_ID = new class_2960("morwapi_ai_villager", "villager_instruction");

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver((class_2960)OPEN_INVENTORY_ID, (server, player, handler, buf, responseSender) -> {
            int entityId = buf.readInt();
            server.execute(() -> {
                class_1297 entity = player.method_37908().method_8469(entityId);
                if (entity instanceof class_1646) {
                    final class_1646 villager = (class_1646)entity;
                    player.method_17355((class_3908)new ExtendedScreenHandlerFactory(){

                        public class_2561 method_5476() {
                            return villager.method_5476();
                        }

                        public class_1703 createMenu(int syncId, class_1661 inv, class_1657 player) {
                            return new VillagerControlScreenHandler(syncId, inv, (class_1263)villager.method_35199());
                        }

                        public void writeScreenOpeningData(class_3222 player, class_2540 buf) {
                            buf.writeInt(villager.method_5628());
                        }
                    });
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver((class_2960)VILLAGER_INSTRUCTION_ID, (server, player, handler, buf, responseSender) -> {
            int entityId = buf.readInt();
            String instruction = buf.method_19772();
            server.execute(() -> {
                class_1297 entity = player.method_37908().method_8469(entityId);
                if (entity instanceof class_1646) {
                    class_1646 villager = (class_1646)entity;
                    AIController.processInstruction(villager, instruction, player);
                }
            });
        });
    }
}

