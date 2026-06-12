/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.class_1297
 *  net.minecraft.class_1299
 *  net.minecraft.class_1646
 *  net.minecraft.class_1937
 *  net.minecraft.class_2168
 *  net.minecraft.class_2170
 *  net.minecraft.class_2170$class_5364
 *  net.minecraft.class_2561
 *  net.minecraft.class_3218
 *  net.minecraft.class_3222
 *  net.minecraft.class_5575
 *  net.minecraft.class_7157
 */
package com.morwapi.aivillager.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.morwapi.aivillager.ai.AIController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1646;
import net.minecraft.class_1937;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_2561;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_5575;
import net.minecraft.class_7157;

public class VillagerControlCommands {
    private static final Map<UUID, String> lastTargetMap = new HashMap<UUID, String>();

    public static void register(CommandDispatcher<class_2168> dispatcher, class_7157 registryAccess, class_2170.class_5364 environment) {
        dispatcher.register((LiteralArgumentBuilder)class_2170.method_9247((String)"generate").then(class_2170.method_9244((String)"name", (ArgumentType)StringArgumentType.string()).executes(context -> VillagerControlCommands.executeGenerate((CommandContext<class_2168>)context))));
        dispatcher.register((LiteralArgumentBuilder)class_2170.method_9247((String)"control").then(class_2170.method_9244((String)"args", (ArgumentType)StringArgumentType.greedyString()).executes(context -> VillagerControlCommands.executeSmartControl((CommandContext<class_2168>)context))));
    }

    private static int executeGenerate(CommandContext<class_2168> context) {
        try {
            class_3222 player = ((class_2168)context.getSource()).method_9207();
            String name = StringArgumentType.getString(context, (String)"name");
            class_3218 world = player.method_51469();
            class_1646 villager = (class_1646)class_1299.field_6077.method_5883((class_1937)world);
            if (villager != null) {
                villager.method_5808(player.method_23317(), player.method_23318(), player.method_23321(), 0.0f, 0.0f);
                villager.method_5665(class_2561.method_30163((String)name));
                villager.method_5880(true);
                villager.method_5971();
                world.method_8649((class_1297)villager);
                ((class_2168)context.getSource()).method_9226(() -> class_2561.method_30163((String)("Summoned Villager named: " + name)), false);
                return 1;
            }
            ((class_2168)context.getSource()).method_9213(class_2561.method_30163((String)"Failed to create villager entity."));
            return 0;
        }
        catch (Exception e) {
            ((class_2168)context.getSource()).method_9213(class_2561.method_30163((String)("Error generating villager: " + e.getMessage())));
            return 0;
        }
    }

    private static int executeSmartControl(CommandContext<class_2168> context) {
        try {
            class_3222 player = ((class_2168)context.getSource()).method_9207();
            String args = StringArgumentType.getString(context, (String)"args");
            class_3218 world = ((class_2168)context.getSource()).method_9225();
            String[] parts = args.split(" ", 2);
            String potentialName = parts[0];
            String instruction = "";
            String targetName = "";
            boolean nameFound = false;
            class_5575 filter = class_5575.method_31795(class_1646.class);
            for (class_1297 entity : world.method_27909()) {
                if (!(entity instanceof class_1646) || !entity.method_16914() || !entity.method_5797().getString().equals(potentialName)) continue;
                nameFound = true;
                break;
            }
            if (nameFound) {
                targetName = potentialName;
                if (parts.length <= 1) {
                    ((class_2168)context.getSource()).method_9213(class_2561.method_30163((String)("Please provide an instruction for " + targetName)));
                    return 0;
                }
                instruction = parts[1];
                lastTargetMap.put(player.method_5667(), targetName);
            } else if (lastTargetMap.containsKey(player.method_5667())) {
                targetName = lastTargetMap.get(player.method_5667());
                instruction = args;
            } else {
                ((class_2168)context.getSource()).method_9213(class_2561.method_30163((String)("Villager '" + potentialName + "' not found, and no previous villager selected.")));
                return 0;
            }
            String finalTargetName = targetName;
            ArrayList<class_1646> villagers = new ArrayList<class_1646>();
            for (class_1297 entity : world.method_27909()) {
                if (!(entity instanceof class_1646)) continue;
                class_1646 villager = (class_1646)entity;
                if (!entity.method_16914() || !entity.method_5797().getString().equals(finalTargetName)) continue;
                villagers.add(villager);
            }
            if (villagers.isEmpty()) {
                ((class_2168)context.getSource()).method_9213(class_2561.method_30163((String)("Target villager '" + finalTargetName + "' not found (maybe despawned?).")));
                return 0;
            }
            class_1646 target = (class_1646)villagers.get(0);
            AIController.processInstruction(target, instruction, player);
            return 1;
        }
        catch (Exception e) {
            ((class_2168)context.getSource()).method_9213(class_2561.method_30163((String)("Error executing control: " + e.getMessage())));
            return 0;
        }
    }
}

