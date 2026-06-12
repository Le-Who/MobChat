/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1799
 *  net.minecraft.class_1935
 *  net.minecraft.class_2960
 *  net.minecraft.class_3218
 *  net.minecraft.class_7923
 */
package com.morwapi.aivillager.recipe;

import com.morwapi.aivillager.AIVillagerMod;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.class_1799;
import net.minecraft.class_1935;
import net.minecraft.class_2960;
import net.minecraft.class_3218;
import net.minecraft.class_7923;

public class RecipeCache {
    private static final Map<String, SimpleRecipe> cache = new HashMap<String, SimpleRecipe>();
    private static boolean initialized = false;

    public static void init(class_3218 world) {
        if (initialized) {
            return;
        }
        AIVillagerMod.LOGGER.info("Initializing RecipeCache with Hardcoded Core Recipes...");
        cache.clear();
        RecipeCache.addRecipe("oak_planks", 4, "oak_log");
        RecipeCache.addRecipe("spruce_planks", 4, "spruce_log");
        RecipeCache.addRecipe("birch_planks", 4, "birch_log");
        RecipeCache.addRecipe("jungle_planks", 4, "jungle_log");
        RecipeCache.addRecipe("acacia_planks", 4, "acacia_log");
        RecipeCache.addRecipe("dark_oak_planks", 4, "dark_oak_log");
        RecipeCache.addRecipe("mangrove_planks", 4, "mangrove_log");
        RecipeCache.addRecipe("cherry_planks", 4, "cherry_log");
        RecipeCache.addRecipe("bamboo_planks", 2, "bamboo_block");
        RecipeCache.addRecipe("stick", 4, "oak_planks", "oak_planks");
        RecipeCache.addRecipe("crafting_table", 1, "oak_planks", "oak_planks", "oak_planks", "oak_planks");
        RecipeCache.addRecipe("chest", 1, "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks");
        RecipeCache.addRecipe("furnace", 1, "cobblestone", "cobblestone", "cobblestone", "cobblestone", "cobblestone", "cobblestone", "cobblestone", "cobblestone");
        RecipeCache.addRecipe("torch", 4, "coal", "stick");
        RecipeCache.addRecipe("torch", 4, "charcoal", "stick");
        RecipeCache.addRecipe("wooden_sword", 1, "oak_planks", "oak_planks", "stick");
        RecipeCache.addRecipe("wooden_pickaxe", 1, "oak_planks", "oak_planks", "oak_planks", "stick", "stick");
        RecipeCache.addRecipe("wooden_axe", 1, "oak_planks", "oak_planks", "oak_planks", "stick", "stick");
        RecipeCache.addRecipe("wooden_shovel", 1, "oak_planks", "stick", "stick");
        RecipeCache.addRecipe("wooden_hoe", 1, "oak_planks", "oak_planks", "stick", "stick");
        RecipeCache.addRecipe("stone_sword", 1, "cobblestone", "cobblestone", "stick");
        RecipeCache.addRecipe("stone_pickaxe", 1, "cobblestone", "cobblestone", "cobblestone", "stick", "stick");
        RecipeCache.addRecipe("stone_axe", 1, "cobblestone", "cobblestone", "cobblestone", "stick", "stick");
        RecipeCache.addRecipe("stone_shovel", 1, "cobblestone", "stick", "stick");
        RecipeCache.addRecipe("stone_hoe", 1, "cobblestone", "cobblestone", "stick", "stick");
        RecipeCache.addRecipe("iron_sword", 1, "iron_ingot", "iron_ingot", "stick");
        RecipeCache.addRecipe("iron_pickaxe", 1, "iron_ingot", "iron_ingot", "iron_ingot", "stick", "stick");
        RecipeCache.addRecipe("iron_axe", 1, "iron_ingot", "iron_ingot", "iron_ingot", "stick", "stick");
        RecipeCache.addRecipe("iron_shovel", 1, "iron_ingot", "stick", "stick");
        RecipeCache.addRecipe("iron_hoe", 1, "iron_ingot", "iron_ingot", "stick", "stick");
        RecipeCache.addRecipe("iron_helmet", 1, "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot");
        RecipeCache.addRecipe("iron_chestplate", 1, "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot");
        RecipeCache.addRecipe("iron_leggings", 1, "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot");
        RecipeCache.addRecipe("iron_boots", 1, "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot");
        RecipeCache.addRecipe("diamond_sword", 1, "diamond", "diamond", "stick");
        RecipeCache.addRecipe("diamond_pickaxe", 1, "diamond", "diamond", "diamond", "stick", "stick");
        RecipeCache.addRecipe("diamond_axe", 1, "diamond", "diamond", "diamond", "stick", "stick");
        RecipeCache.addRecipe("diamond_shovel", 1, "diamond", "stick", "stick");
        RecipeCache.addRecipe("diamond_hoe", 1, "diamond", "diamond", "stick", "stick");
        RecipeCache.addRecipe("diamond_helmet", 1, "diamond", "diamond", "diamond", "diamond", "diamond");
        RecipeCache.addRecipe("diamond_chestplate", 1, "diamond", "diamond", "diamond", "diamond", "diamond", "diamond", "diamond", "diamond");
        RecipeCache.addRecipe("diamond_leggings", 1, "diamond", "diamond", "diamond", "diamond", "diamond", "diamond", "diamond");
        RecipeCache.addRecipe("diamond_boots", 1, "diamond", "diamond", "diamond", "diamond");
        RecipeCache.addRecipe("golden_sword", 1, "gold_ingot", "gold_ingot", "stick");
        RecipeCache.addRecipe("golden_pickaxe", 1, "gold_ingot", "gold_ingot", "gold_ingot", "stick", "stick");
        RecipeCache.addRecipe("golden_axe", 1, "gold_ingot", "gold_ingot", "gold_ingot", "stick", "stick");
        RecipeCache.addRecipe("golden_shovel", 1, "gold_ingot", "stick", "stick");
        RecipeCache.addRecipe("golden_hoe", 1, "gold_ingot", "gold_ingot", "stick", "stick");
        RecipeCache.addRecipe("golden_helmet", 1, "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot");
        RecipeCache.addRecipe("golden_chestplate", 1, "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot");
        RecipeCache.addRecipe("golden_leggings", 1, "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot");
        RecipeCache.addRecipe("golden_boots", 1, "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot");
        RecipeCache.addRecipe("leather_helmet", 1, "leather", "leather", "leather", "leather", "leather");
        RecipeCache.addRecipe("leather_chestplate", 1, "leather", "leather", "leather", "leather", "leather", "leather", "leather", "leather");
        RecipeCache.addRecipe("leather_leggings", 1, "leather", "leather", "leather", "leather", "leather", "leather", "leather");
        RecipeCache.addRecipe("leather_boots", 1, "leather", "leather", "leather", "leather");
        RecipeCache.addRecipe("redstone_torch", 1, "redstone", "stick");
        RecipeCache.addRecipe("repeater", 1, "redstone_torch", "redstone_torch", "redstone_torch", "stone");
        RecipeCache.addRecipe("comparator", 1, "redstone_torch", "redstone_torch", "redstone_torch", "quartz");
        RecipeCache.addRecipe("piston", 1, "oak_planks", "oak_planks", "oak_planks", "cobblestone", "cobblestone", "cobblestone", "iron_ingot", "redstone");
        RecipeCache.addRecipe("sticky_piston", 1, "piston", "slime_ball");
        RecipeCache.addRecipe("observer", 1, "cobblestone", "cobblestone", "cobblestone", "cobblestone", "cobblestone", "cobblestone", "redstone", "redstone", "quartz");
        RecipeCache.addRecipe("lever", 1, "stick", "cobblestone");
        RecipeCache.addRecipe("button", 1, "oak_planks");
        RecipeCache.addRecipe("stone_button", 1, "stone");
        RecipeCache.addRecipe("bread", 1, "wheat", "wheat", "wheat");
        RecipeCache.addRecipe("cookie", 8, "wheat", "cocoa_beans", "wheat");
        RecipeCache.addRecipe("cake", 1, "milk_bucket", "milk_bucket", "milk_bucket", "sugar", "egg", "sugar", "wheat", "wheat", "wheat");
        RecipeCache.addRecipe("mushroom_stew", 1, "bowl", "red_mushroom", "brown_mushroom");
        RecipeCache.addRecipe("bowl", 4, "oak_planks", "oak_planks", "oak_planks");
        RecipeCache.addRecipe("oak_stairs", 4, "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks");
        RecipeCache.addRecipe("spruce_stairs", 4, "spruce_planks", "spruce_planks", "spruce_planks", "spruce_planks", "spruce_planks", "spruce_planks");
        RecipeCache.addRecipe("birch_stairs", 4, "birch_planks", "birch_planks", "birch_planks", "birch_planks", "birch_planks", "birch_planks");
        RecipeCache.addRecipe("cobblestone_stairs", 4, "cobblestone", "cobblestone", "cobblestone", "cobblestone", "cobblestone", "cobblestone");
        RecipeCache.addRecipe("stone_stairs", 4, "stone", "stone", "stone", "stone", "stone", "stone");
        RecipeCache.addRecipe("stone_brick_stairs", 4, "stone_bricks", "stone_bricks", "stone_bricks", "stone_bricks", "stone_bricks", "stone_bricks");
        RecipeCache.addRecipe("oak_slab", 6, "oak_planks", "oak_planks", "oak_planks");
        RecipeCache.addRecipe("spruce_slab", 6, "spruce_planks", "spruce_planks", "spruce_planks");
        RecipeCache.addRecipe("birch_slab", 6, "birch_planks", "birch_planks", "birch_planks");
        RecipeCache.addRecipe("cobblestone_slab", 6, "cobblestone", "cobblestone", "cobblestone");
        RecipeCache.addRecipe("stone_slab", 6, "stone", "stone", "stone");
        RecipeCache.addRecipe("stone_brick_slab", 6, "stone_bricks", "stone_bricks", "stone_bricks");
        RecipeCache.addRecipe("oak_fence", 3, "oak_planks", "stick", "oak_planks", "stick", "oak_planks", "stick");
        RecipeCache.addRecipe("spruce_fence", 3, "spruce_planks", "stick", "spruce_planks", "stick", "spruce_planks", "stick");
        RecipeCache.addRecipe("birch_fence", 3, "birch_planks", "stick", "birch_planks", "stick", "birch_planks", "stick");
        RecipeCache.addRecipe("cobblestone_wall", 6, "cobblestone", "cobblestone", "cobblestone", "cobblestone", "cobblestone", "cobblestone");
        RecipeCache.addRecipe("stone_brick_wall", 6, "stone_bricks", "stone_bricks", "stone_bricks", "stone_bricks", "stone_bricks", "stone_bricks");
        RecipeCache.addRecipe("bow", 1, "stick", "stick", "stick", "string", "string", "string");
        RecipeCache.addRecipe("arrow", 4, "flint", "stick", "feather");
        RecipeCache.addRecipe("shield", 1, "oak_planks", "iron_ingot", "oak_planks", "oak_planks", "oak_planks", "oak_planks");
        RecipeCache.addRecipe("ladder", 3, "stick", "stick", "stick", "stick", "stick", "stick", "stick");
        RecipeCache.addRecipe("door", 3, "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks");
        RecipeCache.addRecipe("trapdoor", 2, "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks");
        RecipeCache.addRecipe("bed", 1, "oak_planks", "oak_planks", "oak_planks", "white_wool", "white_wool", "white_wool");
        RecipeCache.addRecipe("netherite_ingot", 1, "netherite_scrap", "netherite_scrap", "netherite_scrap", "netherite_scrap", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot");
        RecipeCache.addRecipe("netherite_sword", 1, "netherite_ingot", "diamond_sword");
        RecipeCache.addRecipe("netherite_pickaxe", 1, "netherite_ingot", "diamond_pickaxe");
        RecipeCache.addRecipe("netherite_axe", 1, "netherite_ingot", "diamond_axe");
        RecipeCache.addRecipe("netherite_shovel", 1, "netherite_ingot", "diamond_shovel");
        RecipeCache.addRecipe("netherite_hoe", 1, "netherite_ingot", "diamond_hoe");
        RecipeCache.addRecipe("netherite_helmet", 1, "netherite_ingot", "diamond_helmet");
        RecipeCache.addRecipe("netherite_chestplate", 1, "netherite_ingot", "diamond_chestplate");
        RecipeCache.addRecipe("netherite_leggings", 1, "netherite_ingot", "diamond_leggings");
        RecipeCache.addRecipe("netherite_boots", 1, "netherite_ingot", "diamond_boots");
        RecipeCache.addRecipe("nether_bricks", 1, "nether_brick", "nether_brick", "nether_brick", "nether_brick");
        RecipeCache.addRecipe("nether_brick", 1, "netherrack");
        RecipeCache.addRecipe("soul_torch", 4, "coal", "stick", "soul_soil");
        RecipeCache.addRecipe("soul_lantern", 1, "iron_nugget", "iron_nugget", "iron_nugget", "iron_nugget", "iron_nugget", "iron_nugget", "iron_nugget", "iron_nugget", "soul_torch");
        RecipeCache.addRecipe("ender_chest", 1, "obsidian", "obsidian", "obsidian", "obsidian", "obsidian", "obsidian", "obsidian", "obsidian", "ender_eye");
        RecipeCache.addRecipe("ender_eye", 1, "ender_pearl", "blaze_powder");
        RecipeCache.addRecipe("end_crystal", 1, "glass", "glass", "glass", "glass", "glass", "glass", "glass", "ender_eye", "ghast_tear");
        RecipeCache.addRecipe("end_rod", 4, "blaze_rod", "popped_chorus_fruit");
        RecipeCache.addRecipe("rail", 16, "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "stick");
        RecipeCache.addRecipe("powered_rail", 6, "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "stick", "redstone");
        RecipeCache.addRecipe("detector_rail", 6, "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "stone_pressure_plate", "redstone");
        RecipeCache.addRecipe("activator_rail", 6, "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "stick", "redstone_torch");
        RecipeCache.addRecipe("minecart", 1, "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot");
        RecipeCache.addRecipe("chest_minecart", 1, "minecart", "chest");
        RecipeCache.addRecipe("furnace_minecart", 1, "minecart", "furnace");
        RecipeCache.addRecipe("tnt_minecart", 1, "minecart", "tnt");
        RecipeCache.addRecipe("hopper_minecart", 1, "minecart", "hopper");
        RecipeCache.addRecipe("oak_boat", 1, "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks");
        RecipeCache.addRecipe("spruce_boat", 1, "spruce_planks", "spruce_planks", "spruce_planks", "spruce_planks", "spruce_planks");
        RecipeCache.addRecipe("birch_boat", 1, "birch_planks", "birch_planks", "birch_planks", "birch_planks", "birch_planks");
        RecipeCache.addRecipe("enchanting_table", 1, "book", "diamond", "diamond", "obsidian", "obsidian", "obsidian", "obsidian");
        RecipeCache.addRecipe("anvil", 1, "iron_block", "iron_block", "iron_block", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot");
        RecipeCache.addRecipe("brewing_stand", 1, "blaze_rod", "cobblestone", "cobblestone", "cobblestone");
        RecipeCache.addRecipe("cauldron", 1, "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot");
        RecipeCache.addRecipe("bookshelf", 1, "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "book", "book", "book");
        RecipeCache.addRecipe("book", 1, "paper", "paper", "paper", "leather");
        RecipeCache.addRecipe("paper", 3, "sugar_cane", "sugar_cane", "sugar_cane");
        RecipeCache.addRecipe("writable_book", 1, "book", "ink_sac", "feather");
        RecipeCache.addRecipe("grindstone", 1, "stick", "stick", "oak_planks", "oak_planks", "stone_slab");
        RecipeCache.addRecipe("smithing_table", 1, "iron_ingot", "iron_ingot", "oak_planks", "oak_planks", "oak_planks", "oak_planks");
        RecipeCache.addRecipe("composter", 1, "oak_slab", "oak_slab", "oak_slab", "oak_slab", "oak_slab", "oak_slab", "oak_slab");
        RecipeCache.addRecipe("barrel", 1, "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_slab", "oak_slab");
        RecipeCache.addRecipe("smoker", 1, "oak_log", "oak_log", "oak_log", "oak_log", "furnace");
        RecipeCache.addRecipe("blast_furnace", 1, "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "furnace", "smooth_stone", "smooth_stone", "smooth_stone");
        RecipeCache.addRecipe("hopper", 1, "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "chest");
        RecipeCache.addRecipe("dropper", 1, "cobblestone", "cobblestone", "cobblestone", "cobblestone", "cobblestone", "cobblestone", "cobblestone", "redstone");
        RecipeCache.addRecipe("dispenser", 1, "cobblestone", "cobblestone", "cobblestone", "cobblestone", "cobblestone", "cobblestone", "cobblestone", "bow", "redstone");
        RecipeCache.addRecipe("white_dye", 1, "bone_meal");
        RecipeCache.addRecipe("orange_dye", 1, "orange_tulip");
        RecipeCache.addRecipe("magenta_dye", 1, "lilac");
        RecipeCache.addRecipe("light_blue_dye", 1, "blue_orchid");
        RecipeCache.addRecipe("yellow_dye", 1, "dandelion");
        RecipeCache.addRecipe("lime_dye", 1, "green_dye", "white_dye");
        RecipeCache.addRecipe("pink_dye", 1, "pink_tulip");
        RecipeCache.addRecipe("gray_dye", 1, "black_dye", "white_dye");
        RecipeCache.addRecipe("light_gray_dye", 1, "gray_dye", "white_dye");
        RecipeCache.addRecipe("cyan_dye", 1, "blue_dye", "green_dye");
        RecipeCache.addRecipe("purple_dye", 1, "blue_dye", "red_dye");
        RecipeCache.addRecipe("blue_dye", 1, "cornflower");
        RecipeCache.addRecipe("brown_dye", 1, "cocoa_beans");
        RecipeCache.addRecipe("green_dye", 1, "cactus");
        RecipeCache.addRecipe("red_dye", 1, "poppy");
        RecipeCache.addRecipe("black_dye", 1, "ink_sac");
        RecipeCache.addRecipe("glass", 1, "sand");
        RecipeCache.addRecipe("glass_pane", 16, "glass", "glass", "glass", "glass", "glass", "glass");
        RecipeCache.addRecipe("white_stained_glass", 8, "glass", "glass", "glass", "glass", "glass", "glass", "glass", "glass", "white_dye");
        RecipeCache.addRecipe("terracotta", 1, "clay");
        RecipeCache.addRecipe("white_terracotta", 8, "terracotta", "terracotta", "terracotta", "terracotta", "terracotta", "terracotta", "terracotta", "terracotta", "white_dye");
        RecipeCache.addRecipe("white_concrete_powder", 8, "white_dye", "sand", "sand", "sand", "sand", "gravel", "gravel", "gravel", "gravel");
        RecipeCache.addRecipe("note_block", 1, "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "redstone");
        RecipeCache.addRecipe("jukebox", 1, "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "diamond");
        RecipeCache.addRecipe("redstone_lamp", 1, "redstone", "redstone", "redstone", "redstone", "glowstone");
        RecipeCache.addRecipe("daylight_detector", 1, "glass", "glass", "glass", "quartz", "quartz", "quartz", "oak_slab", "oak_slab", "oak_slab");
        RecipeCache.addRecipe("tripwire_hook", 2, "iron_ingot", "stick", "oak_planks");
        RecipeCache.addRecipe("trapped_chest", 1, "chest", "tripwire_hook");
        RecipeCache.addRecipe("target", 1, "redstone", "hay_block");
        RecipeCache.addRecipe("stone_bricks", 4, "stone", "stone", "stone", "stone");
        RecipeCache.addRecipe("bricks", 1, "brick", "brick", "brick", "brick");
        RecipeCache.addRecipe("brick", 1, "clay_ball");
        RecipeCache.addRecipe("quartz_block", 1, "quartz", "quartz", "quartz", "quartz");
        RecipeCache.addRecipe("sandstone", 1, "sand", "sand", "sand", "sand");
        RecipeCache.addRecipe("cut_sandstone", 4, "sandstone", "sandstone", "sandstone", "sandstone");
        RecipeCache.addRecipe("chiseled_sandstone", 1, "cut_sandstone_slab", "cut_sandstone_slab");
        RecipeCache.addRecipe("prismarine", 1, "prismarine_shard", "prismarine_shard", "prismarine_shard", "prismarine_shard");
        RecipeCache.addRecipe("sea_lantern", 1, "prismarine_shard", "prismarine_shard", "prismarine_shard", "prismarine_shard", "prismarine_crystals", "prismarine_crystals", "prismarine_crystals", "prismarine_crystals", "prismarine_crystals");
        RecipeCache.addRecipe("dried_kelp_block", 1, "dried_kelp", "dried_kelp", "dried_kelp", "dried_kelp", "dried_kelp", "dried_kelp", "dried_kelp", "dried_kelp", "dried_kelp");
        RecipeCache.addRecipe("hay_block", 1, "wheat", "wheat", "wheat", "wheat", "wheat", "wheat", "wheat", "wheat", "wheat");
        RecipeCache.addRecipe("bone_block", 1, "bone_meal", "bone_meal", "bone_meal", "bone_meal", "bone_meal", "bone_meal", "bone_meal", "bone_meal", "bone_meal");
        RecipeCache.addRecipe("iron_block", 1, "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot");
        RecipeCache.addRecipe("gold_block", 1, "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot");
        RecipeCache.addRecipe("diamond_block", 1, "diamond", "diamond", "diamond", "diamond", "diamond", "diamond", "diamond", "diamond", "diamond");
        RecipeCache.addRecipe("emerald_block", 1, "emerald", "emerald", "emerald", "emerald", "emerald", "emerald", "emerald", "emerald", "emerald");
        RecipeCache.addRecipe("lapis_block", 1, "lapis_lazuli", "lapis_lazuli", "lapis_lazuli", "lapis_lazuli", "lapis_lazuli", "lapis_lazuli", "lapis_lazuli", "lapis_lazuli", "lapis_lazuli");
        RecipeCache.addRecipe("redstone_block", 1, "redstone", "redstone", "redstone", "redstone", "redstone", "redstone", "redstone", "redstone", "redstone");
        RecipeCache.addRecipe("coal_block", 1, "coal", "coal", "coal", "coal", "coal", "coal", "coal", "coal", "coal");
        RecipeCache.addRecipe("bucket", 1, "iron_ingot", "iron_ingot", "iron_ingot");
        RecipeCache.addRecipe("shears", 1, "iron_ingot", "iron_ingot");
        RecipeCache.addRecipe("flint_and_steel", 1, "iron_ingot", "flint");
        RecipeCache.addRecipe("clock", 1, "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "redstone");
        RecipeCache.addRecipe("compass", 1, "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "redstone");
        RecipeCache.addRecipe("map", 1, "paper", "paper", "paper", "paper", "paper", "paper", "paper", "paper", "compass");
        RecipeCache.addRecipe("fishing_rod", 1, "stick", "stick", "stick", "string", "string");
        RecipeCache.addRecipe("carrot_on_a_stick", 1, "fishing_rod", "carrot");
        RecipeCache.addRecipe("warped_fungus_on_a_stick", 1, "fishing_rod", "warped_fungus");
        RecipeCache.addRecipe("spyglass", 1, "amethyst_shard", "copper_ingot", "copper_ingot");
        RecipeCache.addRecipe("lead", 2, "string", "string", "string", "string", "slime_ball");
        RecipeCache.addRecipe("name_tag", 1, "paper", "string");
        RecipeCache.addRecipe("painting", 1, "stick", "stick", "stick", "stick", "stick", "stick", "stick", "stick", "white_wool");
        RecipeCache.addRecipe("item_frame", 1, "stick", "stick", "stick", "stick", "stick", "stick", "stick", "stick", "leather");
        RecipeCache.addRecipe("glow_item_frame", 1, "item_frame", "glow_ink_sac");
        RecipeCache.addRecipe("armor_stand", 1, "stick", "stick", "stick", "stick", "stick", "stick", "stone_slab");
        RecipeCache.addRecipe("white_banner", 1, "white_wool", "white_wool", "white_wool", "white_wool", "white_wool", "white_wool", "stick");
        RecipeCache.addRecipe("flower_pot", 1, "brick", "brick", "brick");
        RecipeCache.addRecipe("beacon", 1, "glass", "glass", "glass", "glass", "glass", "nether_star", "obsidian", "obsidian", "obsidian");
        RecipeCache.addRecipe("conduit", 1, "nautilus_shell", "nautilus_shell", "nautilus_shell", "nautilus_shell", "nautilus_shell", "nautilus_shell", "nautilus_shell", "nautilus_shell", "heart_of_the_sea");
        RecipeCache.addRecipe("stonecutter", 1, "iron_ingot", "stone", "stone", "stone");
        RecipeCache.addRecipe("loom", 1, "string", "string", "oak_planks", "oak_planks");
        RecipeCache.addRecipe("cartography_table", 1, "paper", "paper", "oak_planks", "oak_planks", "oak_planks", "oak_planks");
        RecipeCache.addRecipe("fletching_table", 1, "flint", "flint", "oak_planks", "oak_planks", "oak_planks", "oak_planks");
        RecipeCache.addRecipe("lectern", 1, "oak_slab", "oak_slab", "oak_slab", "oak_slab", "bookshelf");
        RecipeCache.addRecipe("beehive", 1, "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "honeycomb", "honeycomb", "honeycomb");
        RecipeCache.addRecipe("respawn_anchor", 1, "crying_obsidian", "crying_obsidian", "crying_obsidian", "crying_obsidian", "crying_obsidian", "crying_obsidian", "glowstone", "glowstone", "glowstone");
        RecipeCache.addRecipe("lodestone", 1, "chiseled_stone_bricks", "netherite_ingot", "netherite_ingot", "netherite_ingot", "netherite_ingot", "netherite_ingot", "netherite_ingot", "netherite_ingot", "netherite_ingot");
        RecipeCache.addRecipe("lantern", 1, "iron_nugget", "iron_nugget", "iron_nugget", "iron_nugget", "iron_nugget", "iron_nugget", "iron_nugget", "iron_nugget", "torch");
        RecipeCache.addRecipe("campfire", 1, "stick", "stick", "stick", "coal", "oak_log", "oak_log", "oak_log");
        RecipeCache.addRecipe("soul_campfire", 1, "stick", "stick", "stick", "soul_soil", "oak_log", "oak_log", "oak_log");
        RecipeCache.addRecipe("candle", 1, "string", "honeycomb");
        RecipeCache.addRecipe("jack_o_lantern", 1, "carved_pumpkin", "torch");
        RecipeCache.addRecipe("carved_pumpkin", 1, "pumpkin");
        RecipeCache.addRecipe("glowstone", 1, "glowstone_dust", "glowstone_dust", "glowstone_dust", "glowstone_dust");
        RecipeCache.addRecipe("sea_pickle", 1, "pickle");
        RecipeCache.addRecipe("shroomlight", 1, "glowstone_dust", "glowstone_dust", "glowstone_dust", "glowstone_dust");
        RecipeCache.addRecipe("tnt", 1, "gunpowder", "sand", "gunpowder", "sand", "gunpowder", "sand", "gunpowder", "sand", "gunpowder");
        RecipeCache.addRecipe("fire_charge", 3, "blaze_powder", "coal", "gunpowder");
        RecipeCache.addRecipe("oak_pressure_plate", 1, "oak_planks", "oak_planks");
        RecipeCache.addRecipe("spruce_pressure_plate", 1, "spruce_planks", "spruce_planks");
        RecipeCache.addRecipe("birch_pressure_plate", 1, "birch_planks", "birch_planks");
        RecipeCache.addRecipe("jungle_pressure_plate", 1, "jungle_planks", "jungle_planks");
        RecipeCache.addRecipe("acacia_pressure_plate", 1, "acacia_planks", "acacia_planks");
        RecipeCache.addRecipe("dark_oak_pressure_plate", 1, "dark_oak_planks", "dark_oak_planks");
        RecipeCache.addRecipe("mangrove_pressure_plate", 1, "mangrove_planks", "mangrove_planks");
        RecipeCache.addRecipe("cherry_pressure_plate", 1, "cherry_planks", "cherry_planks");
        RecipeCache.addRecipe("bamboo_pressure_plate", 1, "bamboo_planks", "bamboo_planks");
        RecipeCache.addRecipe("crimson_pressure_plate", 1, "crimson_planks", "crimson_planks");
        RecipeCache.addRecipe("warped_pressure_plate", 1, "warped_planks", "warped_planks");
        RecipeCache.addRecipe("stone_pressure_plate", 1, "stone", "stone");
        RecipeCache.addRecipe("polished_blackstone_pressure_plate", 1, "polished_blackstone", "polished_blackstone");
        RecipeCache.addRecipe("light_weighted_pressure_plate", 1, "gold_ingot", "gold_ingot");
        RecipeCache.addRecipe("heavy_weighted_pressure_plate", 1, "iron_ingot", "iron_ingot");
        RecipeCache.addRecipe("oak_button", 1, "oak_planks");
        RecipeCache.addRecipe("spruce_button", 1, "spruce_planks");
        RecipeCache.addRecipe("birch_button", 1, "birch_planks");
        RecipeCache.addRecipe("jungle_button", 1, "jungle_planks");
        RecipeCache.addRecipe("acacia_button", 1, "acacia_planks");
        RecipeCache.addRecipe("dark_oak_button", 1, "dark_oak_planks");
        RecipeCache.addRecipe("mangrove_button", 1, "mangrove_planks");
        RecipeCache.addRecipe("cherry_button", 1, "cherry_planks");
        RecipeCache.addRecipe("bamboo_button", 1, "bamboo_planks");
        RecipeCache.addRecipe("crimson_button", 1, "crimson_planks");
        RecipeCache.addRecipe("warped_button", 1, "warped_planks");
        RecipeCache.addRecipe("polished_blackstone_button", 1, "polished_blackstone");
        RecipeCache.addRecipe("oak_fence_gate", 1, "stick", "oak_planks", "stick", "oak_planks");
        RecipeCache.addRecipe("spruce_fence_gate", 1, "stick", "spruce_planks", "stick", "spruce_planks");
        RecipeCache.addRecipe("birch_fence_gate", 1, "stick", "birch_planks", "stick", "birch_planks");
        RecipeCache.addRecipe("jungle_fence_gate", 1, "stick", "jungle_planks", "stick", "jungle_planks");
        RecipeCache.addRecipe("acacia_fence_gate", 1, "stick", "acacia_planks", "stick", "acacia_planks");
        RecipeCache.addRecipe("dark_oak_fence_gate", 1, "stick", "dark_oak_planks", "stick", "dark_oak_planks");
        RecipeCache.addRecipe("mangrove_fence_gate", 1, "stick", "mangrove_planks", "stick", "mangrove_planks");
        RecipeCache.addRecipe("cherry_fence_gate", 1, "stick", "cherry_planks", "stick", "cherry_planks");
        RecipeCache.addRecipe("bamboo_fence_gate", 1, "stick", "bamboo_planks", "stick", "bamboo_planks");
        RecipeCache.addRecipe("crimson_fence_gate", 1, "stick", "crimson_planks", "stick", "crimson_planks");
        RecipeCache.addRecipe("warped_fence_gate", 1, "stick", "warped_planks", "stick", "warped_planks");
        RecipeCache.addRecipe("oak_sign", 3, "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "oak_planks", "stick");
        RecipeCache.addRecipe("spruce_sign", 3, "spruce_planks", "spruce_planks", "spruce_planks", "spruce_planks", "spruce_planks", "spruce_planks", "stick");
        RecipeCache.addRecipe("birch_sign", 3, "birch_planks", "birch_planks", "birch_planks", "birch_planks", "birch_planks", "birch_planks", "stick");
        RecipeCache.addRecipe("jungle_sign", 3, "jungle_planks", "jungle_planks", "jungle_planks", "jungle_planks", "jungle_planks", "jungle_planks", "stick");
        RecipeCache.addRecipe("acacia_sign", 3, "acacia_planks", "acacia_planks", "acacia_planks", "acacia_planks", "acacia_planks", "acacia_planks", "stick");
        RecipeCache.addRecipe("dark_oak_sign", 3, "dark_oak_planks", "dark_oak_planks", "dark_oak_planks", "dark_oak_planks", "dark_oak_planks", "dark_oak_planks", "stick");
        RecipeCache.addRecipe("mangrove_sign", 3, "mangrove_planks", "mangrove_planks", "mangrove_planks", "mangrove_planks", "mangrove_planks", "mangrove_planks", "stick");
        RecipeCache.addRecipe("cherry_sign", 3, "cherry_planks", "cherry_planks", "cherry_planks", "cherry_planks", "cherry_planks", "cherry_planks", "stick");
        RecipeCache.addRecipe("bamboo_sign", 3, "bamboo_planks", "bamboo_planks", "bamboo_planks", "bamboo_planks", "bamboo_planks", "bamboo_planks", "stick");
        RecipeCache.addRecipe("crimson_sign", 3, "crimson_planks", "crimson_planks", "crimson_planks", "crimson_planks", "crimson_planks", "crimson_planks", "stick");
        RecipeCache.addRecipe("warped_sign", 3, "warped_planks", "warped_planks", "warped_planks", "warped_planks", "warped_planks", "warped_planks", "stick");
        RecipeCache.addRecipe("oak_hanging_sign", 6, "chain", "chain", "stripped_oak_log", "stripped_oak_log", "stripped_oak_log", "stripped_oak_log", "stripped_oak_log", "stripped_oak_log");
        RecipeCache.addRecipe("spruce_hanging_sign", 6, "chain", "chain", "stripped_spruce_log", "stripped_spruce_log", "stripped_spruce_log", "stripped_spruce_log", "stripped_spruce_log", "stripped_spruce_log");
        RecipeCache.addRecipe("birch_hanging_sign", 6, "chain", "chain", "stripped_birch_log", "stripped_birch_log", "stripped_birch_log", "stripped_birch_log", "stripped_birch_log", "stripped_birch_log");
        RecipeCache.addRecipe("jungle_hanging_sign", 6, "chain", "chain", "stripped_jungle_log", "stripped_jungle_log", "stripped_jungle_log", "stripped_jungle_log", "stripped_jungle_log", "stripped_jungle_log");
        RecipeCache.addRecipe("acacia_hanging_sign", 6, "chain", "chain", "stripped_acacia_log", "stripped_acacia_log", "stripped_acacia_log", "stripped_acacia_log", "stripped_acacia_log", "stripped_acacia_log");
        RecipeCache.addRecipe("dark_oak_hanging_sign", 6, "chain", "chain", "stripped_dark_oak_log", "stripped_dark_oak_log", "stripped_dark_oak_log", "stripped_dark_oak_log", "stripped_dark_oak_log", "stripped_dark_oak_log");
        RecipeCache.addRecipe("mangrove_hanging_sign", 6, "chain", "chain", "stripped_mangrove_log", "stripped_mangrove_log", "stripped_mangrove_log", "stripped_mangrove_log", "stripped_mangrove_log", "stripped_mangrove_log");
        RecipeCache.addRecipe("cherry_hanging_sign", 6, "chain", "chain", "stripped_cherry_log", "stripped_cherry_log", "stripped_cherry_log", "stripped_cherry_log", "stripped_cherry_log", "stripped_cherry_log");
        RecipeCache.addRecipe("bamboo_hanging_sign", 6, "chain", "chain", "stripped_bamboo_block", "stripped_bamboo_block", "stripped_bamboo_block", "stripped_bamboo_block", "stripped_bamboo_block", "stripped_bamboo_block");
        RecipeCache.addRecipe("crimson_hanging_sign", 6, "chain", "chain", "stripped_crimson_stem", "stripped_crimson_stem", "stripped_crimson_stem", "stripped_crimson_stem", "stripped_crimson_stem", "stripped_crimson_stem");
        RecipeCache.addRecipe("warped_hanging_sign", 6, "chain", "chain", "stripped_warped_stem", "stripped_warped_stem", "stripped_warped_stem", "stripped_warped_stem", "stripped_warped_stem", "stripped_warped_stem");
        RecipeCache.addRecipe("chain", 1, "iron_nugget", "iron_ingot", "iron_nugget");
        RecipeCache.addRecipe("crimson_planks", 4, "crimson_stem");
        RecipeCache.addRecipe("warped_planks", 4, "warped_stem");
        RecipeCache.addRecipe("spruce_door", 3, "spruce_planks", "spruce_planks", "spruce_planks", "spruce_planks", "spruce_planks", "spruce_planks");
        RecipeCache.addRecipe("birch_door", 3, "birch_planks", "birch_planks", "birch_planks", "birch_planks", "birch_planks", "birch_planks");
        RecipeCache.addRecipe("jungle_door", 3, "jungle_planks", "jungle_planks", "jungle_planks", "jungle_planks", "jungle_planks", "jungle_planks");
        RecipeCache.addRecipe("acacia_door", 3, "acacia_planks", "acacia_planks", "acacia_planks", "acacia_planks", "acacia_planks", "acacia_planks");
        RecipeCache.addRecipe("dark_oak_door", 3, "dark_oak_planks", "dark_oak_planks", "dark_oak_planks", "dark_oak_planks", "dark_oak_planks", "dark_oak_planks");
        RecipeCache.addRecipe("mangrove_door", 3, "mangrove_planks", "mangrove_planks", "mangrove_planks", "mangrove_planks", "mangrove_planks", "mangrove_planks");
        RecipeCache.addRecipe("cherry_door", 3, "cherry_planks", "cherry_planks", "cherry_planks", "cherry_planks", "cherry_planks", "cherry_planks");
        RecipeCache.addRecipe("bamboo_door", 3, "bamboo_planks", "bamboo_planks", "bamboo_planks", "bamboo_planks", "bamboo_planks", "bamboo_planks");
        RecipeCache.addRecipe("crimson_door", 3, "crimson_planks", "crimson_planks", "crimson_planks", "crimson_planks", "crimson_planks", "crimson_planks");
        RecipeCache.addRecipe("warped_door", 3, "warped_planks", "warped_planks", "warped_planks", "warped_planks", "warped_planks", "warped_planks");
        RecipeCache.addRecipe("iron_door", 3, "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot");
        RecipeCache.addRecipe("spruce_trapdoor", 2, "spruce_planks", "spruce_planks", "spruce_planks", "spruce_planks", "spruce_planks", "spruce_planks");
        RecipeCache.addRecipe("birch_trapdoor", 2, "birch_planks", "birch_planks", "birch_planks", "birch_planks", "birch_planks", "birch_planks");
        RecipeCache.addRecipe("jungle_trapdoor", 2, "jungle_planks", "jungle_planks", "jungle_planks", "jungle_planks", "jungle_planks", "jungle_planks");
        RecipeCache.addRecipe("acacia_trapdoor", 2, "acacia_planks", "acacia_planks", "acacia_planks", "acacia_planks", "acacia_planks", "acacia_planks");
        RecipeCache.addRecipe("dark_oak_trapdoor", 2, "dark_oak_planks", "dark_oak_planks", "dark_oak_planks", "dark_oak_planks", "dark_oak_planks", "dark_oak_planks");
        RecipeCache.addRecipe("mangrove_trapdoor", 2, "mangrove_planks", "mangrove_planks", "mangrove_planks", "mangrove_planks", "mangrove_planks", "mangrove_planks");
        RecipeCache.addRecipe("cherry_trapdoor", 2, "cherry_planks", "cherry_planks", "cherry_planks", "cherry_planks", "cherry_planks", "cherry_planks");
        RecipeCache.addRecipe("bamboo_trapdoor", 2, "bamboo_planks", "bamboo_planks", "bamboo_planks", "bamboo_planks", "bamboo_planks", "bamboo_planks");
        RecipeCache.addRecipe("crimson_trapdoor", 2, "crimson_planks", "crimson_planks", "crimson_planks", "crimson_planks", "crimson_planks", "crimson_planks");
        RecipeCache.addRecipe("warped_trapdoor", 2, "warped_planks", "warped_planks", "warped_planks", "warped_planks", "warped_planks", "warped_planks");
        RecipeCache.addRecipe("iron_trapdoor", 1, "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot");
        RecipeCache.addRecipe("golden_carrot", 1, "gold_nugget", "gold_nugget", "gold_nugget", "gold_nugget", "gold_nugget", "gold_nugget", "gold_nugget", "gold_nugget", "carrot");
        RecipeCache.addRecipe("golden_apple", 1, "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "gold_ingot", "apple");
        RecipeCache.addRecipe("sugar", 1, "sugar_cane");
        RecipeCache.addRecipe("pumpkin_pie", 1, "pumpkin", "sugar", "egg");
        RecipeCache.addRecipe("rabbit_stew", 1, "bowl", "cooked_rabbit", "carrot", "baked_potato", "mushroom");
        RecipeCache.addRecipe("copper_block", 1, "copper_ingot", "copper_ingot", "copper_ingot", "copper_ingot", "copper_ingot", "copper_ingot", "copper_ingot", "copper_ingot", "copper_ingot");
        RecipeCache.addRecipe("cut_copper", 4, "copper_block", "copper_block", "copper_block", "copper_block");
        RecipeCache.addRecipe("copper_grate", 4, "copper_block", "copper_block", "copper_block", "copper_block");
        RecipeCache.addRecipe("copper_bulb", 4, "copper_block", "blaze_rod", "redstone");
        RecipeCache.addRecipe("lightning_rod", 1, "copper_ingot", "copper_ingot", "copper_ingot");
        RecipeCache.addRecipe("bamboo_mosaic", 1, "bamboo_slab", "bamboo_slab");
        RecipeCache.addRecipe("bamboo_block", 1, "bamboo", "bamboo", "bamboo", "bamboo", "bamboo", "bamboo", "bamboo", "bamboo", "bamboo");
        RecipeCache.addRecipe("bamboo_raft", 1, "bamboo_planks", "bamboo_planks", "bamboo_planks", "bamboo_planks", "bamboo_planks");
        RecipeCache.addRecipe("scaffolding", 6, "bamboo", "bamboo", "bamboo", "bamboo", "bamboo", "bamboo", "string");
        RecipeCache.addRecipe("snow_block", 1, "snowball", "snowball", "snowball", "snowball");
        RecipeCache.addRecipe("honey_block", 1, "honey_bottle", "honey_bottle", "honey_bottle", "honey_bottle");
        RecipeCache.addRecipe("honeycomb_block", 1, "honeycomb", "honeycomb", "honeycomb", "honeycomb");
        RecipeCache.addRecipe("slime_block", 1, "slime_ball", "slime_ball", "slime_ball", "slime_ball", "slime_ball", "slime_ball", "slime_ball", "slime_ball", "slime_ball");
        RecipeCache.addRecipe("shulker_box", 1, "shulker_shell", "shulker_shell", "chest");
        RecipeCache.addRecipe("ender_pearl", 1, "ender_eye");
        RecipeCache.addRecipe("brush", 1, "feather", "copper_ingot", "stick");
        RecipeCache.addRecipe("decorated_pot", 1, "brick", "brick", "brick", "brick");
        initialized = true;
        AIVillagerMod.LOGGER.info("RecipeCache initialized with {} hardcoded recipes.", (Object)cache.size());
    }

    private static void addRecipe(String outputId, int count, String ... ingredientIds) {
        class_2960 id = new class_2960("minecraft", outputId);
        if (!class_7923.field_41178.method_10250(id)) {
            AIVillagerMod.LOGGER.warn("Skipping recipe for unknown item: {}", (Object)outputId);
            return;
        }
        class_1799 output = new class_1799((class_1935)class_7923.field_41178.method_10223(id), count);
        ArrayList<List<String>> ingredients = new ArrayList<List<String>>();
        for (String ingId : ingredientIds) {
            ArrayList<String> valid = new ArrayList<String>();
            valid.add(ingId);
            if (ingId.equals("oak_planks")) {
                valid.add("spruce_planks");
                valid.add("birch_planks");
                valid.add("jungle_planks");
                valid.add("acacia_planks");
                valid.add("dark_oak_planks");
                valid.add("mangrove_planks");
                valid.add("cherry_planks");
                valid.add("bamboo_planks");
            }
            if (ingId.equals("cobblestone")) {
                valid.add("cobbled_deepslate");
                valid.add("blackstone");
            }
            ingredients.add(valid);
        }
        cache.put(outputId, new SimpleRecipe(output, ingredients));
    }

    public static SimpleRecipe getRecipe(String itemId) {
        if (cache.containsKey(itemId)) {
            return cache.get(itemId);
        }
        for (String key : cache.keySet()) {
            if (!key.equals(itemId)) continue;
            return cache.get(key);
        }
        for (String key : cache.keySet()) {
            if (!key.contains(itemId) && !itemId.contains(key)) continue;
            return cache.get(key);
        }
        return null;
    }

    public static class SimpleRecipe {
        public final class_1799 output;
        public final List<List<String>> ingredients;

        public SimpleRecipe(class_1799 output, List<List<String>> ingredients) {
            this.output = output;
            this.ingredients = ingredients;
        }
    }
}

