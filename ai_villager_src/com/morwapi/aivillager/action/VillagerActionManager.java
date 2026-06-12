/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.shedaniel.autoconfig.AutoConfig
 *  net.minecraft.class_1263
 *  net.minecraft.class_1268
 *  net.minecraft.class_1277
 *  net.minecraft.class_1297
 *  net.minecraft.class_1304
 *  net.minecraft.class_1309
 *  net.minecraft.class_1646
 *  net.minecraft.class_1657
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1937
 *  net.minecraft.class_2183$class_2184
 *  net.minecraft.class_2246
 *  net.minecraft.class_2248
 *  net.minecraft.class_2302
 *  net.minecraft.class_2338
 *  net.minecraft.class_238
 *  net.minecraft.class_2421
 *  net.minecraft.class_2561
 *  net.minecraft.class_2586
 *  net.minecraft.class_2680
 *  net.minecraft.class_2741
 *  net.minecraft.class_2769
 *  net.minecraft.class_2960
 *  net.minecraft.class_3218
 *  net.minecraft.class_3222
 *  net.minecraft.class_3417
 *  net.minecraft.class_5250
 *  net.minecraft.class_7923
 */
package com.morwapi.aivillager.action;

import com.morwapi.aivillager.AIVillagerMod;
import com.morwapi.aivillager.access.FollowTargetAccessor;
import com.morwapi.aivillager.access.VillagerMemoryAccessor;
import com.morwapi.aivillager.action.ActionParser;
import com.morwapi.aivillager.ai.AIController;
import com.morwapi.aivillager.config.AIVillagerConfig;
import com.morwapi.aivillager.memory.VillagerMemory;
import com.morwapi.aivillager.mixin.LivingEntityAccessor;
import com.morwapi.aivillager.recipe.RecipeCache;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.class_1263;
import net.minecraft.class_1268;
import net.minecraft.class_1277;
import net.minecraft.class_1297;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1646;
import net.minecraft.class_1657;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1937;
import net.minecraft.class_2183;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2302;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_2421;
import net.minecraft.class_2561;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_2741;
import net.minecraft.class_2769;
import net.minecraft.class_2960;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_3417;
import net.minecraft.class_5250;
import net.minecraft.class_7923;

public class VillagerActionManager {
    private static void reportError(class_1646 villager, String actionType, String errorMessage) {
        String message = String.format("[%s] \u2717 %s failed: %s", villager.method_5477().getString(), actionType, errorMessage);
        class_1937 class_19372 = villager.method_37908();
        if (class_19372 instanceof class_3218) {
            class_3218 serverWorld = (class_3218)class_19372;
            for (class_3222 player : serverWorld.method_18456()) {
                player.method_7353((class_2561)class_2561.method_43470((String)message), false);
            }
        }
        AIVillagerMod.LOGGER.warn(message);
    }

    private static void reportErrorTranslatable(class_1646 villager, String actionType, String translationKey, Object ... args) {
        class_5250 errorText = class_2561.method_43469((String)translationKey, (Object[])args);
        String prefix = String.format("[%s] \u2717 %s failed: ", villager.method_5477().getString(), actionType);
        class_1937 class_19372 = villager.method_37908();
        if (class_19372 instanceof class_3218) {
            class_3218 serverWorld = (class_3218)class_19372;
            for (class_3222 player : serverWorld.method_18456()) {
                player.method_7353((class_2561)class_2561.method_43470((String)prefix).method_10852((class_2561)errorText), false);
            }
        }
        AIVillagerMod.LOGGER.warn(prefix + errorText.getString());
    }

    public static void execute(class_1646 villager, ActionParser.AIResponse response) {
        if (response.thought == null || !response.thought.isEmpty()) {
            // empty if block
        }
        for (int i = 0; i < response.actions.size(); ++i) {
            try {
                ActionParser.VillagerAction action = response.actions.get(i);
                AIVillagerMod.LOGGER.info("Processing action {}/{}: {}", new Object[]{i + 1, response.actions.size(), action.type});
                VillagerActionManager.executeAction(villager, action);
                continue;
            }
            catch (Exception e) {
                AIVillagerMod.LOGGER.error("Failed to execute action index " + i, (Throwable)e);
            }
        }
    }

    public static void executeAction(class_1646 villager, ActionParser.VillagerAction action) {
        if (action == null || action.type == null) {
            return;
        }
        try {
            switch (action.type.toUpperCase()) {
                case "CHAT": {
                    VillagerActionManager.executeChat(villager, action);
                    break;
                }
                case "MOVE": {
                    VillagerActionManager.executeMove(villager, action);
                    break;
                }
                case "JUMP": {
                    VillagerActionManager.executeJump(villager);
                    break;
                }
                case "LOOK": {
                    VillagerActionManager.executeLook(villager);
                    break;
                }
                case "MINE": {
                    VillagerActionManager.executeMine(villager, action);
                    break;
                }
                case "PLACE": {
                    VillagerActionManager.executePlace(villager, action);
                    break;
                }
                case "EQUIP": {
                    VillagerActionManager.executeEquip(villager, action);
                    break;
                }
                case "DROP": {
                    VillagerActionManager.executeDrop(villager, action);
                    break;
                }
                case "INTERACT": {
                    VillagerActionManager.executeInteract(villager, action);
                    break;
                }
                case "FOLLOW": {
                    VillagerActionManager.executeFollow(villager, action);
                    break;
                }
                case "STOP": {
                    VillagerActionManager.executeStop(villager);
                    break;
                }
                case "PATROL": {
                    VillagerActionManager.executePatrol(villager);
                    break;
                }
                case "ATTACK": {
                    VillagerActionManager.executeAttack(villager, action);
                    break;
                }
                case "WITHDRAW": {
                    VillagerActionManager.executeWithdraw(villager, action);
                    break;
                }
                case "DEPOSIT": {
                    VillagerActionManager.executeDeposit(villager, action);
                    break;
                }
                case "CRAFT": {
                    VillagerActionManager.executeCraft(villager, action);
                    break;
                }
                case "REMEMBER": {
                    VillagerActionManager.executeRemember(villager, action);
                    break;
                }
                case "FORGET": {
                    VillagerActionManager.executeForget(villager, action);
                    break;
                }
                case "TILL": {
                    VillagerActionManager.executeTill(villager, action);
                    break;
                }
                case "PLANT": {
                    VillagerActionManager.executePlant(villager, action);
                    break;
                }
                case "HARVEST": {
                    VillagerActionManager.executeHarvest(villager, action);
                    break;
                }
                case "CONTINUE": {
                    VillagerActionManager.executeContinue(villager);
                    break;
                }
                default: {
                    AIVillagerMod.LOGGER.warn("Unknown action type: {}", (Object)action.type);
                    break;
                }
            }
        }
        catch (Exception e) {
            AIVillagerMod.LOGGER.error("Error executing action " + action.type, (Throwable)e);
        }
    }

