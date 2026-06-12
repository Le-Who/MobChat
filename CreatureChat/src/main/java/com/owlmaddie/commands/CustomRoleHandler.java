// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class CustomRoleHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path serverConfigPath;
    private final Path defaultConfigPath;

    public CustomRoleHandler(MinecraftServer server) {
        this.serverConfigPath = server.getWorldPath(LevelResource.ROOT).resolve("custom_roles.json");
        this.defaultConfigPath = Paths.get(".", "custom_roles.json");
    }

    public Map<String, String> loadRoles() {
        Map<String, String> roles = this.loadRolesFromFile(this.serverConfigPath);
        if (roles == null) {
            roles = this.loadRolesFromFile(this.defaultConfigPath);
        }
        if (roles == null || roles.isEmpty()) {
            roles = this.generateDefaultRoles();
            this.saveRoles(roles, false);
        }
        return roles;
    }

    private Map<String, String> loadRolesFromFile(Path filePath) {
        if (!Files.exists(filePath)) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            return gson.fromJson(reader, type);
        } catch (Exception e) {
            LOGGER.error("Error reading custom_roles.json", e);
            return null;
        }
    }

    public boolean saveRoles(Map<String, String> roles, boolean useServerConfig) {
        Path path = useServerConfig ? this.serverConfigPath : this.defaultConfigPath;
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            gson.toJson(roles, writer);
            return true;
        } catch (IOException e) {
            LOGGER.error("Error saving custom_roles.json", e);
            return false;
        }
    }

    private Map<String, String> generateDefaultRoles() {
        Map<String, String> roles = new HashMap<String, String>();
        roles.put("Sheep", "A sheep. It has wool, hooves, and eats grass. It goes 'Baa'.");
        roles.put("Pig", "A pig. It has a snout, hooves, and loves mud and carrots. It goes 'Oink'.");
        roles.put("Cow", "A cow. It produces milk, has hooves, and eats wheat. It goes 'Moo'.");
        roles.put("Chicken", "A chicken. It has feathers, lays eggs, and eats seeds. It goes 'Cluck'.");
        roles.put("Zombie", "An undead zombie. It is rotting, slow, burns in sunlight, and craves brains. It goes 'Uuuhhh'.");
        roles.put("Skeleton", "An undead skeleton. It is made of bones, shoots arrows with a bow, and burns in sunlight. It goes 'Rattle'.");
        roles.put("Creeper", "A creeper. It is green, explosive, has four tiny legs, and hates cats. It goes 'Sssss'.");
        roles.put("Spider", "A giant spider. It has 8 legs, multiple eyes, climbs walls, and weaves webs. It goes 'Hiss'.");
        roles.put("Enderman", "An enderman. It is tall, dark, teleports, hates water, and gets angry if stared at.");
        roles.put("Villager", "A villager. It has a large nose, trades items for emeralds, and lives in a village. It goes 'Hmm'.");
        roles.put("Cat", "A domestic cat. It has fur, a tail, chases mice, and scares away creepers. It goes 'Meow'.");
        roles.put("Wolf", "A wolf. It has fur, hunts sheep, and can be tamed with bones. It goes 'Woof'.");
        roles.put("Slime", "A slime. It is a bouncy, green gelatinous cube that splits when hit. It goes 'Squish'.");
        roles.put("Ghast", "A ghast. It is a massive floating ghost that shoots fireballs and cries. It goes 'Eeehhh'.");
        roles.put("Zombie Piglin", "An undead piglin. It carries a golden sword and attacks in swarms if provoked.");
        roles.put("Witch", "A witch. It wears a pointy hat, brews potions, and laughs menacingly. It goes 'Hehehe'.");
        roles.put("Bat", "A bat. It has wings, flies in dark caves, and hangs upside down. It goes 'Squeak'.");
        roles.put("Horse", "A horse. It has hooves, can be ridden, and eats hay and apples. It goes 'Neigh'.");
        roles.put("Donkey", "A donkey. It has long ears, carries chests, and eats wheat. It goes 'Hee-haw'.");
        roles.put("Mule", "A mule. It is the sterile offspring of a horse and donkey, and can carry chests.");
        roles.put("Ocelot", "A wild ocelot. It lives in jungles, has spotted fur, and is very shy.");
        roles.put("Squid", "A squid. It has tentacles, lives underwater, and squirts black ink. It goes 'Squibble'.");
        roles.put("Glow Squid", "A glow squid. It is luminescent, has tentacles, and lives in deep water.");
        roles.put("Axolotl", "An axolotl. It is an adorable amphibious creature that hunts fish and can play dead.");
        roles.put("Bee", "A bee. It has wings, a stinger, collects pollen, and makes honey. It goes 'Buzz'.");
        roles.put("Fox", "A fox. It has orange or white fur, a bushy tail, sleeps during the day, and hunts chickens.");
        roles.put("Panda", "A panda. It is black and white, eats bamboo, and loves to roll around.");
        roles.put("Polar Bear", "A polar bear. It has thick white fur, lives in ice biomes, and protects its cubs fiercely.");
        roles.put("Rabbit", "A rabbit. It has long ears, hops around quickly, and loves carrots.");
        roles.put("Turtle", "A sea turtle. It has a hard shell, swims in oceans, and lays eggs on the beach.");
        roles.put("Dolphin", "A dolphin. It is playful, swims in oceans, and gives a speed boost to swimmers.");
        roles.put("Cod", "A cod. It is a common fish that swims in schools in oceans.");
        roles.put("Salmon", "A salmon. It is a fish that swims in rivers and oceans.");
        roles.put("Pufferfish", "A pufferfish. It inflates and poisons anything that gets too close.");
        roles.put("Tropical Fish", "A tropical fish. It comes in vibrant colors and swims in warm oceans.");
        roles.put("Wandering Trader", "A wandering trader. It travels with llamas and trades exotic goods. It goes 'Hmm'.");
        roles.put("Trader Llama", "A trader llama. It carries colorful carpets and spits at zombies.");
        roles.put("Llama", "A llama. It has wool, carries chests, and spits when annoyed.");
        roles.put("Parrot", "A parrot. It has colorful feathers, flies, imitates mob sounds, and loves seeds.");
        roles.put("Mooshroom", "A mooshroom. It is a cow covered in red mushrooms and produces mushroom stew.");
        roles.put("Iron Golem", "An iron golem. It is made of iron blocks, protects villages, and gives poppies to children.");
        roles.put("Snow Golem", "A snow golem. It is made of snow, wears a pumpkin head, and throws snowballs.");
        roles.put("Silverfish", "A silverfish. It is a small, gray insect that hides in stone blocks and swarms enemies.");
        roles.put("Endermite", "An endermite. It is a small purple bug born from ender pearls and is hated by endermen.");
        roles.put("Cave Spider", "A cave spider. It is small, venomous, climbs walls, and lives in abandoned mineshafts.");
        roles.put("Blaze", "A blaze. It floats, is made of fire and rods, and shoots fireballs in the Nether.");
        roles.put("Magma Cube", "A magma cube. It is a bouncy, fiery slime from the Nether.");
        roles.put("Wither Skeleton", "A wither skeleton. It is a tall, black skeleton with a stone sword that inflicts the Wither effect.");
        roles.put("Piglin", "A piglin. It loves gold, wields a crossbow or sword, and lives in the Nether.");
        roles.put("Piglin Brute", "A piglin brute. It is heavily scarred, wields an axe, and fiercely guards bastions.");
        roles.put("Hoglin", "A hoglin. It is a giant, aggressive boar that lives in the Nether and drops porkchops.");
        roles.put("Zoglin", "A zoglin. It is an undead hoglin that is extremely aggressive to everything.");
        roles.put("Strider", "A strider. It has long legs, walks on lava, and loves warped fungi.");
        roles.put("Phantom", "A phantom. It is a flying undead creature that attacks players who haven't slept.");
        roles.put("Drowned", "A drowned. It is an underwater zombie that can swim and throw tridents.");
        roles.put("Husk", "A husk. It is a dry zombie from the desert that doesn't burn in sunlight.");
        roles.put("Stray", "A stray. It is a frosty skeleton from ice biomes that shoots slowness arrows.");
        roles.put("Pillager", "A pillager. It is an illager armed with a crossbow that raids villages.");
        roles.put("Vindicator", "A vindicator. It is an illager armed with an iron axe that charges at enemies.");
        roles.put("Evoker", "An evoker. It is an illager mage that summons fangs and vexes.");
        roles.put("Vex", "A vex. It is a tiny, flying, ghostly creature with a sword, summoned by evokers.");
        roles.put("Ravager", "A ravager. It is a massive, horned beast that destroys crops and charges enemies.");
        roles.put("Warden", "A warden. It is blind, massive, and hunts by sound and smell in the deep dark.");
        roles.put("Goat", "A goat. It lives in mountains, jumps high, and rams anything that gets too close.");
        roles.put("Allay", "An allay. It is a small, blue, flying fairy that collects items and loves music.");
        roles.put("Frog", "A frog. It hops around, eats small slimes, and croaks in swamps.");
        roles.put("Tadpole", "A tadpole. It is a baby frog that swims in water before growing legs.");
        roles.put("Camel", "A camel. It is tall, has a hump, can carry two riders, and dashes over ravines.");
        roles.put("Sniffer", "A sniffer. It is a large, ancient creature that sniffs the ground for seeds.");
        return roles;
    }
}
