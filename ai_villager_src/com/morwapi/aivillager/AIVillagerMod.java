/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.shedaniel.autoconfig.AutoConfig
 *  me.shedaniel.autoconfig.serializer.GsonConfigSerializer
 *  net.fabricmc.api.ModInitializer
 *  net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
 *  net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
 *  net.minecraft.class_2378
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_3222
 *  net.minecraft.class_3917
 *  net.minecraft.class_7923
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.morwapi.aivillager;

import com.morwapi.aivillager.command.VillagerControlCommands;
import com.morwapi.aivillager.config.AIVillagerConfig;
import com.morwapi.aivillager.networking.ModMessages;
import com.morwapi.aivillager.screen.VillagerControlScreenHandler;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.class_2378;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_3917;
import net.minecraft.class_7923;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AIVillagerMod
implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"morwapi_ai_villager");
    public static final class_3917<VillagerControlScreenHandler> VILLAGER_CONTROL_SCREEN_HANDLER = (class_3917)class_2378.method_10230((class_2378)class_7923.field_41187, (class_2960)new class_2960("morwapi_ai_villager", "villager_control"), (Object)new ExtendedScreenHandlerType(VillagerControlScreenHandler::new));

    public void onInitialize() {
        LOGGER.info("AI Villager Mod Initialized!");
        AutoConfig.register(AIVillagerConfig.class, GsonConfigSerializer::new);
        CommandRegistrationCallback.EVENT.register(VillagerControlCommands::register);
        ModMessages.registerC2SPackets();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            AIVillagerConfig config = (AIVillagerConfig)AutoConfig.getConfigHolder(AIVillagerConfig.class).getConfig();
            if (!config.hasShownWelcomeMessage) {
                AIVillagerMod.sendWelcomeMessage(handler.field_14140);
                config.hasShownWelcomeMessage = true;
                AutoConfig.getConfigHolder(AIVillagerConfig.class).save();
            }
        });
    }

    private static void sendWelcomeMessage(class_3222 player) {
        player.method_7353((class_2561)class_2561.method_43470((String)""), false);
        player.method_7353((class_2561)class_2561.method_43471((String)"message.morwapi_ai_villager.welcome.summon"), false);
        player.method_7353((class_2561)class_2561.method_43471((String)"message.morwapi_ai_villager.welcome.control"), false);
        player.method_7353((class_2561)class_2561.method_43470((String)""), false);
    }
}