    private static void executeCraft(class_1646 villager, ActionParser.VillagerAction action) {
        try {
            boolean isCraftingTable;
            if (action.itemName == null) {
                AIVillagerMod.LOGGER.warn("Invalid CRAFT action: missing itemName");
                return;
            }
            String targetName = action.itemName.trim().toLowerCase();
            class_1937 class_19372 = villager.method_37908();
            if (class_19372 instanceof class_3218) {
                class_3218 serverWorld = (class_3218)class_19372;
                RecipeCache.init(serverWorld);
            }
            boolean bl = isCraftingTable = targetName.contains("crafting_table") || targetName.contains("crafting table") || targetName.contains("workbench") || targetName.contains("\u4f5c\u696d\u53f0");
            if (!isCraftingTable) {
                class_2338 villagerPos = villager.method_24515();
                class_2338 tablePos = null;
                int radius = 10;
                for (int x = -radius; x <= radius; ++x) {
                    for (int y = -radius; y <= radius; ++y) {
                        for (int z = -radius; z <= radius; ++z) {
                            class_2338 p = villagerPos.method_10069(x, y, z);
                            if (villager.method_37908().method_8320(p).method_26204() != class_2246.field_9980) continue;
                            tablePos = p;
                            break;
                        }
                        if (tablePos != null) break;
                    }
                    if (tablePos != null) break;
                }
                if (tablePos == null) {
                    VillagerActionManager.reportErrorTranslatable(villager, "CRAFT", "error.morwapi_ai_villager.craft.no_table", new Object[0]);
                    return;
                }
            }
            AIVillagerMod.LOGGER.info("Executing CRAFT action for: {}", (Object)targetName);
            ArrayList<String> missingTracker = new ArrayList<String>();
            if (VillagerActionManager.performCraft(villager, targetName, 0, missingTracker)) {
                AIVillagerMod.LOGGER.info("Successfully crafted {}", (Object)targetName);
            } else {
                String missingStr = missingTracker.isEmpty() ? "unknown recipe" : String.join((CharSequence)", ", missingTracker);
                VillagerActionManager.reportErrorTranslatable(villager, "CRAFT", "error.morwapi_ai_villager.craft.no_materials", targetName, missingStr);
            }
        }
        catch (Exception e) {
            AIVillagerMod.LOGGER.error("Error executing CRAFT action", (Throwable)e);
        }
    }

    private static boolean performCraft(class_1646 villager, String targetName, int depth, List<String> missingTracker) {
        if (depth > 3) {
            AIVillagerMod.LOGGER.warn("Crafting recursion depth exceeded for {}", (Object)targetName);
            missingTracker.add(targetName + " (too complex)");
            return false;
        }
        RecipeCache.SimpleRecipe recipe = RecipeCache.getRecipe(targetName);
        if (recipe == null) {
            AIVillagerMod.LOGGER.warn("No recipe found for {}", (Object)targetName);
            missingTracker.add(targetName);
            return false;
        }
        class_1277 tempInv = new class_1277(villager.method_35199().method_5439() + 2);
        for (int i = 0; i < villager.method_35199().method_5439(); ++i) {
            tempInv.method_5491(villager.method_35199().method_5438(i).method_7972());
        }
        if (!villager.method_6047().method_7960()) {
            tempInv.method_5491(villager.method_6047().method_7972());
        }
        if (!villager.method_6079().method_7960()) {
            tempInv.method_5491(villager.method_6079().method_7972());
        }
        boolean canCraft = true;
        ArrayList<List<String>> missingIngredients = new ArrayList<List<String>>();
        for (List<String> validIds : recipe.ingredients) {
            boolean bl = false;
            for (int k = 0; k < tempInv.method_5439(); ++k) {
                class_1799 stack = tempInv.method_5438(k);
                String stackId = class_7923.field_41178.method_10221((Object)stack.method_7909()).method_12832();
                if (!validIds.contains(stackId) || stack.method_7947() <= 0) continue;
                stack.method_7934(1);
                bl = true;
                break;
            }
            if (bl) continue;
            missingIngredients.add(validIds);
            canCraft = false;
        }
        if (canCraft) {
            for (List<String> validIds : recipe.ingredients) {
                VillagerActionManager.consumeIngredient(villager, validIds);
            }
            class_1799 result = recipe.output.method_7972();
            villager.method_35199().method_5491(result);
            villager.method_5783(class_3417.field_15215, 1.0f, 1.0f);
            AIVillagerMod.LOGGER.info("Crafted: {}", (Object)result.method_7964().getString());
            return true;
        }
        AIVillagerMod.LOGGER.info("Missing ingredients for {}. Attempting recursion (depth {})...", (Object)targetName, (Object)depth);
        boolean allMissingCrafted = true;
        for (List list : missingIngredients) {
            boolean craftedOneOption = false;
            for (String optionId : list) {
                if (RecipeCache.getRecipe(optionId) == null || !VillagerActionManager.performCraft(villager, optionId, depth + 1, missingTracker)) continue;
                craftedOneOption = true;
                break;
            }
            if (craftedOneOption) continue;
            allMissingCrafted = false;
            break;
        }
        if (allMissingCrafted) {
            return VillagerActionManager.performCraft(villager, targetName, depth, missingTracker);
        }
        return false;
    }

    private static void consumeIngredient(class_1646 villager, List<String> validIds) {
        class_1277 inventory = villager.method_35199();
        for (int i = 0; i < inventory.method_5439(); ++i) {
            class_1799 stack = inventory.method_5438(i);
            String stackId = class_7923.field_41178.method_10221((Object)stack.method_7909()).method_12832();
            if (!validIds.contains(stackId)) continue;
            inventory.method_5434(i, 1);
            AIVillagerMod.LOGGER.info("Consumed ingredient {} from slot {}", (Object)stackId, (Object)i);
            return;
        }
        if (VillagerActionManager.checkAndConsumeHand(villager.method_6047(), validIds)) {
            AIVillagerMod.LOGGER.info("Consumed ingredient from Main Hand");
            return;
        }
        if (VillagerActionManager.checkAndConsumeHand(villager.method_6079(), validIds)) {
            AIVillagerMod.LOGGER.info("Consumed ingredient from Off Hand");
            return;
        }
        AIVillagerMod.LOGGER.warn("Failed to consume ingredient! Expected one of: {}", validIds);
    }

