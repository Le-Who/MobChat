/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1277
 *  net.minecraft.class_1297
 *  net.minecraft.class_1309
 *  net.minecraft.class_1646
 *  net.minecraft.class_1799
 *  net.minecraft.class_2244
 *  net.minecraft.class_2246
 *  net.minecraft.class_2248
 *  net.minecraft.class_2323
 *  net.minecraft.class_2338
 *  net.minecraft.class_2349
 *  net.minecraft.class_238
 *  net.minecraft.class_2382
 *  net.minecraft.class_243
 *  net.minecraft.class_2533
 *  net.minecraft.class_2680
 *  net.minecraft.class_7923
 */
package com.morwapi.aivillager.ai;

import com.morwapi.aivillager.AIVillagerMod;
import com.morwapi.aivillager.access.VillagerMemoryAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.class_1277;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1646;
import net.minecraft.class_1799;
import net.minecraft.class_2244;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2323;
import net.minecraft.class_2338;
import net.minecraft.class_2349;
import net.minecraft.class_238;
import net.minecraft.class_2382;
import net.minecraft.class_243;
import net.minecraft.class_2533;
import net.minecraft.class_2680;
import net.minecraft.class_7923;

public class ContextGatherer {
    public static String scanSurroundings(class_1646 villager, String instruction) {
        StringBuilder context = new StringBuilder();
        context.append("--- Current Surroundings ---\n");
        context.append(String.format("My Name: %s\n", villager.method_5477().getString()));
        context.append(String.format("My Position: %.1f, %.1f, %.1f\n", villager.method_23317(), villager.method_23318(), villager.method_23321()));
        context.append("Entities nearby:\n");
        String entityInfo = ContextGatherer.scanEntities(villager, 10.0);
        if (entityInfo.isEmpty()) {
            context.append("  (None)\n");
        } else {
            context.append(entityInfo);
        }
        context.append("Interesting Blocks nearby (radius 10):\n");
        String blockInfo = ContextGatherer.scanBlocks(villager, 10, instruction);
        if (blockInfo.isEmpty()) {
            context.append("  (None)\n");
        } else {
            context.append(blockInfo);
        }
        context.append("Inventory:\n");
        String invInfo = ContextGatherer.scanInventory(villager);
        if (invInfo.isEmpty()) {
            context.append("  (Empty)\n");
        } else {
            context.append(invInfo);
        }
        context.append("My Memories:\n");
        if (villager instanceof VillagerMemoryAccessor) {
            VillagerMemoryAccessor accessor = (VillagerMemoryAccessor)villager;
            Map<String, String> memories = accessor.getAiMemory().getAll();
            if (memories.isEmpty()) {
                context.append("  (None)\n");
            } else {
                for (Map.Entry<String, String> entry : memories.entrySet()) {
                    context.append(String.format("  - %s: %s\n", entry.getKey(), entry.getValue()));
                }
            }
        } else {
            context.append("  (Memory System Unavailable)\n");
        }
        context.append("----------------------------\n");
        String result = context.toString();
        AIVillagerMod.LOGGER.info("Context Scan:\n{}", (Object)result);
        return result;
    }

    private static String scanInventory(class_1646 villager) {
        class_1277 inventory;
        StringBuilder sb = new StringBuilder();
        class_1799 mainHand = villager.method_6047();
        if (!mainHand.method_7960()) {
            sb.append(String.format("  - Main Hand: %s x%d\n", mainHand.method_7964().getString(), mainHand.method_7947()));
        } else {
            sb.append("  - Main Hand: (Empty)\n");
        }
        class_1799 offHand = villager.method_6079();
        if (!offHand.method_7960()) {
            sb.append(String.format("  - Off Hand: %s x%d\n", offHand.method_7964().getString(), offHand.method_7947()));
        }
        if ((inventory = villager.method_35199()) != null) {
            ArrayList<String> items = new ArrayList<String>();
            for (int i = 0; i < inventory.method_5439(); ++i) {
                class_1799 stack = inventory.method_5438(i);
                if (stack.method_7960()) continue;
                items.add(String.format("%s x%d", stack.method_7964().getString(), stack.method_7947()));
            }
            if (!items.isEmpty()) {
                sb.append("  - Pockets: ").append(String.join((CharSequence)", ", items)).append("\n");
            }
        }
        return sb.toString();
    }

