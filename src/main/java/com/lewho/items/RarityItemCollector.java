// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.items;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class RarityItemCollector {

    public static List<String> getItemsByRarity(Rarity rarity, int quantity) {
        List<String> itemsOfSpecificRarity = new ArrayList<>();

        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (stack.getRarity().equals(rarity) &&
                    !item.getName(stack).toString().contains("spawn_egg") &&
                    !item.getName(stack).toString().contains("jukebox") &&
                    !item.getName(stack).toString().contains("slab")) {
                itemsOfSpecificRarity.add(item.getDescriptionId());
            }
        }

        Collections.shuffle(itemsOfSpecificRarity);

        if (quantity >= itemsOfSpecificRarity.size()) {
            return itemsOfSpecificRarity;
        }

        return itemsOfSpecificRarity.subList(0, quantity);
    }

    public static List<String> getEntitiesByRarity(Rarity rarity, int quantity) {
        List<String> categoryCommonEntities = new ArrayList<>();
        List<String> categoryUncommonEntities = new ArrayList<>();
        List<String> categoryRareEntities = new ArrayList<>();
        List<String> entitiesOfSpecificRarity = new ArrayList<>();

        Set<String> commonEntities = new HashSet<>(Arrays.asList("creature"));
        Set<String> unCommonEntities = new HashSet<>(Arrays.asList("ambient", "water_ambient", "water_creature",
                "axolotl", "underground_water_creature", "zombie", "spider", "skeleton", "enderman", "drowned",
                "creeper", "slime", "silverfish", "cave_spider", "piglin", "witch", "zombie_villager"));
        Set<String> rareEntities = new HashSet<>(Arrays.asList("monster"));

        Set<String> excludedMonsters = new HashSet<>(Arrays.asList("ender_dragon", "phantom", "bat"));

        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            String entityName = entityType.toShortString();
            String spawnGroup = entityType.getCategory().getSerializedName();

            if (!excludedMonsters.contains(entityName)) {
                if (commonEntities.contains(spawnGroup) || commonEntities.contains(entityName)) {
                    categoryCommonEntities.add(entityName);
                } else if (unCommonEntities.contains(spawnGroup) || unCommonEntities.contains(entityName)) {
                    categoryUncommonEntities.add(entityName);
                } else if (rareEntities.contains(spawnGroup) || rareEntities.contains(entityName)) {
                    categoryRareEntities.add(entityName);
                }
            }
        }

        if (rarity == Rarity.COMMON) {
            entitiesOfSpecificRarity = categoryCommonEntities;
        } else if (rarity == Rarity.UNCOMMON) {
            entitiesOfSpecificRarity = categoryUncommonEntities;
        } else if (rarity == Rarity.RARE) {
            entitiesOfSpecificRarity = categoryRareEntities;
        }

        Collections.shuffle(entitiesOfSpecificRarity);

        if (quantity >= entitiesOfSpecificRarity.size()) {
            return entitiesOfSpecificRarity;
        }

        return entitiesOfSpecificRarity.subList(0, quantity);
    }
}