    private static boolean checkAndConsumeHand(class_1799 stack, List<String> validIds) {
        if (stack.method_7960()) {
            return false;
        }
        String stackId = class_7923.field_41178.method_10221((Object)stack.method_7909()).method_12832();
        if (validIds.contains(stackId)) {
            stack.method_7934(1);
            return true;
        }
        return false;
    }

    private static void executeWithdraw(class_1646 villager, ActionParser.VillagerAction action) {
        if (action.x == null || action.y == null || action.z == null || action.itemName == null) {
            VillagerActionManager.reportErrorTranslatable(villager, "WITHDRAW", "error.morwapi_ai_villager.withdraw.no_coordinates", new Object[0]);
            return;
        }
        class_2338 pos = new class_2338((int)Math.floor(action.x), (int)Math.floor(action.y), (int)Math.floor(action.z));
        if (villager.method_5707(pos.method_46558()) > 100.0) {
            VillagerActionManager.reportErrorTranslatable(villager, "WITHDRAW", "error.morwapi_ai_villager.withdraw.too_far", new Object[0]);
            return;
        }
        class_2586 be = villager.method_37908().method_8321(pos);
        if (!(be instanceof class_1263)) {
            VillagerActionManager.reportErrorTranslatable(villager, "WITHDRAW", "error.morwapi_ai_villager.withdraw.not_container", new Object[0]);
            return;
        }
        class_1263 chestInv = (class_1263)be;
        String targetName = action.itemName.toLowerCase();
        int amountNeeded = action.count != null && action.count > 0 ? action.count : 64;
        int amountTransferred = 0;
        for (int i = 0; i < chestInv.method_5439(); ++i) {
            class_1799 stack = chestInv.method_5438(i);
            if (stack.method_7960() || !stack.method_7964().getString().toLowerCase().contains(targetName)) continue;
            int take = Math.min(stack.method_7947(), amountNeeded - amountTransferred);
            class_1799 takenStack = chestInv.method_5434(i, take);
            villager.method_35199().method_5491(takenStack);
            if (!takenStack.method_7960()) {
                chestInv.method_5447(i, stack);
                villager.method_5775(takenStack);
            }
            if ((amountTransferred += take) >= amountNeeded) break;
        }
        if (amountTransferred > 0) {
            AIVillagerMod.LOGGER.info("Villager withdrew {} {} from chest.", (Object)amountTransferred, (Object)action.itemName);
            villager.method_6104(class_1268.field_5808);
        } else {
            VillagerActionManager.reportErrorTranslatable(villager, "WITHDRAW", "error.morwapi_ai_villager.withdraw.item_not_found", action.itemName);
        }
    }

    private static void executeDeposit(class_1646 villager, ActionParser.VillagerAction action) {
        if (action.x == null || action.y == null || action.z == null || action.itemName == null) {
            VillagerActionManager.reportErrorTranslatable(villager, "DEPOSIT", "error.morwapi_ai_villager.deposit.no_coordinates", new Object[0]);
            return;
        }
        class_2338 pos = new class_2338((int)Math.floor(action.x), (int)Math.floor(action.y), (int)Math.floor(action.z));
        if (villager.method_5707(pos.method_46558()) > 100.0) {
            VillagerActionManager.reportErrorTranslatable(villager, "DEPOSIT", "error.morwapi_ai_villager.deposit.too_far", new Object[0]);
            return;
        }
        class_2586 be = villager.method_37908().method_8321(pos);
        if (!(be instanceof class_1263)) {
            VillagerActionManager.reportErrorTranslatable(villager, "DEPOSIT", "error.morwapi_ai_villager.deposit.not_container", new Object[0]);
            return;
        }
        class_1263 chestInv = (class_1263)be;
        String targetName = action.itemName.toLowerCase();
        int amountNeeded = action.count != null && action.count > 0 ? action.count : 64;
        int amountTransferred = 0;
        class_1799 mainHand = villager.method_6047();
        if (!mainHand.method_7960() && mainHand.method_7964().getString().toLowerCase().contains(targetName)) {
            int take = Math.min(mainHand.method_7947(), amountNeeded - amountTransferred);
            class_1799 toDeposit = mainHand.method_7971(take);
            class_1799 left = VillagerActionManager.addItemToInventory(chestInv, toDeposit);
            if (!left.method_7960()) {
                mainHand.method_7933(left.method_7947());
            } else {
                amountTransferred += take;
            }
            if (villager instanceof FollowTargetAccessor) {
                FollowTargetAccessor accessor = (FollowTargetAccessor)villager;
                accessor.setForcedMainHandStack(villager.method_6047());
            }
        }
        if (amountTransferred >= amountNeeded) {
            AIVillagerMod.LOGGER.info("Villager deposited {} {} into chest.", (Object)amountTransferred, (Object)action.itemName);
            villager.method_6104(class_1268.field_5808);
            return;
        }
        class_1277 villagerInv = villager.method_35199();
        for (int i = 0; i < villagerInv.method_5439(); ++i) {
            class_1799 stack = villagerInv.method_5438(i);
            if (stack.method_7960() || !stack.method_7964().getString().toLowerCase().contains(targetName)) continue;
            int take = Math.min(stack.method_7947(), amountNeeded - amountTransferred);
            class_1799 toDeposit = villagerInv.method_5434(i, take);
            class_1799 left = VillagerActionManager.addItemToInventory(chestInv, toDeposit);
            if (!left.method_7960()) {
                villagerInv.method_5491(left);
            } else {
                amountTransferred += take;
            }
            if (amountTransferred >= amountNeeded) break;
        }
        if (amountTransferred > 0) {
            AIVillagerMod.LOGGER.info("Villager deposited {} {} into chest.", (Object)amountTransferred, (Object)action.itemName);
            villager.method_6104(class_1268.field_5808);
        } else {
            VillagerActionManager.reportErrorTranslatable(villager, "DEPOSIT", "error.morwapi_ai_villager.deposit.item_not_found", action.itemName);
        }
    }