    private static String scanEntities(class_1646 villager, double radius) {
        class_238 box = villager.method_5829().method_1014(radius);
        List entities = villager.method_37908().method_8390(class_1309.class, box, e -> e != villager);
        if (entities.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int count = 0;
        int maxEntities = 5;
        for (class_1309 entity : entities) {
            String name = entity.method_5477().getString();
            String type = class_7923.field_41177.method_10221((Object)entity.method_5864()).method_12832();
            double dist = villager.method_5739((class_1297)entity);
            sb.append(String.format("  - %s (%s) at distance %.1f\n", name, type, dist));
            if (++count < maxEntities) continue;
            break;
        }
        return sb.toString();
    }

    private static String scanBlocks(class_1646 villager, int radius, String instruction) {
        class_2338 center = villager.method_24515();
        class_243 villagerPos = villager.method_19538();
        HashMap<String, List> interestingBlocks = new HashMap<String, List>();
        int maxBlocks = 15;
        int count = 0;
        Set<class_2248> whitelist = Set.of(class_2246.field_9980, class_2246.field_10034, class_2246.field_10181, class_2246.field_16333, class_2246.field_16334, class_2246.field_16328, class_2246.field_10120, class_2246.field_10149, class_2246.field_9973, class_2246.field_10363, class_2246.field_10494, class_2246.field_10057);
        for (int x = -radius; x <= radius; ++x) {
            for (int y = -3; y <= 3; ++y) {
                for (int z = -radius; z <= radius; ++z) {
                    boolean isGroundBlock;
                    boolean isInteresting;
                    class_2338 pos = center.method_10069(x, y, z);
                    class_2680 state = villager.method_37908().method_8320(pos);
                    class_2248 block = state.method_26204();
                    String blockId = class_7923.field_41175.method_10221((Object)block).method_12832();
                    String blockName = block.method_9518().getString().toLowerCase();
                    boolean bl = isInteresting = whitelist.contains(block) || block instanceof class_2323 || block instanceof class_2244 || block instanceof class_2349 || block instanceof class_2533 || blockId.contains("_log") || blockId.contains("_wood") || blockId.contains("_ore") || blockId.equals("stone") || blockId.equals("cobblestone") || blockId.equals("dirt") || blockId.equals("sand") || blockId.equals("gravel") || blockId.contains("leaves") || blockId.equals("farmland") || blockId.contains("wheat") || blockId.contains("carrot") || blockId.contains("potato") || blockId.contains("beetroot") || blockId.contains("melon") || blockId.contains("pumpkin") || blockId.contains("sugar_cane") || blockId.contains("cactus") || blockId.contains("nether_wart");
                    if (!isInteresting || state.method_26215()) continue;
                    boolean bl2 = isGroundBlock = blockId.equals("dirt") || blockId.equals("grass_block") || blockId.equals("sand") || blockId.equals("gravel") || blockId.equals("soul_sand") || blockId.equals("soul_soil");
                    if (isGroundBlock) {
                        class_2338 above = pos.method_10084();
                        if (!villager.method_37908().method_8320(above).method_26215()) continue;
                    }
                    String name = block.method_9518().getString();
                    double distance = villagerPos.method_1022(class_243.method_24954((class_2382)pos).method_1031(0.5, 0.5, 0.5));
                    interestingBlocks.computeIfAbsent(name, k -> new ArrayList()).add(new BlockPosWithDistance(pos, distance));
                    ++count;
                }
            }
        }
        if (interestingBlocks.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry entry : interestingBlocks.entrySet()) {
            String blockName = (String)entry.getKey();
            List positions = (List)entry.getValue();
            positions.sort((a, b) -> Double.compare(a.distance, b.distance));
            Object posList = positions.stream().limit(3L).map(p -> String.format("(%d,%d,%d) %.1fm", p.pos.method_10263(), p.pos.method_10264(), p.pos.method_10260(), p.distance)).collect(Collectors.joining(", "));
            if (positions.size() > 3) {
                posList = (String)posList + ", ... (" + positions.size() + " total)";
            }
            sb.append(String.format("  - %s: %s\n", blockName, posList));
        }
        return sb.toString();
    }

    private static class BlockPosWithDistance {
        final class_2338 pos;
        final double distance;

        BlockPosWithDistance(class_2338 pos, double distance) {
            this.pos = pos;
            this.distance = distance;
        }
    }
}

