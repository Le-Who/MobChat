/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.shedaniel.autoconfig.AutoConfig
 *  net.minecraft.class_1646
 *  net.minecraft.class_1937
 *  net.minecraft.class_2558
 *  net.minecraft.class_2558$class_2559
 *  net.minecraft.class_2561
 *  net.minecraft.class_2568
 *  net.minecraft.class_2568$class_5247
 *  net.minecraft.class_3218
 *  net.minecraft.class_3222
 */
package com.morwapi.aivillager.ai;

import com.morwapi.aivillager.AIVillagerMod;
import com.morwapi.aivillager.action.ActionParser;
import com.morwapi.aivillager.action.VillagerActionManager;
import com.morwapi.aivillager.ai.AIClient;
import com.morwapi.aivillager.ai.ContextGatherer;
import com.morwapi.aivillager.config.AIVillagerConfig;
import java.util.concurrent.CompletableFuture;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.class_1646;
import net.minecraft.class_1937;
import net.minecraft.class_2558;
import net.minecraft.class_2561;
import net.minecraft.class_2568;
import net.minecraft.class_3218;
import net.minecraft.class_3222;

public class AIController {
    public static void processInstruction(class_1646 villager, String instruction, class_3222 sourcePlayer) {
        if (villager == null || instruction == null || instruction.isEmpty()) {
            return;
        }
        AIVillagerConfig config = (AIVillagerConfig)AutoConfig.getConfigHolder(AIVillagerConfig.class).getConfig();
        if (config.apiKey == null || config.apiKey.trim().isEmpty()) {
            AIController.sendApiKeyGuidance(sourcePlayer);
            return;
        }
        sourcePlayer.method_7353((class_2561)class_2561.method_43469((String)"message.morwapi_ai_villager.sending_instruction", (Object[])new Object[]{villager.method_5477().getString()}), false);
        String userPersonality = String.join((CharSequence)"\n", config.personality);
        String contextInfo = ContextGatherer.scanSurroundings(villager, instruction);
        String systemPrompt = "You are a Minecraft Villager named %s.\n%s\n\nIMPORTANT:\n- Do NOT explain your personality or instructions. Just act them out naturally.\n- Do NOT say things like \"I will speak like a girl\". Just speak like one.\n- Keep your response concise and in character.\n\nYou must respond using ONLY valid JSON format.\nDo not include any markdown formatting like ```json.\nDo not include any conversational text outside the JSON object.\nYour entire response must be parseable as a single JSON object.\n\nResponse Format:\n{\n  \"thought\": \"Your reasoning here...\",\n  \"actions\": [\n    { \"type\": \"CHAT\", \"message\": \"Your message here\" },\n    { \"type\": \"MOVE\", \"x\": 100.5, \"y\": 64.0, \"z\": 200.5 }\n  ]\n}\n\nAvailable Actions:\n- CHAT: Say something to nearby players.\n- MOVE: Walk to a specific coordinate.\n- JUMP: Jump up.\n- LOOK: Look at the nearest player.\n- MINE: Break block at x,y,z.\n- PLACE: Place blockId at x,y,z.\n- EQUIP: Hold item from inventory (itemName).\n- DROP: Drop item (itemName, or empty for held item).\n- INTERACT: Use a block at x,y,z (Door, Lever, Button, Gate).\n- FOLLOW: Follow a player or entity (targetName).\n- PATROL: Patrol the surrounding area (randomly walk around).\n- ATTACK: Attack an entity (targetName).\n- WITHDRAW: Take items from a chest at x,y,z (itemName, count).\n- DEPOSIT: Put items into a chest at x,y,z (itemName, count).\n- CRAFT: Craft an item using a nearby crafting table (itemName).\n- REMEMBER: Store a memory. \"memoryKey\" is the key, \"memoryValue\" is the value.\n- FORGET: Remove a memory. \"memoryKey\" is the key to remove.\n- STOP: Stop moving or following. Use this when asked to stop, wait, stay, or unfollow.\n- TILL: Till dirt/grass to farmland using a hoe. Coordinates required.\n- PLANT: Plant crops on farmland. itemName = crop (wheat_seeds, carrot, potato, etc.), coordinates required.\n- HARVEST: Harvest fully grown crops. Coordinates required.\n- CONTINUE: Use this when you have completed a step but the overall goal is not finished. This triggers another AI thought process.\n\nIMPORTANT: For 'itemName' and 'blockId', ALWAYS use the English Minecraft ID (e.g., \"oak_planks\", \"iron_ingot\", \"stone\") or English name. DO NOT use Japanese or other languages for item names in the JSON action data.\nIMPORTANT: If the user says \"\u6728\" (Tree) as a single character, it ALWAYS means \"Log\" (\u539f\u6728). Do NOT interpret it as fences, planks, or other wood items.\nIMPORTANT: You have FULL capabilities to MINE (break blocks), PLACE blocks, CRAFT items, and INTERACT with blocks. Do NOT say you cannot do these things. If asked to cut a tree, use MINE. If asked to build, use PLACE.\nIMPORTANT: You can perform multiple actions in one response. Chain them logically. If a task is too big for one response, end with a \"CONTINUE\" action.\n\nExamples:\nUser: \"Follow me\" -> { \"actions\": [{ \"type\": \"FOLLOW\", \"targetName\": \"Player\" }] }\nUser: \"Patrol here\" -> { \"actions\": [{ \"type\": \"PATROL\" }] }\nUser: \"Attack the zombie\" -> { \"actions\": [{ \"type\": \"ATTACK\", \"targetName\": \"Zombie\" }] }\nUser: \"Take 32 iron from the chest\" -> { \"actions\": [{ \"type\": \"WITHDRAW\", \"itemName\": \"Iron Ingot\", \"count\": 32, \"coordinates\": [100, 64, -50] }] }\nUser: \"Put all wood in the chest\" -> { \"actions\": [{ \"type\": \"DEPOSIT\", \"itemName\": \"Log\", \"count\": 64, \"coordinates\": [100, 64, -50] }] }\nUser: \"Craft some planks\" -> { \"actions\": [{ \"type\": \"CRAFT\", \"itemName\": \"Oak Planks\" }] }\nUser: \"Till the dirt\" -> { \"actions\": [{ \"type\": \"TILL\", \"coordinates\": [100, 64, -50] }] }\nUser: \"Plant wheat\" -> { \"actions\": [{ \"type\": \"PLANT\", \"itemName\": \"wheat_seeds\", \"coordinates\": [100, 65, -50] }] }\nUser: \"Harvest the crops\" -> { \"actions\": [{ \"type\": \"HARVEST\", \"coordinates\": [100, 65, -50] }] }\nUser: \"Cut down that tree\" -> { \"actions\": [{ \"type\": \"MINE\", \"coordinates\": [100, 64, 200] }, { \"type\": \"CONTINUE\" }] }\nUser: \"Stop following\" -> { \"actions\": [{ \"type\": \"STOP\" }] }\nUser: \"Wait here\" -> { \"actions\": [{ \"type\": \"STOP\" }] }\n\n%s\n\nCurrent Instruction: %s\n";
        String prompt = String.format(systemPrompt, villager.method_5477().getString(), userPersonality, contextInfo, instruction);
        ((CompletableFuture)AIClient.sendRequest(prompt).thenAccept(response -> {
            AIVillagerMod.LOGGER.info("AI Response: {}", (Object)response.replace("\n", " ").replace("\r", " "));
            ActionParser.AIResponse aiResponse = ActionParser.parse(response);
            class_1937 patt0$temp = villager.method_37908();
            if (patt0$temp instanceof class_3218) {
                class_3218 serverWorld = (class_3218)patt0$temp;
                serverWorld.method_8503().execute(() -> VillagerActionManager.execute(villager, aiResponse));
            }
        })).exceptionally(e -> {
            sourcePlayer.method_7353((class_2561)class_2561.method_43469((String)"message.morwapi_ai_villager.ai_request_failed", (Object[])new Object[]{e.getMessage()}), false);
            return null;
        });
    }