    private static class_1799 addItemToInventory(class_1263 inventory, class_1799 stack) {
        int i;
        for (i = 0; i < inventory.method_5439(); ++i) {
            class_1799 slotStack = inventory.method_5438(i);
            if (!class_1799.method_31577((class_1799)slotStack, (class_1799)stack)) continue;
            int space = slotStack.method_7914() - slotStack.method_7947();
            int add = Math.min(stack.method_7947(), space);
            slotStack.method_7933(add);
            stack.method_7934(add);
            if (!stack.method_7960()) continue;
            return class_1799.field_8037;
        }
        for (i = 0; i < inventory.method_5439(); ++i) {
            if (!inventory.method_5438(i).method_7960()) continue;
            inventory.method_5447(i, stack.method_7971(stack.method_7947()));
            return class_1799.field_8037;
        }
        return stack;
    }

    private static void executePatrol(class_1646 villager) {
        if (villager instanceof FollowTargetAccessor) {
            FollowTargetAccessor accessor = (FollowTargetAccessor)villager;
            accessor.setAiPatrol(true);
            AIVillagerMod.LOGGER.info("Villager started patrolling.");
        }
    }

    private static void executeAttack(class_1646 villager, ActionParser.VillagerAction action) {
        if (action.targetName == null || action.targetName.isEmpty()) {
            AIVillagerMod.LOGGER.warn("Invalid ATTACK action: targetName is null");
            return;
        }
        class_3218 world = (class_3218)villager.method_37908();
        class_3222 target = null;
        for (class_3222 player : world.method_18456()) {
            if (!player.method_5477().getString().equalsIgnoreCase(action.targetName)) continue;
            target = player;
            break;
        }
        if (target == null) {
            class_238 box = villager.method_5829().method_1014(50.0);
            List entities = world.method_8390(class_1309.class, box, e -> e != villager && e.method_16914() && e.method_5797().getString().equalsIgnoreCase(action.targetName));
            if (entities.isEmpty()) {
                entities = world.method_8390(class_1309.class, box, e -> e != villager && e.method_5864().method_5897().getString().equalsIgnoreCase(action.targetName));
            }
            if (!entities.isEmpty()) {
                target = (class_1309)entities.get(0);
            }
        }
        if (target != null) {
            if (villager instanceof FollowTargetAccessor) {
                FollowTargetAccessor accessor = (FollowTargetAccessor)villager;
                accessor.setAiAttackTarget((class_1309)target);
                AIVillagerMod.LOGGER.info("Villager started attacking {}", (Object)target.method_5477().getString());
            }
        } else {
            AIVillagerMod.LOGGER.warn("Could not find target '{}' to attack.", (Object)action.targetName);
        }
    }

    private static void executeFollow(class_1646 villager, ActionParser.VillagerAction action) {
        class_238 box;
        List entities;
        if (action.targetName == null || action.targetName.isEmpty()) {
            AIVillagerMod.LOGGER.warn("Invalid FOLLOW action: targetName is null");
            return;
        }
        class_3218 world = (class_3218)villager.method_37908();
        class_3222 target = null;
        for (class_3222 player : world.method_18456()) {
            if (!player.method_5477().getString().equalsIgnoreCase(action.targetName)) continue;
            target = player;
            break;
        }
        if (target == null && !(entities = world.method_8390(class_1309.class, box = villager.method_5829().method_1014(50.0), e -> e != villager && e.method_16914() && e.method_5797().getString().equalsIgnoreCase(action.targetName))).isEmpty()) {
            target = (class_1309)entities.get(0);
        }
        if (target != null) {
            if (villager instanceof FollowTargetAccessor) {
                FollowTargetAccessor accessor = (FollowTargetAccessor)villager;
                accessor.setAiFollowTarget((class_1309)target);
                AIVillagerMod.LOGGER.info("Villager started following {}", (Object)target.method_5477().getString());
            } else {
                AIVillagerMod.LOGGER.error("Villager does not implement FollowTargetAccessor!");
            }
        } else {
            AIVillagerMod.LOGGER.warn("Could not find target '{}' to follow.", (Object)action.targetName);
        }
    }

    private static void executeStop(class_1646 villager) {
        if (villager instanceof FollowTargetAccessor) {
            FollowTargetAccessor accessor = (FollowTargetAccessor)villager;
            accessor.setAiFollowTarget(null);
            accessor.setAiPatrol(false);
            accessor.setAiAttackTarget(null);
            villager.method_5942().method_6340();
            AIVillagerMod.LOGGER.info("Villager stopped following/moving/patrolling/attacking.");
        }
    }

    private static void executeInteract(class_1646 villager, ActionParser.VillagerAction action) {
        if (action.x != null && action.y != null && action.z != null) {
            class_2338 pos = new class_2338((int)Math.floor(action.x), (int)Math.floor(action.y), (int)Math.floor(action.z));
            class_3218 world = (class_3218)villager.method_37908();
            class_2680 state = world.method_8320(pos);
            class_2248 block = state.method_26204();
            villager.method_6104(class_1268.field_5808);
            AIVillagerMod.LOGGER.info("Villager interacting with {} at {}", (Object)state.method_26204().method_9518().getString(), (Object)pos);
            if (state.method_28498((class_2769)class_2741.field_12537)) {
                world.method_8501(pos, (class_2680)state.method_28493((class_2769)class_2741.field_12537));
            } else if (state.method_28498((class_2769)class_2741.field_12484)) {
                world.method_8501(pos, (class_2680)state.method_28493((class_2769)class_2741.field_12484));
            } else {
                AIVillagerMod.LOGGER.warn("Interaction with {} not fully supported yet (only Doors/Levers/Buttons).", (Object)state.method_26204().method_9518().getString());
            }
        } else {
            AIVillagerMod.LOGGER.warn("Invalid INTERACT action: coordinates are null");
        }
    }

    public static void executeAction(class_1646 villager, String json) {
        ActionParser.AIResponse response = ActionParser.parse(json);
        VillagerActionManager.execute(villager, response);
    }

    private static void executeChat(class_1646 villager, ActionParser.VillagerAction action) {
        class_1937 class_19372;
        if (action.message != null && (class_19372 = villager.method_37908()) instanceof class_3218) {
            class_3218 serverWorld = (class_3218)class_19372;
            serverWorld.method_18456().forEach(player -> {
                if (player.method_5858((class_1297)villager) < 400.0) {
                    player.method_7353(class_2561.method_30163((String)("<" + villager.method_5477().getString() + "> " + action.message)), false);
                }
            });
        }
    }

    private static void executeMove(class_1646 villager, ActionParser.VillagerAction action) {
        if (action.x != null && action.y != null && action.z != null) {
            AIVillagerMod.LOGGER.info("Attempting to move villager to: {}, {}, {}", new Object[]{action.x, action.y, action.z});
            boolean started = villager.method_5942().method_6337(action.x.doubleValue(), action.y.doubleValue(), action.z.doubleValue(), 0.6);
            if (!started) {
                VillagerActionManager.reportErrorTranslatable(villager, "MOVE", "error.morwapi_ai_villager.move.unreachable", new Object[0]);
            } else {
                AIVillagerMod.LOGGER.info("Navigation started successfully.");
            }
        } else {
            VillagerActionManager.reportErrorTranslatable(villager, "MOVE", "error.morwapi_ai_villager.move.no_coordinates", new Object[0]);
        }
    }

    private static void executeJump(class_1646 villager) {
        if (villager.method_24828()) {
            ((LivingEntityAccessor)villager).invokeJump();
            AIVillagerMod.LOGGER.info("Villager jumping (systematic).");
        } else {
            AIVillagerMod.LOGGER.info("Villager tried to jump but was not on ground.");
        }
    }

    private static void executeLook(class_1646 villager) {
        class_1657 nearestPlayer = villager.method_37908().method_18460((class_1297)villager, 10.0);
        if (nearestPlayer != null) {
            villager.method_5702(class_2183.class_2184.field_9851, nearestPlayer.method_33571());
            villager.method_5847(villager.method_36454());
            villager.method_5636(villager.method_36454());
            villager.method_5988().method_6226((class_1297)nearestPlayer, 30.0f, 30.0f);
            AIVillagerMod.LOGGER.info("Villager looking at player: {}", (Object)nearestPlayer.method_5477().getString());
        } else {
            AIVillagerMod.LOGGER.info("Villager tried to look, but no player nearby.");
        }
    }

    private static void executeMine(class_1646 villager, ActionParser.VillagerAction action) {
        if (action.x != null && action.y != null && action.z != null) {
            class_2338 pos = new class_2338((int)Math.floor(action.x), (int)Math.floor(action.y), (int)Math.floor(action.z));
            class_2680 state = villager.method_37908().method_8320(pos);
            VillagerActionManager.autoEquipBestTool(villager, state);
            villager.method_6104(class_1268.field_5808);
            villager.method_37908().method_8651(pos, true, (class_1297)villager);
            AIVillagerMod.LOGGER.info("Villager mined block at {}", (Object)pos);
        } else {
            VillagerActionManager.reportErrorTranslatable(villager, "MINE", "error.morwapi_ai_villager.mine.no_coordinates", new Object[0]);
        }
    }

    private static void autoEquipBestTool(class_1646 villager, class_2680 state) {
        class_1277 inventory = villager.method_35199();
        class_1799 currentStack = villager.method_6047();
        float currentSpeed = currentStack.method_7924(state);
        int bestSlot = -1;
        float bestSpeed = currentSpeed;
        for (int i = 0; i < inventory.method_5439(); ++i) {
            float speed;
            class_1799 stack = inventory.method_5438(i);
            if (stack.method_7960() || !((speed = stack.method_7924(state)) > bestSpeed)) continue;
            bestSpeed = speed;
            bestSlot = i;
        }
        if (bestSlot != -1) {
            class_1799 bestStack = inventory.method_5438(bestSlot);
            AIVillagerMod.LOGGER.info("Auto-equipping {} (Speed {}) for {}", new Object[]{bestStack.method_7964().getString(), Float.valueOf(bestSpeed), state.method_26204().method_9518().getString()});
            class_1799 newHand = bestStack.method_7971(bestStack.method_7947());
            if (villager instanceof FollowTargetAccessor) {
                FollowTargetAccessor accessor = (FollowTargetAccessor)villager;
                accessor.setForcedMainHandStack(newHand);
            } else {
                villager.method_6122(class_1268.field_5808, newHand);
            }
            inventory.method_5447(bestSlot, currentStack);
        }
    }

    private static void executePlace(class_1646 villager, ActionParser.VillagerAction action) {
        if (action.x != null && action.y != null && action.z != null && action.blockId != null) {
            class_2338 pos = new class_2338((int)Math.floor(action.x), (int)Math.floor(action.y), (int)Math.floor(action.z));
            class_2960 id = new class_2960(action.blockId);
            class_2248 block = (class_2248)class_7923.field_41175.method_10223(id);
            if (block != class_2246.field_10124) {
                villager.method_6104(class_1268.field_5808);
                villager.method_37908().method_8501(pos, block.method_9564());
                AIVillagerMod.LOGGER.info("Villager placed {} at {}", (Object)action.blockId, (Object)pos);
            } else {
                AIVillagerMod.LOGGER.warn("Invalid PLACE action: Unknown block ID {}", (Object)action.blockId);
            }
        } else {
            AIVillagerMod.LOGGER.warn("Invalid PLACE action: missing args");
        }
    }

    private static void executeEquip(class_1646 villager, ActionParser.VillagerAction action) {
        if (action.itemName == null) {
            AIVillagerMod.LOGGER.warn("Invalid EQUIP action: itemName is null");
            return;
        }
        class_1277 inventory = villager.method_35199();
        String targetName = action.itemName.toLowerCase();
        if (villager.method_6047().method_7964().getString().toLowerCase().contains(targetName)) {
            AIVillagerMod.LOGGER.info("Villager already holding {}", (Object)action.itemName);
            return;
        }
        for (int i = 0; i < inventory.method_5439(); ++i) {
            class_1799 stack = inventory.method_5438(i);
            if (stack.method_7960() || !stack.method_7964().getString().toLowerCase().contains(targetName)) continue;
            class_1799 currentHand = villager.method_6047();
            AIVillagerMod.LOGGER.info("Equipping: Found {} in slot {}. Current Hand: {}", new Object[]{stack.method_7964().getString(), i, currentHand.method_7964().getString()});
            class_1799 newHand = stack.method_7971(stack.method_7947());
            if (villager instanceof FollowTargetAccessor) {
                FollowTargetAccessor accessor = (FollowTargetAccessor)villager;
                accessor.setForcedMainHandStack(newHand);
            } else {
                villager.method_6122(class_1268.field_5808, newHand);
            }
            inventory.method_5447(i, currentHand);
            AIVillagerMod.LOGGER.info("Equipped. New Hand: {}", (Object)villager.method_6047().method_7964().getString());
            return;
        }
        AIVillagerMod.LOGGER.warn("Villager could not find {} to equip", (Object)action.itemName);
    }