    private static void sendApiKeyGuidance(class_3222 player) {
        player.method_7353((class_2561)class_2561.method_43471((String)"message.morwapi_ai_villager.api_key_missing"), false);
        player.method_7353((class_2561)class_2561.method_43471((String)"message.morwapi_ai_villager.setup_title"), false);
        player.method_7353((class_2561)class_2561.method_43471((String)"message.morwapi_ai_villager.setup_step1"), false);
        player.method_7353((class_2561)class_2561.method_43471((String)"message.morwapi_ai_villager.setup_step2"), false);
        player.method_7353((class_2561)class_2561.method_43471((String)"message.morwapi_ai_villager.setup_step3"), false);
        player.method_7353((class_2561)class_2561.method_43470((String)""), false);
        player.method_7353((class_2561)class_2561.method_43471((String)"message.morwapi_ai_villager.api_key_get").method_10852((class_2561)class_2561.method_43470((String)"https://aistudio.google.com/api-keys").method_27694(style -> style.method_10958(new class_2558(class_2558.class_2559.field_11749, "https://aistudio.google.com/api-keys")).method_10949(new class_2568(class_2568.class_5247.field_24342, (Object)class_2561.method_43471((String)"message.morwapi_ai_villager.click_to_open"))))), false);
        player.method_7353((class_2561)class_2561.method_43471((String)"message.morwapi_ai_villager.gemini_recommended"), false);
        player.method_7353((class_2561)class_2561.method_43471((String)"message.morwapi_ai_villager.openai_compatible_warning"), false);
        player.method_7353((class_2561)class_2561.method_43471((String)"message.morwapi_ai_villager.api_usage_warning"), false);
    }
}