    private static void executeDrop(class_1646 villager, ActionParser.VillagerAction action) {
        if (action.itemName == null || action.itemName.isEmpty()) {
            if (!villager.method_6047().method_7960()) {
                if (villager instanceof FollowTargetAccessor) {
                    FollowTargetAccessor accessor = (FollowTargetAccessor)villager;
                    accessor.setForcedMainHandStack(class_1799.field_8037);
                }
                villager.method_5775(villager.method_6047().method_7971(villager.method_6047().method_7947()));
                AIVillagerMod.LOGGER.info("Villager dropped held item");
            }
            return;
        }
        String targetName = action.itemName.toLowerCase();
        if (villager.method_6047().method_7964().getString().toLowerCase().contains(targetName)) {
            if (villager instanceof FollowTargetAccessor) {
                FollowTargetAccessor accessor = (FollowTargetAccessor)villager;
                accessor.setForcedMainHandStack(class_1799.field_8037);
            }
            villager.method_5775(villager.method_6047().method_7971(villager.method_6047().method_7947()));
            AIVillagerMod.LOGGER.info("Villager dropped held {}", (Object)action.itemName);
            return;
        }
        class_1277 inventory = villager.method_35199();
        for (int i = 0; i < inventory.method_5439(); ++i) {
            class_1799 stack = inventory.method_5438(i);
            if (stack.method_7960() || !stack.method_7964().getString().toLowerCase().contains(targetName)) continue;
            villager.method_5775(inventory.method_5441(i));
            AIVillagerMod.LOGGER.info("Villager dropped {} from inventory", (Object)action.itemName);
            return;
        }
        AIVillagerMod.LOGGER.warn("Villager could not find {} to drop", (Object)action.itemName);
    }

    private static void executeRemember(class_1646 villager, ActionParser.VillagerAction action) {
        if (action.memoryKey == null || action.memoryValue == null) {
            AIVillagerMod.LOGGER.warn("Invalid REMEMBER action: missing key or value");
            return;
        }
        if (villager instanceof VillagerMemoryAccessor) {
            VillagerMemoryAccessor accessor = (VillagerMemoryAccessor)villager;
            accessor.getAiMemory().set(action.memoryKey, action.memoryValue);
            AIVillagerMod.LOGGER.info("Villager remembered: {} = {}", (Object)action.memoryKey, (Object)action.memoryValue);
        } else {
            AIVillagerMod.LOGGER.error("Villager does not implement VillagerMemoryAccessor!");
        }
    }

    private static void executeForget(class_1646 villager, ActionParser.VillagerAction action) {
        if (action.memoryKey == null) {
            AIVillagerMod.LOGGER.warn("Invalid FORGET action: missing key");
            return;
        }
        if (villager instanceof VillagerMemoryAccessor) {
            VillagerMemoryAccessor accessor = (VillagerMemoryAccessor)villager;
            accessor.getAiMemory().remove(action.memoryKey);
            AIVillagerMod.LOGGER.info("Villager forgot: {}", (Object)action.memoryKey);
        } else {
            AIVillagerMod.LOGGER.error("Villager does not implement VillagerMemoryAccessor!");
        }
    }

    private static void executeTill(class_1646 villager, ActionParser.VillagerAction action) {
        if (action.coordinates == null || action.coordinates.length < 3) {
            VillagerActionManager.reportErrorTranslatable(villager, "TILL", "error.morwapi_ai_villager.till.no_coordinates", new Object[0]);
            return;
        }
        class_2338 targetPos = new class_2338((int)action.coordinates[0], (int)action.coordinates[1], (int)action.coordinates[2]);
        double distance = villager.method_5649((double)targetPos.method_10263() + 0.5, (double)targetPos.method_10264(), (double)targetPos.method_10260() + 0.5);
        AIVillagerMod.LOGGER.info("TILL: Villager at ({}, {}, {}), target at {}, distance: {}", new Object[]{villager.method_23317(), villager.method_23318(), villager.method_23321(), targetPos, Math.sqrt(distance)});
        if (distance > 10000.0) {
            VillagerActionManager.reportErrorTranslatable(villager, "TILL", "error.morwapi_ai_villager.too_far", Math.sqrt(distance));
            return;
        }
        class_2680 targetState = villager.method_37908().method_8320(targetPos);
        if (!targetState.method_27852(class_2246.field_10566) && !targetState.method_27852(class_2246.field_10219)) {
            VillagerActionManager.reportErrorTranslatable(villager, "TILL", "error.morwapi_ai_villager.till.cannot_till", new Object[0]);
            return;
        }
        if (!villager.method_37908().method_8320(targetPos.method_10084()).method_26215()) {
            VillagerActionManager.reportErrorTranslatable(villager, "TILL", "error.morwapi_ai_villager.till.block_above", new Object[0]);
            return;
        }
        if (!VillagerActionManager.equipBestHoe(villager)) {
            VillagerActionManager.reportErrorTranslatable(villager, "TILL", "error.morwapi_ai_villager.till.no_hoe", new Object[0]);
            return;
        }
        villager.method_37908().method_8501(targetPos, class_2246.field_10362.method_9564());
        class_1937 class_19372 = villager.method_37908();
        if (class_19372 instanceof class_3218) {
            class_3218 serverWorld = (class_3218)class_19372;
            serverWorld.method_14178().method_14128(targetPos);
        }
        AIVillagerMod.LOGGER.info("Villager tilled farmland at {}", (Object)targetPos);
        class_1799 hoe = villager.method_6047();
        if (hoe.method_7963()) {
            hoe.method_7956(1, (class_1309)villager, e -> {});
        }
    }

    private static void executePlant(class_1646 villager, ActionParser.VillagerAction action) {
        if (action.itemName == null) {
            VillagerActionManager.reportErrorTranslatable(villager, "PLANT", "error.morwapi_ai_villager.plant.no_crop", new Object[0]);
            return;
        }
        if (action.coordinates == null || action.coordinates.length < 3) {
            VillagerActionManager.reportErrorTranslatable(villager, "PLANT", "error.morwapi_ai_villager.plant.no_coordinates", new Object[0]);
            return;
        }
        class_2338 targetPos = new class_2338((int)action.coordinates[0], (int)action.coordinates[1], (int)action.coordinates[2]);
        double distance = villager.method_5649((double)targetPos.method_10263() + 0.5, (double)targetPos.method_10264(), (double)targetPos.method_10260() + 0.5);
        AIVillagerMod.LOGGER.info("PLANT: Villager at ({}, {}, {}), target at {}, distance: {}", new Object[]{villager.method_23317(), villager.method_23318(), villager.method_23321(), targetPos, Math.sqrt(distance)});
        if (distance > 10000.0) {
            VillagerActionManager.reportErrorTranslatable(villager, "PLANT", "error.morwapi_ai_villager.too_far", Math.sqrt(distance));
            return;
        }
        String cropName = action.itemName.toLowerCase();
        class_1792 cropItem = VillagerActionManager.getCropItem(cropName);
        if (cropItem == null) {
            VillagerActionManager.reportErrorTranslatable(villager, "PLANT", "error.morwapi_ai_villager.plant.unknown_crop", action.itemName);
            return;
        }
        if (!VillagerActionManager.hasItem(villager, cropItem)) {
            VillagerActionManager.reportErrorTranslatable(villager, "PLANT", "error.morwapi_ai_villager.no_item", action.itemName);
            return;
        }
        class_2680 targetState = villager.method_37908().method_8320(targetPos);
        if (targetState.method_26215()) {
            targetPos = targetPos.method_10074();
            targetState = villager.method_37908().method_8320(targetPos);
        }
        if ((targetState.method_27852(class_2246.field_10566) || targetState.method_27852(class_2246.field_10219)) && (cropName.contains("wheat") || cropName.contains("carrot") || cropName.contains("potato") || cropName.contains("beetroot") || cropName.contains("melon") || cropName.contains("pumpkin")) && VillagerActionManager.equipBestHoe(villager)) {
            villager.method_37908().method_8501(targetPos, class_2246.field_10362.method_9564());
            targetState = villager.method_37908().method_8320(targetPos);
            class_1937 class_19372 = villager.method_37908();
            if (class_19372 instanceof class_3218) {
                class_3218 serverWorld = (class_3218)class_19372;
                serverWorld.method_14178().method_14128(targetPos);
            }
        }
        if (!VillagerActionManager.isValidPlantingGround(cropName, targetState)) {
            VillagerActionManager.reportErrorTranslatable(villager, "PLANT", "error.morwapi_ai_villager.plant.cannot_plant", targetState.method_26204().method_9518().getString());
            return;
        }
        class_2338 abovePos = targetPos.method_10084();
        if (!villager.method_37908().method_8320(abovePos).method_26215()) {
            VillagerActionManager.reportErrorTranslatable(villager, "PLANT", "error.morwapi_ai_villager.plant.not_empty", new Object[0]);
            return;
        }
        class_2248 cropBlock = VillagerActionManager.getCropBlock(cropName);
        if (cropBlock != null) {
            villager.method_37908().method_8501(abovePos, cropBlock.method_9564());
            VillagerActionManager.consumeItem(villager, cropItem, 1);
            AIVillagerMod.LOGGER.info("Villager planted {} at {}", (Object)action.itemName, (Object)abovePos);
        }
    }

    private static void executeHarvest(class_1646 villager, ActionParser.VillagerAction action) {
        if (action.coordinates == null || action.coordinates.length < 3) {
            VillagerActionManager.reportErrorTranslatable(villager, "HARVEST", "error.morwapi_ai_villager.harvest.no_coordinates", new Object[0]);
            return;
        }
        class_2338 targetPos = new class_2338((int)action.coordinates[0], (int)action.coordinates[1], (int)action.coordinates[2]);
        double distance = villager.method_5649((double)targetPos.method_10263() + 0.5, (double)targetPos.method_10264(), (double)targetPos.method_10260() + 0.5);
        AIVillagerMod.LOGGER.info("HARVEST: Villager at ({}, {}, {}), target at {}, distance: {}", new Object[]{villager.method_23317(), villager.method_23318(), villager.method_23321(), targetPos, Math.sqrt(distance)});
        if (distance > 10000.0) {
            VillagerActionManager.reportErrorTranslatable(villager, "HARVEST", "error.morwapi_ai_villager.too_far", Math.sqrt(distance));
            return;
        }
        class_2680 cropState = villager.method_37908().method_8320(targetPos);
        class_2248 crop = cropState.method_26204();
        if (!VillagerActionManager.isCrop(crop)) {
            VillagerActionManager.reportErrorTranslatable(villager, "HARVEST", "error.morwapi_ai_villager.harvest.not_crop", new Object[0]);
            return;
        }
        if (!VillagerActionManager.isFullyGrown(cropState)) {
            VillagerActionManager.reportErrorTranslatable(villager, "HARVEST", "error.morwapi_ai_villager.harvest.not_mature", new Object[0]);
            return;
        }
        class_3218 world = (class_3218)villager.method_37908();
        List drops = class_2248.method_9562((class_2680)cropState, (class_3218)world, (class_2338)targetPos, null);
        class_1277 inventory = villager.method_35199();
        for (class_1799 drop : drops) {
            boolean added = false;
            for (int i = 0; i < inventory.method_5439(); ++i) {
                class_1799 existingStack = inventory.method_5438(i);
                if (existingStack.method_7960()) {
                    inventory.method_5447(i, drop);
                    added = true;
                    break;
                }
                if (!class_1799.method_31577((class_1799)existingStack, (class_1799)drop)) continue;
                int maxCount = Math.min(existingStack.method_7914(), inventory.method_5444());
                int combinedCount = existingStack.method_7947() + drop.method_7947();
                if (combinedCount <= maxCount) {
                    existingStack.method_7939(combinedCount);
                    added = true;
                    break;
                }
                int remainder = combinedCount - maxCount;
                existingStack.method_7939(maxCount);
                drop.method_7939(remainder);
            }
            if (added || drop.method_7960()) continue;
            villager.method_5775(drop);
        }
        if (VillagerActionManager.isReplantableCrop(crop)) {
            villager.method_37908().method_8501(targetPos, (class_2680)cropState.method_11657((class_2769)class_2302.field_10835, (Comparable)Integer.valueOf(0)));
            AIVillagerMod.LOGGER.info("Villager harvested and replanted at {}", (Object)targetPos);
        } else {
            villager.method_37908().method_8501(targetPos, class_2246.field_10124.method_9564());
            AIVillagerMod.LOGGER.info("Villager harvested at {}", (Object)targetPos);
        }
    }

    private static boolean equipBestHoe(class_1646 villager) {
        String[] hoes;
        for (String hoeName : hoes = new String[]{"diamond_hoe", "iron_hoe", "stone_hoe", "wooden_hoe", "golden_hoe"}) {
            class_1792 hoe = (class_1792)class_7923.field_41178.method_10223(new class_2960("minecraft", hoeName));
            if (!VillagerActionManager.findAndEquipTool(villager, hoe)) continue;
            return true;
        }
        return false;
    }

    private static boolean findAndEquipTool(class_1646 villager, class_1792 tool) {
        class_1277 inventory = villager.method_35199();
        for (int i = 0; i < inventory.method_5439(); ++i) {
            class_1799 stack = inventory.method_5438(i);
            if (stack.method_7909() != tool) continue;
            villager.method_5673(class_1304.field_6173, stack);
            return true;
        }
        return false;
    }

    private static class_1792 getCropItem(String cropName) {
        return switch (cropName) {
            case "wheat", "wheat_seeds" -> class_1802.field_8317;
            case "carrot", "carrots" -> class_1802.field_8179;
            case "potato", "potatoes" -> class_1802.field_8567;
            case "beetroot", "beetroot_seeds" -> class_1802.field_8309;
            case "pumpkin", "pumpkin_seeds" -> class_1802.field_8706;
            case "melon", "melon_seeds" -> class_1802.field_8188;
            case "sugar_cane" -> class_1802.field_17531;
            case "cactus" -> class_1802.field_17520;
            case "nether_wart" -> class_1802.field_8790;
            default -> null;
        };
    }

    private static class_2248 getCropBlock(String cropName) {
        return switch (cropName) {
            case "wheat", "wheat_seeds" -> class_2246.field_10293;
            case "carrot", "carrots" -> class_2246.field_10609;
            case "potato", "potatoes" -> class_2246.field_10247;
            case "beetroot", "beetroot_seeds" -> class_2246.field_10341;
            case "pumpkin", "pumpkin_seeds" -> class_2246.field_9984;
            case "melon", "melon_seeds" -> class_2246.field_10168;
            case "sugar_cane" -> class_2246.field_10424;
            case "cactus" -> class_2246.field_10029;
            case "nether_wart" -> class_2246.field_9974;
            default -> null;
        };
    }

    private static boolean isValidPlantingGround(String cropName, class_2680 ground) {
        return switch (cropName) {
            case "wheat", "wheat_seeds", "carrot", "carrots", "potato", "potatoes", "beetroot", "beetroot_seeds", "pumpkin", "pumpkin_seeds", "melon", "melon_seeds" -> ground.method_27852(class_2246.field_10362);
            case "sugar_cane" -> {
                if (ground.method_27852(class_2246.field_10219) || ground.method_27852(class_2246.field_10566) || ground.method_27852(class_2246.field_10102)) {
                    yield true;
                }
                yield false;
            }
            case "cactus" -> {
                if (ground.method_27852(class_2246.field_10102) || ground.method_27852(class_2246.field_10534)) {
                    yield true;
                }
                yield false;
            }
            case "nether_wart" -> {
                if (ground.method_27852(class_2246.field_10114) || ground.method_27852(class_2246.field_22090)) {
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    private static boolean hasItem(class_1646 villager, class_1792 item) {
        class_1277 inventory = villager.method_35199();
        for (int i = 0; i < inventory.method_5439(); ++i) {
            class_1799 stack = inventory.method_5438(i);
            if (stack.method_7909() != item || stack.method_7960()) continue;
            return true;
        }
        return false;
    }

    private static void consumeItem(class_1646 villager, class_1792 item, int count) {
        class_1277 inventory = villager.method_35199();
        for (int i = 0; i < inventory.method_5439(); ++i) {
            class_1799 stack = inventory.method_5438(i);
            if (stack.method_7909() != item || stack.method_7960()) continue;
            stack.method_7934(count);
            return;
        }
    }

    private static boolean isCrop(class_2248 block) {
        return block instanceof class_2302 || block == class_2246.field_10261 || block == class_2246.field_10545 || block == class_2246.field_10424 || block == class_2246.field_10029 || block == class_2246.field_9974;
    }

    private static boolean isFullyGrown(class_2680 state) {
        class_2248 class_22482 = state.method_26204();
        if (class_22482 instanceof class_2302) {
            class_2302 crop = (class_2302)class_22482;
            return crop.method_9825(state);
        }
        if (state.method_26204() == class_2246.field_9974) {
            return (Integer)state.method_11654((class_2769)class_2421.field_11306) == 3;
        }
        return state.method_26204() == class_2246.field_10261 || state.method_26204() == class_2246.field_10545 || state.method_26204() == class_2246.field_10424 || state.method_26204() == class_2246.field_10029;
    }

    private static boolean isReplantableCrop(class_2248 block) {
        return block instanceof class_2302;
    }

    private static void executeContinue(class_1646 villager) {
        if (villager instanceof VillagerMemoryAccessor) {
            VillagerMemoryAccessor accessor = (VillagerMemoryAccessor)villager;
            VillagerMemory memory = accessor.getAiMemory();
            String stepStr = memory.get("auto_step_count");
            int currentStep = stepStr != null ? Integer.parseInt(stepStr) : 0;
            AIVillagerConfig config = (AIVillagerConfig)AutoConfig.getConfigHolder(AIVillagerConfig.class).getConfig();
            if (currentStep >= config.maxAutoSteps) {
                AIVillagerMod.LOGGER.info("Max autonomous steps reached for " + villager.method_5477().getString());
                memory.remove("auto_step_count");
                return;
            }
            memory.set("auto_step_count", String.valueOf(currentStep + 1));
            AIVillagerMod.LOGGER.info("Executing autonomous step {}/{}", (Object)(currentStep + 1), (Object)config.maxAutoSteps);
            class_1937 class_19372 = villager.method_37908();
            if (class_19372 instanceof class_3218) {
                class_3218 serverWorld = (class_3218)class_19372;
                class_3222 player = (class_3222)serverWorld.method_18460((class_1297)villager, 20.0);
                serverWorld.method_8503().execute(() -> AIController.processInstruction(villager, "Continue previous task. If finished, say so.", player));
            }
        }
    }
}

